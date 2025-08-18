package alpaca.core

import scala.quoted.*
import alpaca.dbg

inline def allValuesOfType[T](obj: Any): List[T] = ${ allValuesOfTypeImpl[T]('{ obj }) }

private def allValuesOfTypeImpl[T: Type](obj: Expr[Any])(using quotes: Quotes): Expr[List[T]] = {
  import quotes.reflect.*

  val term = obj.asTerm

  val members = term.symbol.fieldMembers
  members.dbg
  Expr.ofList(members.filter(_.typeRef.widen <:< TypeRepr.of[T]).map { field =>
    Select(term, field).asExprOf[T]
  })
}
