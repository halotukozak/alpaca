package alpaca.parser

import alpaca.core.{show, Showable}
import alpaca.core.Showable.*
import alpaca.lexer.AlgorithmError
import alpaca.parser.Production
import alpaca.parser.Symbol.*

/** Represents an LR(1) item in the parser's state machine.
  *
  * An item consists of a production with a position marker (the "dot")
  * indicating how much of the production has been recognized, plus a
  * lookahead terminal. For example: `E -> E • + T, $` means we have
  * recognized `E` and expect `+` next, with `$` as the lookahead.
  *
  * @param production the grammar production
  * @param dotPosition the position of the dot (0 to production.rhs.size)
  * @param lookAhead the lookahead terminal
  */
final case class Item(production: Production, dotPosition: Int, lookAhead: Terminal) {
  if production.rhs.size < dotPosition then
    throw AlgorithmError(s"Cannot initialize $production with dotPosition equal $dotPosition")

  /** The symbol immediately after the dot, or the lookahead if at the end. */
  lazy val nextSymbol: Symbol = production.rhs.lift(dotPosition).getOrElse(lookAhead)
  
  /** The item with the dot advanced by one position.
    *
    * @throws AlgorithmError if this is already the last item
    */
  lazy val nextItem: Item =
    if isLastItem then throw AlgorithmError(s"$this already is the last item, cannot create any next one")
    else Item(production, dotPosition + 1, lookAhead)

  /** Whether the dot is at the end of the production. */
  val isLastItem: Boolean = production.rhs.size == dotPosition

  /** Computes the set of possible next terminals for this item.
    *
    * @param firstSet the FIRST set calculator
    * @return the set of terminals that could appear next
    */
  def nextTerminals(firstSet: FirstSet): Set[Terminal] =
    production.rhs.lift(dotPosition + 1) match
      case Some(symbol: Symbol) => firstSet.first(symbol)
      case None => Set(lookAhead)
}

object Item:
  given Showable[Item] = item =>
    val (left, right) = item.production.rhs.splitAt(item.dotPosition)
    show"${item.production.lhs} -> ${left.mkShow}•${right.mkShow}, ${item.lookAhead}"
