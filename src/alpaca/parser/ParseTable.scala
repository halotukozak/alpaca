package alpaca
package parser

import alpaca.core.{*, given}

import scala.collection.mutable
import scala.quoted.*
import scala.NamedTuple.NamedTuple

opaque type ParseTable = Map[(state: Int, stepSymbol: Symbol), ParseAction]

object ParseTable {
  extension (table: ParseTable) def apply(state: Int, symbol: Symbol): ParseAction = table((state, symbol))

  def apply(productions: List[Production]): ParseTable = {
    val firstSet = FirstSet(productions)
    var currStateId = 0
    val states =
      mutable.ListBuffer(
        State.fromItem(
          State.empty,
          productions.find(_.lhs == parser.Symbol.Start).get.toItem(),
          productions,
          firstSet,
        ),
      )
    val table = mutable.Map.empty[(state: Int, stepSymbol: Symbol), ParseAction]

    while states.sizeIs > currStateId do {
      val currState = states(currStateId)

      for item <- currState if item.isLastItem do {
        table += ((currStateId, item.lookAhead) -> ParseAction.Reduction(item.production))
      }

      for stepSymbol <- currState.possibleSteps do {
        val newState = currState.nextState(stepSymbol, productions, firstSet)

        states.indexOf(newState) match
          case -1 =>
            table += ((currStateId, stepSymbol) -> ParseAction.Shift(states.length))
            states += newState
          case stateId =>
            table += ((currStateId, stepSymbol) -> ParseAction.Shift(stateId))
      }

      currStateId += 1
    }

    table.toMap
  }

  given Showable[ParseTable] = { table =>
    val symbols = table.keysIterator.map(_.stepSymbol).distinct.toList
    val states = table.keysIterator.map(_.state).distinct.toList.sorted

    def centerText(text: String, width: Int = 10): String =
      if text.length >= width then text
      else {
        val padding = width - text.length
        val leftPad = padding / 2
        val rightPad = padding - leftPad
        (" " * leftPad) + text + (" " * rightPad)
      }

    val result = new StringBuilder
    result.append(centerText("State"))
    result.append("|")
    for (s <- symbols) {
      result.append(centerText(s.show))
      result.append("|")
    }

    for (i <- states) {
      result.append('\n')
      result.append(centerText(i.toString))
      result.append("|")
      for (s <- symbols) {
        result.append(centerText(table.get((i, s)).fold("")(_.show)))
        result.append("|")
      }
    }
    result.append('\n')
    result.result()
  }

  given ToExpr[ParseTable] with
    def apply(x: ParseTable)(using Quotes): Expr[ParseTable] = '{ ${ Expr(x.toList) }.toMap }
}
