package alpaca.parser

import alpaca.core.{show, Showable}
import alpaca.core.Showable.*
import alpaca.parser.Symbol
import alpaca.parser.Symbol.*

import scala.quoted.*

/** Represents a grammar production rule.
  *
  * A production defines how a non-terminal symbol can be expanded into
  * a sequence of symbols. For example: `E -> E + T` means the non-terminal
  * E can be produced by the sequence `E + T`.
  *
  * @param lhs the left-hand side non-terminal
  * @param rhs the right-hand side sequence of symbols
  */
final case class Production(lhs: NonTerminal, rhs: List[Symbol]) {
  
  /** Converts this production to an LR(0) item with a given lookahead.
    *
    * @param lookAhead the lookahead terminal (defaults to EOF)
    * @return an Item representing this production with the dot at position 0
    */
  def toItem(lookAhead: Terminal = Symbol.EOF): Item = Item(this, 0, lookAhead)
}

object Production {
  given Showable[Production] =
    case Production(lhs, rhs) => show"$lhs -> ${rhs.mkShow}"

  given ToExpr[Production] with
    def apply(x: Production)(using Quotes): Expr[Production] =
      '{ Production(${ Expr(x.lhs) }, ${ Expr(x.rhs) }) }
}
