package halotukozak
package alpaca
package internal
package parser

import halotukozak.alpaca.internal.DebugSettings

import scala.annotation.tailrec
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

  val empty: State = SortedSet.empty[Item](
    using Ordering
      .by[Item, String](_.production.lhs.name)
      .orElseBy(_.production.ordinal)
      .orElseBy(_.production.rhs.hashCode)
      .orElseBy(_.dotPosition)
      .orElseBy(_.lookAhead.name),
  )

  extension (state: State)

    /**
     * Groups this state's non-final items by the symbol they can shift on next.
     *
     * Computed once per state and reused for every step symbol, instead of each step
     * re-scanning the whole state to find its items (`nextSymbol` was otherwise called once
     * per item per step symbol -- proportional to state fan-out, not state size).
     *
     * @return a map from each possible step symbol to the items that shift on it
     */
    def itemsByNextSymbol: Map[Symbol, List[Item]] =
      state.iterator.filterNot(_.isLastItem).toList.groupBy(_.nextSymbol) - Symbol.Empty

  /**
   * Computes the state reached after shifting a symbol.
   *
   * This advances the dot in all given items, then closes the set by adding all items
   * derivable from non-terminals.
   *
   * @param items       the items that shift on the symbol being stepped to (see [[itemsByNextSymbol]])
   * @param productions all grammar productions
   * @param firstSet    the FIRST sets for lookahead computation
   * @return the new state
   */
  def nextState(items: List[Item], productions: List[Production], firstSet: FirstSet)(using DebugSettings): State =
    items.foldLeft(State.empty)((acc, item) => State.fromItem(acc, item.nextItem, productions, firstSet))

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
  def fromItem(state: State, item: Item, productions: List[Production], firstSet: FirstSet)(using DebugSettings)
    : State =
    @tailrec def loop(state: State, pending: Set[Item], worklist: List[Item]): State = worklist match
      case Nil => state
      case item :: rest =>
        if !item.isLastItem && !item.nextSymbol.isInstanceOf[Terminal] then
          val newState = state + item
          val lookAheads = item.nextTerminals(firstSet)
          val newItems = productions.iterator
            .filter(_.lhs == item.nextSymbol)
            .flatMap(production => lookAheads.iterator.map(production.toItem))
            .filterNot(candidate => newState.contains(candidate) || pending.contains(candidate))
            .toList
          loop(newState, pending ++ newItems, newItems ::: rest)
        else loop(state + item, pending, rest)

    loop(state, Set.empty, item :: Nil)
