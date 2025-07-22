package alpaca

case class Item(production: Production, dotPosition: Int, lookAhead: Terminal) {
  if dotPosition > production.rhs.length then
    throw AlgorithmError(s"Cannot initialize $production with dotPosition equal $dotPosition")

  def nextSymbol(): Symbol = production.rhs.lift(dotPosition).getOrElse(lookAhead)

  def nextTerminals(firstSet: FirstSet): Set[Terminal] =
    production.rhs.lift(dotPosition + 1) match
      case Some(symbol) => firstSet.first(symbol)
      case None => Set(lookAhead)

  val isLastItem: Boolean = dotPosition == production.rhs.length

  def nextItem(): Item =
    if isLastItem then
      throw AlgorithmError(s"$this already is the last item, cannot create any next one")
    else
      Item(production, dotPosition + 1, lookAhead)

  override def toString: String =
    s"${production.lhs} -> ${production.rhs.take(dotPosition).mkString}•${production.rhs.drop(dotPosition).mkString}, $lookAhead"
}
