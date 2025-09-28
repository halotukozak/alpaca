package alpaca.parser

import scala.collection.mutable

private val parseTableCache = mutable.Map.empty[List[Production], mutable.Map[(Int, Symbol), Int | Production]]

def parseTable(productions: List[Production]): mutable.Map[(Int, Symbol), Int | Production] = {
  parseTableCache.getOrElseUpdate(productions, computeParseTable(productions))
}

private def computeParseTable(productions: List[Production]): mutable.Map[(Int, Symbol), Int | Production] = {
  val firstSet = FirstSet(productions)
  var currStateId = 0
  val states = mutable.ListBuffer(State.fromItem(State.empty, productions.head.toItem(), productions, firstSet))
  val stateToId = mutable.Map[State, Int](states.head -> 0)
  val table = mutable.Map.empty[(Int, Symbol), Int | Production]

  while states.sizeIs > currStateId do
    val currState = states(currStateId)

    for (item <- currState if item.isLastItem) {
      table += ((currStateId, item.lookAhead) -> item.production)
    }

    for (stepSymbol <- currState.possibleSteps) {
      val newState = currState.nextState(stepSymbol, productions, firstSet)

      stateToId.get(newState) match {
        case None =>
          val newStateId = states.length
          table += ((currStateId, stepSymbol) -> newStateId)
          states += newState
          stateToId += (newState -> newStateId)
        case Some(stateId) =>
          table += ((currStateId, stepSymbol) -> stateId)
      }
    }

    currStateId += 1
  end while

  table
}
