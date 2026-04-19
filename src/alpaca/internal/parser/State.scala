package alpaca
package internal
package parser

import scala.collection.immutable.SortedSet

/**
 * An opaque type representing a parser state.
 *
 * In LR parsing, a state is a set of LR(1) items that represent the
 * parser's current position in recognizing various productions. Each
 * state knows which symbols can be shifted and which items might be reduced.
 */
opaque private[parser] type State <: SortedSet[Item] = SortedSet[Item]

private[parser] object State:
  // Structural ordering: hashCode-based ordering can silently drop items on
  // collision, producing incomplete LR states without any diagnostic.
  private given Ordering[Item] with
    def compare(a: Item, b: Item): Int =
      val byLhs = a.production.lhs.name.compareTo(b.production.lhs.name)
      if byLhs != 0 then byLhs
      else
        val byProd = compareProductions(a.production, b.production)
        if byProd != 0 then byProd
        else
          val byDot = Integer.compare(a.dotPosition, b.dotPosition)
          if byDot != 0 then byDot
          else a.lookAhead.name.compareTo(b.lookAhead.name)

    private def compareProductions(a: Production, b: Production): Int = (a, b) match
      case (_: Production.Empty, _: Production.NonEmpty) => -1
      case (_: Production.NonEmpty, _: Production.Empty) => 1
      case (a: Production.Empty, b: Production.Empty) => compareNames(a.name, b.name)
      case (a: Production.NonEmpty, b: Production.NonEmpty) =>
        val byName = compareNames(a.name, b.name)
        if byName != 0 then byName else compareRhs(a.rhs, b.rhs)

    private def compareRhs(a: NEL[Symbol.NonEmpty], b: NEL[Symbol.NonEmpty]): Int =
      val byLen = Integer.compare(a.size, b.size)
      if byLen != 0 then byLen
      else
        val ai = a.iterator
        val bi = b.iterator
        var result = 0
        while result == 0 && ai.hasNext do result = ai.next().name.compareTo(bi.next().name)
        result

    private def compareNames(a: ValidName | Null, b: ValidName | Null): Int =
      if a == null && b == null then 0
      else if a == null then -1
      else if b == null then 1
      else a.compareTo(b)

  val empty: State = SortedSet.empty[Item]

  extension (state: State)

    /**
     * Gets the set of symbols that can be shifted from this state.
     *
     * @return the set of symbols that appear after the dot in non-final items
     */
    def possibleSteps: Set[Symbol] = state.iterator.filterNot(_.isLastItem).map(_.nextSymbol).toSet.excl(Symbol.Empty)

    /**
     * Computes the next state after shifting a symbol.
     *
     * This advances the dot in all items that have the given symbol next,
     * then closes the set by adding all items derivable from non-terminals.
     *
     * @param step        the symbol to shift
     * @param productions all grammar productions
     * @param firstSet    the FIRST sets for lookahead computation
     * @return the new state
     */
    def nextState(step: Symbol, productions: List[Production], firstSet: FirstSet)(using Log): State =
      logger.trace(show"computing next state for symbol $step")
      state.iterator
        .filter(item => !item.isLastItem && item.nextSymbol == step)
        .foldLeft(State.empty)((acc, item) => State.fromItem(acc, item.nextItem, productions, firstSet))

  /**
   * Constructs a state closure from a single item.
   *
   * This computes the closure of an item set by recursively adding items
   * for all productions of non-terminals that appear after the dot.
   *
   * @param state the current state to add to
   * @param item the item to close
   * @param productions all grammar productions
   * @param firstSet the FIRST sets for lookahead computation
   * @return the closed state
   */
  def fromItem(state: State, item: Item, productions: List[Production], firstSet: FirstSet)(using Log): State =
    if !item.isLastItem && !item.nextSymbol.isInstanceOf[Terminal] then
      val lookAheads = item.nextTerminals(firstSet)

      productions.iterator
        .filter(_.lhs == item.nextSymbol)
        .foldLeft(state + item): (acc, production) =>
          lookAheads.foldLeft(acc): (acc, lookAhead) =>
            val item = production.toItem(lookAhead)
            if state.contains(item) then acc else fromItem(acc, item, productions, firstSet)
    else state + item
