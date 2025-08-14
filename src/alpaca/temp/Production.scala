package alpaca.temp

import alpaca.Symbol
import alpaca.temp.Symbol.*

final case class Production(lhs: NonTerminal, rhs: Vector[Symbol]) {
  def toItem(lookAhead: Terminal = Symbol.EOF): Item = Item(this, 0, lookAhead)
}

given Showable[Production] = production => show"${production.lhs} -> ${production.rhs.mkShow}"
