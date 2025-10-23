package alpaca
package parser

import alpaca.core.Showable.*
import alpaca.core.{show, Showable}
import alpaca.lexer.AlgorithmError
import alpaca.parser.{FirstSet, Symbol}

private[parser] final case class Item(production: Production, dotPosition: Int, lookAhead: Terminal) {
  production match
    case NonEmptyProduction(lhs, rhs) if dotPosition < 0 || rhs.sizeIs < dotPosition =>
      throw AlgorithmError(s"dotPosition $dotPosition out of bounds for production $production")
    case EmptyProduction(lhs) if dotPosition != 0 =>
      throw AlgorithmError(s"dotPosition for empty production must be 0, got $dotPosition")
    case _ =>

  lazy val nextSymbol: Symbol = production match
    case NonEmptyProduction(lhs, rhs) => rhs(dotPosition)
    case EmptyProduction(lhs) => throw AlgorithmError(s"$this is the last item, has no next symbol")

  lazy val nextItem: Item =
    if isLastItem then throw AlgorithmError(s"$this already is the last item, cannot create any next one")
    else Item(production, dotPosition + 1, lookAhead)

  val isLastItem: Boolean = production match
    case NonEmptyProduction(lhs, rhs) => rhs.sizeIs == dotPosition
    case EmptyProduction(lhs) => true

  val isEmpty: Boolean = production match
    case EmptyProduction(lhs) => true
    case NonEmptyProduction(lhs, rhs) => false

  def nextTerminals(firstSet: FirstSet): Set[Terminal] = production match
    case EmptyProduction(lhs) => ???
    case NonEmptyProduction(lhs, rhs) =>
      rhs.lift(dotPosition + 1) match
        case Some(symbol: Symbol) => firstSet.first(symbol)
        case None => Set(lookAhead)
}

private[parser] object Item:
  given Showable[Item] = item =>
    item.production match
      case NonEmptyProduction(lhs, rhs) =>
        val (left, right) = rhs.splitAt(item.dotPosition)
        show"$lhs -> ${left.mkShow}•${right.mkShow}, ${item.lookAhead}"
      case EmptyProduction(lhs) =>
        show"$lhs -> •${Symbol.Empty}, ${item.lookAhead}"
