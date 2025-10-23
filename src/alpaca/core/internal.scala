package alpaca
package core

import scala.quoted.*
import scala.NamedTuple.NamedTuple

/**
 * Throws an exception indicating an impossible code path was reached.
 *
 * This function is used in pattern matching and other control flow
 * to mark code paths that should never be executed. If they are executed,
 * it indicates a bug in the library.
 *
 * @param x additional context information about what was encountered
 * @throws Exception always
 */
private[alpaca] def raiseShouldNeverBeCalled(x: String = ""): Nothing =
  throw new Exception(s"It should never happen. Got: $x")

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
private[alpaca] final class ReplaceRefs[Q <: Quotes](using val quotes: Q) {
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
        .collectFirst { case (find, replace) if find == tree.symbol => replace }
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
private[alpaca] final class CreateLambda[Q <: Quotes](using val quotes: Q) {
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
      (sym, args) => rhsFn.applyOrElse((sym, args), _ => raiseShouldNeverBeCalled(s"Unexpected arguments: $sym, $args")),
    ).asExprOf[F]
  }
}

private[alpaca] given [K <: Tuple, V <: Tuple: ToExpr]: ToExpr[NamedTuple[K, V]] with
  def apply(x: NamedTuple[K, V])(using Quotes): Expr[NamedTuple[K, V]] = Expr(x.toTuple)
