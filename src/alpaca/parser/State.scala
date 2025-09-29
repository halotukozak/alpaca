package alpaca.parser

opaque type State <: Set[Item] = Set[Item]

extension (items: Set[Item]) {
  def possibleSteps: Set[Symbol] = items.view.filterNot(_.isLastItem).map(_.nextSymbol).toSet

  def nextState(step: Symbol, productions: List[Production], firstSet: FirstSet): State =
    items.view
      .filter(item => !item.isLastItem && item.nextSymbol == step)
      .foldLeft(State.empty)((acc, item) => State.fromItem(acc, item.nextItem, productions, firstSet))
}

object State {
  val empty: State = Set.empty

  def fromItem(state: State, item: Item, productions: List[Production], firstSet: FirstSet): State =
    if !item.isLastItem && !item.nextSymbol.isTerminal then
      val lookAheads = item.nextTerminals(firstSet)
      val newState = state + item

      productions.view
        .filter(_.lhs == item.nextSymbol)
        .foldLeft(newState) { (acc, production) =>
          lookAheads.foldLeft(acc) { (acc, lookAhead) =>
            val newItem = production.toItem(lookAhead)
            if acc.contains(newItem) then acc else fromItem(acc, newItem, productions, firstSet)
          }
        }
    else state + item
}
