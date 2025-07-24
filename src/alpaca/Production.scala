package alpaca

final case class Production(lhs: NonTerminal, rhs: Vector[Symbol]) {
  def toItem(lookAhead: Terminal = Terminal.EOF): Item = Item(this, 0, lookAhead)

  override def toString: String =
    s"$lhs -> ${rhs.mkString}"
}
