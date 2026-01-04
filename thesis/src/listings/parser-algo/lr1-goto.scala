def goto(
    state: State,
    step: Symbol,
    productions: List[Production],
    firstSet: FirstSet
): State =
  state.view
    .filter(item => !item.isLastItem && item.nextSymbol == step)
    .foldLeft(State.empty) { (acc, item) =>
      closure(acc, item.nextItem, productions, firstSet)
    }
