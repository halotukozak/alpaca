package alpaca.parser

import scala.annotation.publicInBinary
import scala.collection.mutable
import alpaca.core.{Showable, show}
import scala.quoted.*

enum ParseAction {
  case Shift(newState: Int)
  case Reduction(production: Production)
}

object ParseAction {
  given Showable[ParseAction] = parseAction => parseAction match
    case ParseAction.Shift(newState)       => show"S${newState}"
    case ParseAction.Reduction(production) => show"${production}"
}

opaque type ParseTable <: mutable.Map[(Int, Symbol), ParseAction] = mutable.Map[(Int, Symbol), ParseAction]

object ParseTable {
  def empty: ParseTable = mutable.Map.empty

  def apply(productions: List[Production]): ParseTable = {
    val firstSet = FirstSet(productions)
    var currStateId = 0
    val states = mutable.ListBuffer(State.fromItem(State.empty, productions.head.toItem(), productions, firstSet))
    val table = ParseTable.empty

    while states.sizeIs > currStateId do
      val currState = states(currStateId)

      for (item <- currState if item.isLastItem) {
        table += ((currStateId, item.lookAhead) -> ParseAction.Reduction(item.production))
      }

      for (stepSymbol <- currState.possibleSteps) {
        val newState = currState.nextState(stepSymbol, productions, firstSet)

        states.indexOf(newState) match {
          case -1 =>
            table += ((currStateId, stepSymbol) -> ParseAction.Shift(states.length))
            states += newState
          case stateId =>
            table += ((currStateId, stepSymbol) -> ParseAction.Shift(stateId))
        }
      }

      currStateId += 1
    end while

    table
  }

  given ToExpr[ParseAction] with
    def apply(x: ParseAction)(using Quotes): Expr[ParseAction] = x match
      case ParseAction.Shift(i) => '{ ParseAction.Shift(${Expr(i)}) }
      case ParseAction.Reduction(p) => '{ ParseAction.Reduction(${Expr(p)}) }

  given ToExpr[ParseTable] with
    def apply(x: ParseTable)(using Quotes): Expr[ParseTable] = {
      val underlying = Expr(x.toList)

      '{ $underlying.to(mutable.Map) }
    }
}
