package alpaca
package internal

import ox.*

import scala.NamedTuple.{AnyNamedTuple, NamedTuple}
import scala.concurrent.duration.{Duration, FiniteDuration}

private[alpaca] def dummy[T]: T = null.asInstanceOf[T]

/**
 * A TreeMap that replaces symbol references in a tree.
 *
 * This class is used during macro expansion to transform trees by
 * replacing references to specific symbols with replacement terms.
 * This is useful for adapting code from one context to another.
 *
 * @tparam Q the Quotes type
 * @param quotes the Quotes instance
 */
private[internal] final class ReplaceRefs[Q <: Quotes](using val quotes: Q)(using Log):
  import quotes.reflect.*

  /**
   * Creates a TreeMap that replaces symbol references.
   *
   * Given a sequence of (symbol to find, term to replace with) pairs,
   * this creates a TreeMap that will substitute all references to the
   * find symbols with the corresponding replacement terms.
   *
   * @param queries pairs of (symbol to find, replacement term)
   * @return a TreeMap that performs the replacements
   */
  def apply(queries: (find: Symbol, replace: Term)*): TreeMap =
    Log.trace(show"creating ReplaceRefs with ${queries.size} queries")
    new TreeMap:
      // skip NoSymbol
      private val filtered = queries.filter(!_.find.isNoSymbol)

      override def transformTerm(tree: Term)(owner: Symbol): Term =
        filtered
          .collectFirst:
            case (find, replace) if find == tree.symbol =>
              Log.trace(show"replacing reference to $find with $replace")
              replace
          .getOrElse(super.transformTerm(tree)(owner))

/**
 * A helper for creating lambda expressions during macro expansion.
 *
 * This class provides a way to construct function expressions programmatically
 * by specifying how to build the function body given the parameter symbols.
 *
 * @tparam Q the Quotes type
 * @param quotes the Quotes instance
 */
private[internal] final class CreateLambda[Q <: Quotes](using val quotes: Q)(using Log):
  import quotes.reflect.*

  /**
   * Creates a lambda expression with a given body construction function.
   *
   * @tparam F the function type to create
   * @param rhsFn a function that builds the body tree given the method symbol and argument trees
   * @return an expression of type F
   */
  def apply[F: Type](rhsFn: PartialFunction[(Symbol, List[Tree]), Tree]): Expr[F] =
    Log.trace(show"creating lambda of type ${Type.of[F]}")
    require(TypeRepr.of[F].isFunctionType, show"Expected a function type, but got: ${TypeRepr.of[F]}")

    val params :+ r = TypeRepr.of[F].typeArgs.runtimeChecked

    Lambda(
      Symbol.spliceOwner,
      MethodType(params.zipWithIndex.map((_, i) => show"$$arg$i"))(_ => params, _ => r),
      (sym, args) => rhsFn.unsafeApply((sym, args))(using Default[Expr[?]].transform(_.asTerm)),
    ).asExprOf[F]

private[internal] given [K <: Tuple, V <: Tuple: ToExpr] => ToExpr[NamedTuple[K, V]]:
  def apply(x: NamedTuple[K, V])(using Quotes): Expr[NamedTuple[K, V]] = Expr(x.toTuple)

private[internal] given [T: ToExpr as toExpr] => ToExpr[T | Null]:
  def apply(x: T | Null)(using Quotes): Expr[T | Null] = x match
    case null => '{ null }
    case value => toExpr.apply(value.asInstanceOf[T])

private[internal] given [T: FromExpr as fromExpr] => FromExpr[T | Null]:
  def unapply(x: Expr[T | Null])(using Quotes): Option[T | Null] = x match
    case '{ $_ : Null } => Some(null)
    case value => fromExpr.unapply(value.asInstanceOf[Expr[T]])

private[alpaca] def timeoutOnTooLongCompilation()(using debugSettings: DebugSettings)(using Ox): CancellableFork[Unit] =
  forkCancellable:
    debugSettings.compilationTimeout.runtimeChecked match
      case duration: FiniteDuration =>
        sleep(duration)
        throw AlpacaTimeoutException
      case Duration.Inf => ()
      case Duration.MinusInf => throw AlpacaTimeoutException

private[internal] final class WithOverridingSymbol[Q <: Quotes](using val quotes: Q)(using Log):
  import quotes.reflect.*

  def apply[T](parent: Symbol)(symbol: Symbol => Symbol)(body: Quotes ?=> Symbol => T): T =
    val baseSymbol = symbol(parent)
    val owner = baseSymbol.overridingSymbol(parent) match
      case owner if owner.isNoSymbol =>
        Log.warn(show"overriding $baseSymbol resulted in NoSymbol, using base symbol instead")
        baseSymbol
      case owner =>
        Log.trace(show"overriding symbol $baseSymbol in $parent, new owner: $owner")
        owner

    body(using owner.asQuotes)(owner)

infix type withFields[B <: { type Fields <: AnyNamedTuple }, A <: AnyNamedTuple] = B { type Fields = A }

def extractAll[Tup <: Tuple: Type](using Quotes): List[Type[?]] = Type.of[Tup] match
  case '[h *: t] => Type.of[h] :: extractAll[t]
  case '[EmptyTuple] => Nil

def extractAll(using quotes: Quotes)(tpe: quotes.reflect.TypeRepr): List[quotes.reflect.TypeRepr] =
  import quotes.reflect.*
  tpe match
    case AppliedType(tycon, List(head, tail)) if tycon =:= TypeRepr.of[*:] => head :: extractAll(tail)
    case tpe if tpe =:= TypeRepr.of[EmptyTuple] => Nil

def refinementTpeFrom(using quotes: Quotes)(refn: Iterable[(label: String, tpe: quotes.reflect.TypeRepr)])
  : quotes.reflect.TypeRepr =
  import quotes.reflect.*
  refn.foldLeft(TypeRepr.of[Any]):
    case (acc, (label, tpe)) => Refinement(acc, label, tpe)

def fieldsTpeFrom(using quotes: Quotes)(refn: Iterable[(label: String, tpe: quotes.reflect.TypeRepr)])
  : quotes.reflect.TypeRepr =
  import quotes.reflect.*
  TypeRepr
    .of[NamedTuple]
    .appliedTo(
      refn
        .foldLeft((TypeRepr.of[EmptyTuple], TypeRepr.of[EmptyTuple])):
          case ((labels, types), (label, tpe)) =>
            (
              TypeRepr.of[*:].appliedTo(List(ConstantType(StringConstant(label)), labels)),
              TypeRepr.of[*:].appliedTo(List(tpe, types)),
            )
        .toList,
    )

extension (using quotes: Quotes)(tpe: quotes.reflect.TypeRepr)(using Log)
  // todo may not work
  def asTypeOf[T: Type]: Type[? <: T] =
    import quotes.reflect.*
    tpe.asType match
      case '[t] if TypeRepr.of[t] <:< TypeRepr.of[T] => Type.of[t & T]
      case _ => report.errorAndAbort(show"expected type ${TypeRepr.of[T]} but got $tpe")
