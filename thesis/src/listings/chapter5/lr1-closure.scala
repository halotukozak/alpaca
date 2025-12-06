def closure(
    state: State,
    item: Item,
    productions: List[Production],
    firstSet: FirstSet
): State =
  if !item.isLastItem && !item.nextSymbol.isInstanceOf[Terminal] then
    val lookAheads = item.nextTerminals(firstSet)

    productions.view
      .filter(_.lhs == item.nextSymbol)
      .foldLeft(state + item) { (acc, production) =>
        lookAheads.foldLeft(acc) { (acc, lookAhead) =>
          val item = production.toItem(lookAhead)

          if state.contains(item) then acc
          else closure(acc, item, productions, firstSet)
        }
      }
  else state + item
