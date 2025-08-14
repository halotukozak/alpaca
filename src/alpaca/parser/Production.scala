package alpaca.parser

import alpaca.parser.Symbol
import alpaca.parser.Symbol.*
import alpaca.core.{mkShow, show, Showable}

final case class Production(lhs: NonTerminal, rhs: Vector[Symbol]) {
  def toItem(lookAhead: Terminal = Symbol.EOF): Item = Item(this, 0, lookAhead)
}

object Production {
  given Showable[Production] = production => show"${production.lhs} -> ${production.rhs.mkShow}"
}
