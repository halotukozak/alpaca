package alpaca
package internal

import scala.NamedTuple.{AnyNamedTuple, NamedTuple}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

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
private[internal] final class ReplaceRefs[Q <: Quotes](using val quotes: Q):
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
    logger.trace(show"creating ReplaceRefs with ${queries.size} queries")
    new TreeMap:
      // skip NoSymbol
      private val filtered = queries.view.filterNot(_.find.isNoSymbol)

      override def transformTerm(tree: Term)(owner: Symbol): Term =
        filtered
          .collectFirst:
            case (find, replace) if find == tree.symbol =>
              logger.trace(show"replacing reference to $find with $replace")
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
private[internal] final class CreateLambda[Q <: Quotes](using val quotes: Q):
  import quotes.reflect.*

  /**
   * Creates a lambda expression with a given body construction function.
   *
   * @tparam F the function type to create
   * @param rhsFn a function that builds the body tree given the method symbol and argument trees
   * @return an expression of type F
   */
  def apply[F: Type](rhsFn: PartialFunction[(Symbol, List[Tree]), Tree]): Expr[F] =
    logger.trace(show"creating lambda of type ${Type.of[F]}")
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

inline private[alpaca] def withTimeout[T](using debugSettings: DebugSettings)(inline block: T): T =
  val future = Future(block)
  Await.result(future, debugSettings.compilationTimeout)

private[internal] final class WithOverridingSymbol[Q <: Quotes](using val quotes: Q):
  import quotes.reflect.*

  def apply[T](parent: Symbol)(symbol: Symbol => Symbol)(body: Quotes ?=> Symbol => T): T =
    val baseSymbol = symbol(parent)
    val owner = baseSymbol.overridingSymbol(parent) match
      case owner if owner.isNoSymbol =>
        logger.warn(show"overriding $baseSymbol resulted in NoSymbol, using base symbol instead")
        baseSymbol
      case owner =>
        logger.trace(show"overriding symbol $baseSymbol in $parent, new owner: $owner")
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

def refinementFrom(using quotes: Quotes)(refn: Seq[(label: String, tpe: quotes.reflect.TypeRepr)]): Type[? <: AnyKind] =
  import quotes.reflect.*
  refn
    .foldLeft(TypeRepr.of[Any]):
      case (acc, (label, tpe)) => Refinement(acc, label, tpe)
    .asType

def fieldsFrom(
  using quotes: Quotes,
)(
  refn: Seq[(label: String, tpe: quotes.reflect.TypeRepr)],
): Type[? <: AnyKind] =
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
    .asType

extension (using quotes: Quotes)(tpe: quotes.reflect.TypeRepr)
  def asTypeOf[T: Type]: Type[? <: T] =
    import quotes.reflect.*
    tpe.asType match
      case '[t] if TypeRepr.of[t] <:< TypeRepr.of[T] => Type.of[t & T]
      case _ => report.errorAndAbort(show"expected type ${TypeRepr.of[T]} but got $tpe")
