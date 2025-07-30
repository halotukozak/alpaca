package alpaca

import Symbol.*

final case class Production(lhs: NonTerminal, rhs: Vector[Symbol]) {
  def toItem(lookAhead: Terminal = Symbol.EOF): Item = Item(this, 0, lookAhead)
}

given Showable[Production] = production => show"${production.lhs} -> ${production.rhs.mkShow}"
