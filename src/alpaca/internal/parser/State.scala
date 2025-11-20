package alpaca.internal.parser

import scala.collection.immutable.SortedSet
import scala.language.experimental.relaxedLambdaSyntax

/**
 * An opaque type representing a parser state.
 *
 * In LR parsing, a state is a set of LR(1) items that represent the
 * parser's current position in recognizing various productions. Each
 * state knows which symbols can be shifted and which items might be reduced.
 */
opaque private[parser] type State <: SortedSet[Item] = SortedSet[Item]

private[parser] object State {
  val empty: State = SortedSet.empty[Item](using Ordering.by(_.hashCode))

  extension (state: State) {

    /**
     * Gets the set of symbols that can be shifted from this state.
     *
     * @return the set of symbols that appear after the dot in non-final items
     */
    def possibleSteps: Set[Symbol] = state.view.filterNot(_.isLastItem).map(_.nextSymbol).toSet.excl(Symbol.Empty)

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
    def nextState(step: Symbol, productions: List[Production], firstSet: FirstSet): State =
      state.view
        .filter: item => !item.isLastItem && item.nextSymbol == step
        .foldLeft(State.empty): (acc, item) => State.fromItem(acc, item.nextItem, productions, firstSet)
  }

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
  def fromItem(state: State, item: Item, productions: List[Production], firstSet: FirstSet): State =
    if !item.isLastItem && !item.nextSymbol.isInstanceOf[Terminal] then
      val lookAheads = item.nextTerminals(firstSet)

      productions.view
        .filter(_.lhs == item.nextSymbol)
        .foldLeft(state + item): (acc, production) =>
          lookAheads.foldLeft(acc): (acc, lookAhead) =>
            val item = production.toItem(lookAhead)
            if state.contains(item) then acc else fromItem(acc, item, productions, firstSet)
    
    else state + item
}
