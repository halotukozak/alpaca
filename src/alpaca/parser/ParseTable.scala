package alpaca
package parser

import alpaca.core.{*, given}
import alpaca.lexer.AlgorithmError

import scala.collection.mutable
import scala.quoted.*
import scala.NamedTuple.NamedTuple
import ParseAction.*
import alpaca.lexer.AlgorithmError
import scala.annotation.tailrec
import alpaca.core.Showable.mkShow

/**
 * An opaque type representing the LR parse table.
 *
 * The parse table maps from (state, symbol) pairs to parse actions.
 * This table is generated at compile time from the grammar.
 */
opaque private[parser] type ParseTable = Map[(state: Int, stepSymbol: Symbol), ParseAction]

private[parser] object ParseTable {
  extension (table: ParseTable)
    def apply(state: Int, symbol: Symbol): ParseAction =
      try table((state, symbol))
      catch case e: NoSuchElementException => throw AlgorithmError(s"No action for state $state and symbol $symbol")

    def toCsv: Csv = {
      val symbols = table.keysIterator.map(_.stepSymbol).distinct.toList
      val states = table.keysIterator.map(_.state).distinct.toList.sorted

      val headers = show"State" :: symbols.map(_.show)
      val rows = states.map(i => show"$i" :: symbols.map(s => table.get((i, s)).fold[Showable.Shown]("")(_.show)))

      Csv(headers, rows)
    }

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

    def addToTable(symbol: Symbol, action: ParseAction): Unit =
      table.get((currStateId, symbol)) match
        case None => table += ((currStateId, symbol) -> action)
        case Some(existingAction) =>
          val path = toPath(currStateId, List(symbol))
          (existingAction, action) match
            case (red1: Reduction, red2: Reduction) => throw ReduceReduceConflict(red1, red2, path)
            case (Shift(_), red: Reduction) => throw ShiftReduceConflict(symbol, red, path)
            case (red: Reduction, Shift(_)) => throw ShiftReduceConflict(symbol, red, path)
            case (Shift(_), Shift(_)) => throw AlgorithmError("Shift-Shift conflict should never happen")

    @tailrec
    def toPath(stateId: Int, acc: List[Symbol] = Nil): List[Symbol] =
      if stateId == 0 then acc
      else
        val (sourceStateId, symbol) = table.collectFirst { case (key, Shift(`stateId`)) => key }.get
        toPath(sourceStateId, symbol :: acc)

    while states.sizeIs > currStateId do {
      val currState = states(currStateId)

      for item <- currState if item.isLastItem do {
        addToTable(item.lookAhead, Reduction(item.production))
      }

      for stepSymbol <- currState.possibleSteps do {
        val newState = currState.nextState(stepSymbol, productions, firstSet)

        states.indexOf(newState) match
          case -1 =>
            addToTable(stepSymbol, Shift(states.length))
            states += newState
          case stateId =>
            addToTable(stepSymbol, Shift(stateId))
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
