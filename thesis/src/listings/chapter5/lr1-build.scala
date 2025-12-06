var currStateId = 0

val initialState =
  closure(
    State.empty,
    productions.find(_.lhs == parser.Symbol.Start).get.toItem(),
    // S' -> • root, $
    productions,
    firstSet
  )

val states = mutable.ListBuffer(initialState)
val table = mutable.Map.empty[(state: Int, stepSymbol: Symbol), ParseAction]

while states.sizeIs > currStateId do
  val currState = states(currStateId)

  // redukcje i akceptacja
  for item <- currState if item.isLastItem do
      addToTable(item.lookAhead, Reduction(item.production))

  // przejścia shift (goto)
  for stepSymbol <- currState.possibleSteps do
    val newState = goto(currState, stepSymbol, productions, firstSet)

    states.indexOf(newState) match
      case -1 =>
        val newId = states.length
        addToTable(stepSymbol, Shift(newId))
        states += newState
      case stateId =>
        addToTable(stepSymbol, Shift(stateId))

  currStateId += 1
