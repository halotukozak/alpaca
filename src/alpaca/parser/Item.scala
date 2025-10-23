package alpaca
package parser

import alpaca.core.{show, Showable}
import alpaca.core.Showable.*
import alpaca.lexer.AlgorithmError
import alpaca.parser.{FirstSet, Symbol}

private[parser] final case class Item(production: Production, dotPosition: Int, lookAhead: Terminal) {
  production match
    case Production.NonEmpty(_, rhs) =>
      if dotPosition < 0 || rhs.sizeIs < dotPosition then
        throw AlgorithmError(s"dotPosition $dotPosition out of bounds for production $production")
    case _: Production.Empty =>
      if dotPosition != 0 then throw AlgorithmError(s"dotPosition for empty production must be 0, got $dotPosition")

  lazy val nextSymbol: Symbol = production match
    case Production.NonEmpty(_, rhs) => rhs(dotPosition)
    case _: Production.Empty => throw AlgorithmError(s"$this is the last item, has no next symbol")

  lazy val nextItem: Item =
    if isLastItem then throw AlgorithmError(s"$this already is the last item, cannot create any next one")
    else Item(production, dotPosition + 1, lookAhead)

  val isLastItem: Boolean = production match
    case Production.NonEmpty(_, rhs) => rhs.sizeIs == dotPosition
    case _: Production.Empty => true

  def nextTerminals(firstSet: FirstSet): Set[Terminal] = production match
    case Production.NonEmpty(lhs, rhs) =>
      rhs.lift(dotPosition + 1) match
        case Some(symbol: Symbol) => firstSet.first(symbol)
        case None => Set(lookAhead)
    case _: Production.Empty => throw AlgorithmError(s"$this is an empty production, has no next terminals")
}

private[parser] object Item:
  given Showable[Item] =
    case Item(Production.NonEmpty(lhs, rhs), dotPosition, lookAhead) =>
      val (left, right) = rhs.splitAt(dotPosition)
      show"$lhs -> ${left.mkShow}•${right.mkShow}, $lookAhead"
    case Item(Production.Empty(lhs), _, lookAhead) =>
      show"$lhs -> •${Symbol.Empty}, $lookAhead"
