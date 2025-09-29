package alpaca.parser

import scala.collection.mutable

def parseTable(productions: List[Production]): mutable.Map[(Int, Symbol), Int | Production] = {
  val firstSet = FirstSet(productions)
  var currStateId = 0
  val states = mutable.ListBuffer(State.fromItem(State.empty, productions.head.toItem(), productions, firstSet))
  val table = mutable.Map.empty[(Int, Symbol), Int | Production]

  while states.sizeIs > currStateId do
    val currState = states(currStateId)

    for (item <- currState.view if item.isLastItem) {
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
  end while

  table
}
