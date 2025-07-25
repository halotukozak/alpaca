package alpaca

enum Symbol(val isTerminal: Boolean) {
  val name: String

  case NonTerminal(name: String) extends Symbol(isTerminal = false)
  case Terminal(name: String) extends Symbol(isTerminal = true)

  override def toString: String = name
}

object Symbol {
  val EOF: Terminal = Terminal("$")
}

type NonTerminal = Symbol.NonTerminal
type Terminal = Symbol.Terminal
