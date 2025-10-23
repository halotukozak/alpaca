package alpaca.parser

import scala.collection.immutable.SortedSet

opaque private[parser] type State <: SortedSet[Item] = SortedSet[Item]

private[parser] object State {
  val empty: State = SortedSet.empty[Item](using Ordering.by(_.hashCode))

  extension (state: State) {
    def possibleSteps: Set[Symbol] = state.view.filterNot(_.isLastItem).map(_.nextSymbol).toSet.excl(Symbol.Empty)

    def nextState(step: Symbol, productions: List[Production], firstSet: FirstSet): State =
      state.view
        .filter(item => !item.isLastItem && item.nextSymbol == step)
        .foldLeft(State.empty)((acc, item) => State.fromItem(acc, item.nextItem, productions, firstSet))
  }

  def fromItem(state: State, item: Item, productions: List[Production], firstSet: FirstSet): State =
    if !item.isLastItem && !item.nextSymbol.isInstanceOf[Terminal] then
      val lookAheads = item.nextTerminals(firstSet)

      productions.view
        .filter(_.lhs == item.nextSymbol)
        .foldLeft(state + item) { (acc, production) =>
          lookAheads.foldLeft(acc) { (acc, lookAhead) =>
            val item = production.toItem(lookAhead)
            if state.contains(item) then acc else fromItem(acc, item, productions, firstSet)
          }
        }
    else state + item
}
