package alpaca

import scala.quoted.{Expr, Quotes, Type}

object ExprBlock {
  def unapply[T: Type](expr: Expr[T])(using quotes: Quotes): Option[(List[Expr[Any]], Expr[T])] = {
    import quotes.reflect.*
    expr.asTerm match
      case Block(stats, expr) => Some((stats.map(_.asExpr), expr.asExprOf[T]))
      case _ => None
  }
}
