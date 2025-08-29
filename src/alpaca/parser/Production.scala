package alpaca.parser

import alpaca.core.{Showable, mkShow, show}
import alpaca.parser.Symbol
import alpaca.parser.Symbol.*

final case class Production(lhs: NonTerminal, rhs: Vector[Symbol]) {
  def toItem(lookAhead: Terminal = Symbol.EOF): Item = Item(this, 0, lookAhead)
}

object Production {
  given Showable[Production] = production => show"${production.lhs} -> ${production.rhs.mkShow}"
}
