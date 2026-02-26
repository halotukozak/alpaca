package alpaca
package internal

import ox.*

import scala.NamedTuple.{AnyNamedTuple, NamedTuple}
import scala.concurrent.duration.{Duration, FiniteDuration}

/**
 * A dummy value for compile-time placeholders.
 *
 * This function returns null cast to any type. It is used exclusively in
 * compile-time contexts (macros) as a placeholder value that should never
 * actually be evaluated at runtime.
 *
 * @tparam T the type to cast to
 * @return null cast to type T
 * @note This is for compile-time use only and should never be called at runtime
 */
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
    logger.trace(show"creating ReplaceRefs with ${queries.size} queries")
    new TreeMap:
      // skip NoSymbol
      private val filtered = queries.filter(!_.find.isNoSymbol)

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
    logger.trace(show"creating lambda of type ${Type.of[F]}")
    require(TypeRepr.of[F].isFunctionType, show"Expected a function type, but got: ${TypeRepr.of[F]}")

    val params :+ r = TypeRepr.of[F].typeArgs.runtimeChecked

    Lambda(
      Symbol.spliceOwner,
      MethodType(params.zipWithIndex.map((_, i) => show"$$arg$i"))(_ => params, _ => r),
      (sym, args) => rhsFn.unsafeApply((sym, args))(using Default[Expr[?]].transform(_.asTerm)),
    ).asExprOf[F]

/**
 * ToExpr instance for NamedTuple.
 *
 * Allows named tuples to be lifted to expressions during macro expansion
 * by converting them to regular tuples.
 */
private[internal] given [K <: Tuple, V <: Tuple: ToExpr] => ToExpr[NamedTuple[K, V]]:
  def apply(x: NamedTuple[K, V])(using Quotes): Expr[NamedTuple[K, V]] = Expr(x.toTuple)

/**
 * ToExpr instance for nullable types.
 *
 * Handles conversion of nullable values to expressions, properly
 * distinguishing between null and non-null values.
 */
private[internal] given [T: ToExpr as toExpr] => ToExpr[T | Null]:
  def apply(x: T | Null)(using Quotes): Expr[T | Null] = x match
    case null => '{ null }
    case value => toExpr.apply(value.asInstanceOf[T])

/**
 * FromExpr instance for nullable types.
 *
 * Handles extraction of nullable values from expressions during
 * macro expansion.
 */
private[internal] given [T: FromExpr as fromExpr] => FromExpr[T | Null]:
  def unapply(x: Expr[T | Null])(using Quotes): Option[T | Null] = x match
    case '{ $_ : Null } => Some(null)
    case value => fromExpr.unapply(value.asInstanceOf[Expr[T]])

/**
 * Creates a cancellable timeout for compilation.
 *
 * This starts a background task that will throw AlpacaTimeoutException
 * if the configured compilation timeout is exceeded. Call cancelNow()
 * on the returned fork to prevent the timeout.
 *
 * @param debugSettings the debug configuration
 * @return a cancellable fork that can be cancelled to prevent timeout
 */
private[alpaca] def timeoutOnTooLongCompilation()(using Log)(using Ox): Unit =
  forkDiscard:
    summon[Log].debugSettings.compilationTimeout.runtimeChecked match
      case duration: FiniteDuration =>
        sleep(duration)
        throw AlpacaTimeoutException()
      case Duration.Inf => ()
      case Duration.MinusInf => throw AlpacaTimeoutException()

/**
 * A helper class for overriding symbols in macro expansion.
 *
 * This class provides a way to create overriding symbols in a specific
 * parent context, which is useful for generating code that properly
 * respects symbol ownership.
 *
 * @tparam Q the Quotes type
 * @param quotes the Quotes instance
 */
private[internal] final class WithOverridingSymbol[Q <: Quotes](using val quotes: Q)(using Log):
  import quotes.reflect.*

  /**
   * Creates and uses an overriding symbol.
   *
   * @tparam T the result type
   * @param parent the parent symbol context
   * @param symbol a function to create the base symbol
   * @param body the code to execute with the overriding symbol
   * @return the result of executing body
   */
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

/**
 * Type-level operator for adding named fields to a type.
 *
 * This infix type operator allows specifying that a type B has named fields
 * of type A, enabling compile-time tracking of field information.
 *
 * @tparam B the base type with a Fields member
 * @tparam A the named tuple type representing the fields
 */
infix type withFields[B <: { type Fields <: AnyNamedTuple }, A <: AnyNamedTuple] = B { type Fields = A }

/**
 * Creates a refinement type from a sequence of labeled types.
 *
 * Builds a type with structural refinements for each label/type pair.
 * This is used for creating types with named members dynamically.
 *
 * @param refn sequence of label and type pairs
 * @return a refined TypeRepr
 */
def refinementTpeFrom(using quotes: Quotes)(refn: Seq[(label: String, tpe: quotes.reflect.TypeRepr)])
  : quotes.reflect.TypeRepr =
  import quotes.reflect.*
  refn.foldLeft(TypeRepr.of[Any]):
    case (acc, (label, tpe)) => Refinement(acc, label, tpe)

/**
 * Creates a NamedTuple type from a sequence of labeled types.
 *
 * Constructs a NamedTuple TypeRepr with the given labels and types.
 * This is used for dynamically creating named tuple types.
 *
 * @param refn sequence of label and type pairs
 * @return a NamedTuple TypeRepr
 */
def fieldsTpeFrom(using quotes: Quotes)(refn: Seq[(label: String, tpe: quotes.reflect.TypeRepr)])
  : quotes.reflect.TypeRepr =
  import quotes.reflect.*
// $COVERAGE-OFF$

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

/**
 * The number of available processor threads.
 *
 * This constant is used for parallel operations during compilation.
 */
val threads = Runtime.getRuntime.availableProcessors
// $COVERAGE-ON$