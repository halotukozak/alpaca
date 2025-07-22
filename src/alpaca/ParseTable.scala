package alpaca

def parseTable(productions: List[Production]): Map[(Int, Symbol), Int | Production] = {
  val firstSet = FirstSet(productions)
  var currStateId = 0
  var states = List(State.fromItem(State.empty(), productions.head.toItem(Terminal("$")), productions, firstSet))
  var table = Map.empty[(Int, Symbol), Int | Production]

  while (currStateId < states.length) do
    val currState = states(currStateId)

    for (item <- currState if item.isLastItem) {
      table += ((currStateId, item.lookAhead), item.production)
    }

    for (stepSymbol <- currState.possibleSteps()) {
      val new_state = currState.nextState(stepSymbol, productions, firstSet)

      states.indexOf(new_state) match {
        case -1 =>
          table += ((currStateId, stepSymbol), states.length)
          states = states.appended(new_state)
        case stateId =>
          table += ((currStateId, stepSymbol), stateId)
      }
    }

    currStateId += 1

  table
}
