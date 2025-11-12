package alpaca
package parser

import alpaca.core.{show, Showable}
import alpaca.core.Showable.*
import alpaca.core.AlgorithmError
import alpaca.parser.{FirstSet, Symbol}

/**
 * Represents an LR(1) item in the parser's state machine.
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
private[parser] final case class Item(production: Production, dotPosition: Int, lookAhead: Terminal) {
  production match
    case Production.NonEmpty(_, rhs, name) =>
      if dotPosition < 0 || rhs.sizeIs < dotPosition then
        throw AlgorithmError(s"dotPosition $dotPosition out of bounds for production $production")
    case _: Production.Empty =>
      if dotPosition != 0 then throw AlgorithmError(s"dotPosition for empty production must be 0, got $dotPosition")

  /** The symbol immediately after the dot, or the lookahead if at the end. */

  lazy val nextSymbol: Symbol = production match
    case Production.NonEmpty(_, rhs, name) => rhs(dotPosition)
    case _: Production.Empty => throw AlgorithmError(s"$this is the last item, has no next symbol")

  /**
   * The item with the dot advanced by one position.
   *
   * @throws AlgorithmError if this is already the last item
   */
  lazy val nextItem: Item =
    if isLastItem then throw AlgorithmError(s"$this already is the last item, cannot create any next one")
    else Item(production, dotPosition + 1, lookAhead)

  /** Whether the dot is at the end of the production. */
  val isLastItem: Boolean = production match
    case Production.NonEmpty(_, rhs, name) => rhs.sizeIs == dotPosition
    case _: Production.Empty => true

  /**
   * Computes the set of possible next terminals for this item.
   *
   * @param firstSet the FIRST set calculator
   * @return the set of terminals that could appear next
   */
  def nextTerminals(firstSet: FirstSet): Set[Terminal] = production match
    case Production.NonEmpty(lhs, rhs, name) =>
      rhs.lift(dotPosition + 1) match
        case Some(symbol: Symbol) => firstSet.first(symbol)
        case None => Set(lookAhead)
    case _: Production.Empty => throw AlgorithmError(s"$this is an empty production, has no next terminals")
}

private[parser] object Item:
  given Showable[Item] =
    case Item(Production.NonEmpty(lhs, rhs, name), dotPosition, lookAhead) =>
      val (left, right) = rhs.splitAt(dotPosition)
      show"$lhs -> ${left.mkShow}•${right.mkShow}, $lookAhead"
    case Item(Production.Empty(lhs, name), _, lookAhead) =>
      show"$lhs -> •${Symbol.Empty}, $lookAhead"
