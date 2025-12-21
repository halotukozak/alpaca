package alpaca
package internal

import scala.NamedTuple.NamedTuple
import scala.concurrent.duration.FiniteDuration
import scala.language.experimental.modularity

private[alpaca] def dummy[T]: T = null.asInstanceOf[T]
private[alpaca] infix type has[A <: AnyKind, B <: Any { type Self <: AnyKind }] = B { type Self = A }

/**
 * A TreeMap that replaces symbol references in a tree.
 *
 * This class is used during macro expansion to transform trees by
 * replacing references to specific symbols with replacement terms.
 * This is useful for adapting code from one context to another.
 *
 * @param quotes the Quotes instance
 */
private[internal] final class ReplaceRefs(using tracked val quotes: Quotes) {
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
 * @param quotes the Quotes instance
 */
private[internal] final class CreateLambda(using tracked val quotes: Quotes) {
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
      (sym, args) => rhsFn.unsafeApply((sym, args)),
    ).asExprOf[F]
  }
}

private[internal] given [K <: Tuple, V <: Tuple: ToExpr] => ToExpr[NamedTuple[K, V]]:
  def apply(x: NamedTuple[K, V])(using Quotes): Expr[NamedTuple[K, V]] = Expr(x.toTuple)

private[internal] given [T: ToExpr as toExpr] => ToExpr[T | Null]:
  def apply(x: T | Null)(using Quotes): Expr[T | Null] = x match
    case null => '{ null }
    case value => toExpr(value.asInstanceOf[T])

// todo: it's temporary, remove when we have a proper timeout implementation
inline private[internal] def runWithTimeout[T](using debugSettings: DebugSettings)(inline block: T): T =
  import scala.concurrent.{Await, Future}
  import scala.concurrent.duration.*
  import scala.concurrent.ExecutionContext.Implicits.global

  val future = Future(block)
  Await.result(future, debugSettings.timeout.seconds)

given (using quotes: Quotes): Conversion[Expr[DebugSettings], DebugSettings] =
  case '{ DebugSettings($enabled, $directory, $timeout) } =>
    DebugSettings(
      enabled = enabled.valueOrAbort,
      directory = directory.valueOrAbort,
      timeout = timeout.valueOrAbort,
    )
  case _ =>
    quotes.reflect.report.errorAndAbort("DebugSettings must be defined inline")
