package alpaca.parser

import alpaca.core.{show, Showable}
import alpaca.core.Showable.*
import alpaca.lexer.AlgorithmError
import alpaca.parser.Production
import alpaca.parser.Symbol.*

final case class Item(production: Production, dotPosition: Int, lookAhead: Terminal) {
  if production.rhs.size < dotPosition then
    throw AlgorithmError(s"Cannot initialize $production with dotPosition equal $dotPosition")

  lazy val nextSymbol: Symbol = production.rhs.lift(dotPosition).getOrElse(lookAhead)
  lazy val nextItem: Item =
    if isLastItem then throw AlgorithmError(s"$this already is the last item, cannot create any next one")
    else Item(production, dotPosition + 1, lookAhead)

  val isLastItem: Boolean = production.rhs.size == dotPosition

  def nextTerminals(firstSet: FirstSet): Set[Terminal] =
    production.rhs.lift(dotPosition + 1) match
      case Some(symbol: Symbol) => firstSet.first(symbol)
      case None => Set(lookAhead)
}

object Item:
  given Showable[Item] = item =>
    val (left, right) = item.production.rhs.splitAt(item.dotPosition)
    show"${item.production.lhs} -> ${left.mkShow}•${right.mkShow}, ${item.lookAhead}"
