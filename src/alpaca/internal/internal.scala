package alpaca
package internal

import scala.NamedTuple.NamedTuple
import scala.concurrent.duration.FiniteDuration

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
private[internal] final class ReplaceRefs[Q <: Quotes](using val quotes: Q) {
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
  def apply(queries: (find: Symbol, replace: Term)*): TreeMap = new TreeMap {
    // skip NoSymbol
    private val filtered = queries.view.filterNot(_.find.isNoSymbol)

    override def transformTerm(tree: Term)(owner: Symbol): Term =
      filtered
        .collectFirst:
          case (find, replace) if find == tree.symbol => replace
        .getOrElse(super.transformTerm(tree)(owner))
  }
}

/**
 * A helper for creating lambda expressions during macro expansion.
 *
 * This class provides a way to construct function expressions programmatically
 * by specifying how to build the function body given the parameter symbols.
 *
 * @tparam Q the Quotes type
 * @param quotes the Quotes instance
 */
private[internal] final class CreateLambda[Q <: Quotes](using val quotes: Q) {
  import quotes.reflect.*

  /**
   * Creates a lambda expression with a given body construction function.
   *
   * @tparam F the function type to create
   * @param rhsFn a function that builds the body tree given the method symbol and argument trees
   * @return an expression of type F
   */
  def apply[F: Type](rhsFn: PartialFunction[(Symbol, List[Tree]), Tree]): Expr[F] = {
    require(TypeRepr.of[F].isFunctionType, s"Expected a function type, but got: ${TypeRepr.of[F]}")

    val params :+ r = TypeRepr.of[F].typeArgs.runtimeChecked

    Lambda(
      Symbol.spliceOwner,
      MethodType(params.zipWithIndex.map((_, i) => s"$$arg$i"))(_ => params, _ => r),
      (sym, args) => rhsFn.unsafeApply((sym, args))(using Default[Expr[?]].transform(_.asTerm)),
    ).asExprOf[F]
  }
}

private[internal] given [K <: Tuple, V <: Tuple: ToExpr]: ToExpr[NamedTuple[K, V]] with
  def apply(x: NamedTuple[K, V])(using Quotes): Expr[NamedTuple[K, V]] = Expr(x.toTuple)

private[internal] given [T: ToExpr as toExpr]: ToExpr[T | Null] with
  def apply(x: T | Null)(using Quotes): Expr[T | Null] = x match
    case null => '{ null }
    case value => toExpr(value.asInstanceOf[T])

// todo: it's temporary, remove when we have a proper timeout implementation
inline private[internal] def runWithTimeout[T](using debugSettings: DebugSettings)(inline block: T): T =
  import scala.concurrent.{Await, Future}
  import scala.concurrent.duration._
  import scala.concurrent.ExecutionContext.Implicits.global

  val future = Future(block)
  Await.result(future, debugSettings.timeout.seconds)

given (using quotes: Quotes): Conversion[Expr[DebugSettings], DebugSettings] with
  def apply(x: Expr[DebugSettings]): DebugSettings =
    import quotes.reflect.*
    x match
      case '{ DebugSettings($enabled, $directory, $timeout) } =>
        DebugSettings(
          enabled = enabled.valueOrAbort,
          directory = directory.valueOrAbort,
          timeout = timeout.valueOrAbort,
        )
      case _ =>
        report.errorAndAbort("DebugSettings must be defined inline")

private[internal] final class WithOverridingSymbol[Q <: Quotes](using val quotes: Q):
  import quotes.reflect.*

  def apply[T](parent: Symbol)(symbol: Symbol => Symbol)(body: Quotes ?=> Symbol => T): T =
    val owner = symbol(parent).overridingSymbol(parent)
    body(using owner.asQuotes)(owner)
