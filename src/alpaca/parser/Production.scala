package alpaca.parser

import alpaca.core.{show, NonEmptyList, Showable}
import alpaca.core.Showable.*
import alpaca.parser.Symbol
import alpaca.parser.Symbol.*

import scala.quoted.*

private[parser] final case class Production(lhs: NonTerminal, rhs: NonEmptyList[Symbol]) {
  def rhsSize: Int = if rhs == NonEmptyList(Symbol.Empty) then 0 else rhs.size
  def toItem(lookAhead: Terminal = Symbol.EOF): Item = Item(this, 0, lookAhead)
}

private[parser] object Production {
  given Showable[Production] =
    case Production(lhs, rhs) => show"$lhs -> ${rhs.mkShow(" ")}"

  given ToExpr[Production] with
    def apply(x: Production)(using Quotes): Expr[Production] =
      '{ Production(${ Expr(x.lhs) }, ${ Expr(x.rhs) }) }
}
