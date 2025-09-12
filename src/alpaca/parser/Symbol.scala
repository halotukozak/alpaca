package alpaca.parser

import alpaca.core.Showable

enum Symbol(val isTerminal: Boolean) {
  val name: String

  case NonTerminal(name: String) extends Symbol(isTerminal = false)
  case Terminal(name: String) extends Symbol(isTerminal = true)
}

object Symbol {
  val EOF: Terminal = Terminal("$")
  val Empty: Terminal = Terminal("ε")

  given Showable[Symbol] = _.name
}
