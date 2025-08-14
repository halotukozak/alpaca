package alpaca.temp

enum Symbol(val isTerminal: Boolean) {
  val name: String

  case NonTerminal(name: String) extends Symbol(isTerminal = false)
  case Terminal(name: String) extends Symbol(isTerminal = true)
}

given Showable[Symbol] = _.name

object Symbol {
  val EOF: Terminal = Terminal("$")
}
