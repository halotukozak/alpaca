package alpaca

sealed trait Symbol(val isTerminal: Boolean) {
  def name: String

  override def toString: String = name
}

case class NonTerminal(name: String) extends Symbol(isTerminal = false)

case class Terminal(name: String) extends Symbol(isTerminal = true)

object Terminal {
  val EOF = Terminal("$")
}
