package alpaca

opaque type State <: Set[Item] = Set[Item]

extension (items: Set[Item]) {
  def possibleSteps: Set[Symbol] = items.view.filter(item => !item.isLastItem).map(item => item.nextSymbol()).toSet

  def nextState(step: Symbol, productions: List[Production], firstSet: FirstSet): State =
    items.view
      .filter(item => !item.isLastItem && item.nextSymbol() == step)
      .foldLeft(State.empty)((acc, item) => State.fromItem(acc, item.nextItem, productions, firstSet))
}

object State {
  def fromItem(state: State, item: Item, productions: List[Production], firstSet: FirstSet): State =
    if !item.isLastItem && !item.nextSymbol().isTerminal then
      val lookAheads = item.nextTerminals(firstSet)

      productions.view
        .filter(production => production.lhs == item.nextSymbol())
        .foldLeft(state + item) { (acc, production) =>
          lookAheads.foldLeft(acc) { (acc, lookAhead) =>
            val item = production.toItem(lookAhead)
            if state.contains(item) then acc else fromItem(acc, item, productions, firstSet)
          }
        }
    else state + item

  val empty: State = Set.empty
}
