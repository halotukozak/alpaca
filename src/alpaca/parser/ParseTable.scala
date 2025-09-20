package alpaca.parser

import scala.annotation.publicInBinary
import scala.collection.mutable
import scala.quoted.*

opaque type ParseTable = Map[(Int, Symbol), Int | Production]

object ParseTable {

  extension (table: ParseTable) def get(key: (Int, Symbol)): Option[Int | Production] = table.get(key)

  def apply(productions: List[Production]): ParseTable = {
    val firstSet = FirstSet(productions)
    var currStateId = 0
    val states = mutable.ListBuffer(State.fromItem(State.empty, productions.head.toItem(), productions, firstSet))
    val table = mutable.Map.empty[(Int, Symbol), Int | Production]

    while states.sizeIs > currStateId do {
      val currState = states(currStateId)

      for (item <- currState if item.isLastItem) {
        table += ((currStateId, item.lookAhead) -> item.production)
      }

      for (stepSymbol <- currState.possibleSteps) {
        val newState = currState.nextState(stepSymbol, productions, firstSet)

        states.indexOf(newState) match {
          case -1 =>
            table += ((currStateId, stepSymbol) -> states.length)
            states += newState
          case stateId =>
            table += ((currStateId, stepSymbol) -> stateId)
        }
      }

      currStateId += 1
    }
    table.toMap
  }

  given ToExpr[Int | Production] with
    def apply(x: Int | Production)(using Quotes): Expr[Int | Production] = x match
      case i: Int => Expr(i)
      case p: Production => Expr(p)

  given ToExpr[ParseTable] with
    def apply(x: ParseTable)(using Quotes): Expr[ParseTable] = {
      val underlying = Expr(x.toList)

      '{ $underlying.toMap }
    }
}
