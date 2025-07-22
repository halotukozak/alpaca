package alpaca

opaque type State <: Set[Item] = Set[Item]

extension (items: Set[Item]) {
  def possibleSteps(): Set[Symbol] = items.filter(item => !item.isLastItem).map(item => item.nextSymbol())

  def nextState(step: Symbol, productions: List[Production], firstSet: FirstSet): State =
    items
      .filter(item => !item.isLastItem && item.nextSymbol() == step)
      .foldLeft(State.empty())((acc, item) => State.fromItem(acc, item.nextItem(), productions, firstSet))
}

object State {
  def fromItem(state: State = State.empty(), item: Item, productions: List[Production], firstSet: FirstSet): State = {
    val newState = state + item

    if !item.isLastItem && !item.nextSymbol().isTerminal then
      val lookAheads = item.nextTerminals(firstSet)

      productions
        .filter(production => production.lhs == item.nextSymbol())
        .foldLeft(newState) { (acc, production) =>
          lookAheads.foldLeft(acc) { (acc, lookAhead) =>
            val item = production.toItem(lookAhead)
            if state.contains(item) then acc else fromItem(acc, item, productions, firstSet)
          }
        }
    else newState
  }

  inline def apply(items: Set[Item]): State = items
  inline def empty(): State = Set.empty
}
