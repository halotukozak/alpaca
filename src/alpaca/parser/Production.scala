package alpaca.parser

import alpaca.core.{show, Showable}
import alpaca.parser.Symbol
import alpaca.parser.Symbol.*

import scala.quoted.*
import scala.runtime.FunctionXXL
import compiletime.ops.boolean.&&

final case class Production(lhs: NonTerminal, rhs: List[Symbol]) {
  def toItem(lookAhead: Terminal = Symbol.EOF): Item = Item(this, 0, lookAhead)
}

object Production {
  given Showable[Production] = production => show"${production.lhs} -> ${production.rhs}"

  given ToExpr[Production] with
    def apply(x: Production)(using Quotes): Expr[Production] =
      '{ Production(${ Expr(x.lhs) }, ${ Expr(x.rhs) }) }
}
