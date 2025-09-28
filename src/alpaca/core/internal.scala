package alpaca
package core

import scala.annotation.experimental
import scala.quoted.{Expr, Quotes, ToExpr, Type}
import scala.NamedTuple.NamedTuple

private[alpaca] def raiseShouldNeverBeCalled(x: String = ""): Nothing =
  throw new Exception(s"It should never happen. Got: $x")

private[alpaca] final class ReplaceRefs[Q <: Quotes](using val quotes: Q) {
  import quotes.reflect.*

  def apply(queries: (find: Symbol, replace: Term)*): TreeMap = new TreeMap {
    // skip NoSymbol
    private val filtered = queries.view.filterNot(_.find.isNoSymbol)

    override def transformTerm(tree: Term)(owner: Symbol): Term =
      filtered
        .collectFirst { case (find, replace) if find == tree.symbol => replace }
        .getOrElse(super.transformTerm(tree)(owner))
  }
}


private[alpaca] final class CreateLambda[Q <: Quotes](using val quotes: Q) {
  import quotes.reflect.*

  def apply[F: Type](rhsFn: PartialFunction[(Symbol, List[Tree]), Tree]): Expr[F] = {
    require(TypeRepr.of[F].isFunctionType, s"Expected a function type, but got: ${TypeRepr.of[F]}")

    val params :+ r = TypeRepr.of[F].typeArgs: @unchecked

    Lambda(
      Symbol.spliceOwner,
      MethodType(params.zipWithIndex.map((_, i) => s"$$arg$i"))(_ => params, _ => r),
      (sym, args) => rhsFn.applyOrElse((sym, args), _ => raiseShouldNeverBeCalled(s"Unexpected arguments: $sym, $args")),
    ).asExprOf[F]
  }
}

given [K <: Tuple, V <: Tuple: ToExpr]: ToExpr[NamedTuple[K, V]] with
  override def apply(x: NamedTuple[K, V])(using Quotes): Expr[NamedTuple[K, V]] =
    Expr(x.toTuple)
