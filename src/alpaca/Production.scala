package alpaca

case class Production(lhs: NonTerminal, rhs: List[Symbol]) {
  def toItem(lookAhead: Terminal): Item = Item(this, 0, lookAhead)

  override def toString: String = {
    s"${lhs} -> ${rhs.mkString}"
  }
}
