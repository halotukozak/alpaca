package alpaca

sealed trait Symbol(val isTerminal: Boolean) {
  def name: String

  override def toString: String = name
}

case class Terminal(name: String) extends Symbol(isTerminal = true)

case class NonTerminal(name: String) extends Symbol(isTerminal = false)
