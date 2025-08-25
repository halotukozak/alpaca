package alpaca.core

import scala.quoted.Expr
import alpaca.lexer.EmptyCtx
import scala.quoted.Quotes
import alpaca.symbolInfo
import alpaca.dbg
import scala.quoted.Type
import scala.util.matching.Regex.Match

inline def reifyAllBetweenLexems[Ctx <: EmptyCtx](ctx: Ctx)(m: Match): Unit =
  ${ reifyAll[Ctx]('{ ctx }, '{ "betweenLexems" })('{ m }) }

private def reifyAll[T: Type](obj: Expr[T], method: Expr[String])(params: Expr[Any]*)(using quotes: Quotes)
  : Expr[Unit] = {
  import quotes.reflect.*

  val allSymbols =
    TypeRepr
      .of[T]
      .typeSymbol
      .methodMember(method.valueOrAbort)
      .flatMap(_.allOverriddenSymbols)

  val calls = allSymbols.map(obj.asTerm.select(_).appliedToArgs(params.map(_.asTerm).toList))

  Block(calls, Literal(UnitConstant())).asExprOf[Unit]
}
