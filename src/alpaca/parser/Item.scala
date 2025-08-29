package alpaca.parser

import alpaca.core.{Showable, mkShow, show}
import alpaca.lexer.AlgorithmError
import alpaca.parser.Production
import alpaca.parser.Symbol.*

final case class Item(production: Production, dotPosition: Int, lookAhead: Terminal) {
  if production.rhs.lengthIs < dotPosition then
    throw AlgorithmError(s"Cannot initialize $production with dotPosition equal $dotPosition")

  lazy val nextSymbol: Symbol = production.rhs.lift(dotPosition).getOrElse(lookAhead)
  lazy val nextItem: Item =
    if isLastItem then throw AlgorithmError(s"$this already is the last item, cannot create any next one")
    else Item(production, dotPosition + 1, lookAhead)
  val isLastItem: Boolean = production.rhs.lengthIs == dotPosition

  def nextTerminals(firstSet: FirstSet): Set[Terminal] =
    production.rhs.lift(dotPosition + 1) match
      case Some(symbol) => firstSet.first(symbol)
      case None => Set(lookAhead)

}

given Showable[Item] = item =>
  show"${item.production.lhs} -> ${item.production.rhs.take(item.dotPosition).mkShow}•${item.production.rhs.drop(item.dotPosition).mkShow}, ${item.lookAhead}"
