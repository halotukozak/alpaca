package halotukozak
package alpaca
package internal
package parser

import scala.collection.mutable

/**
 * The LR(0) automaton built as the first phase of LALR(1) construction (#504).
 *
 * @param states  the automaton's states, dense-indexed
 * @param kernels each state's kernel items -- the initial item for state 0, or (for any other
 *                state) the cores obtained by advancing the dot on whichever items of its
 *                predecessor state shifted into it. Every other item in a state's closure is
 *                derivable from its kernel, so lookahead propagation (see [[Lookaheads]]) only
 *                needs to track kernels.
 * @param goto    each state's outgoing shift transitions by symbol
 */
private[parser] final case class LR0Automaton(
  states: IndexedSeq[LR0State],
  kernels: IndexedSeq[List[Core]],
  goto: IndexedSeq[Map[Symbol, Int]],
)

private[parser] object LR0Automaton:
  /**
   * Constructs the LR(0) automaton for a grammar.
   *
   * Closure here never consults `FirstSet` -- combined with states being keyed by [[Core]]
   * (dropping lookahead from item identity), this is what keeps automaton construction cheap
   * relative to canonical LR(1), independent of the lookahead-propagation phase that follows.
   *
   * @param productionsByLhs all grammar productions, indexed by their LHS non-terminal
   * @return the constructed automaton
   */
  def apply(productionsByLhs: Map[NonTerminal, List[Production]]): LR0Automaton = {
    val initialKernel = Core(productionsByLhs(parser.Symbol.Start).head)
    val initialState = LR0State.fromCore(LR0State.empty, initialKernel, productionsByLhs)

    val states = mutable.ArrayBuffer(initialState)
    val kernels = mutable.ArrayBuffer(List(initialKernel))
    val gotoRows = mutable.ArrayBuffer(mutable.HashMap.empty[Symbol, Int])
    val stateIndex = mutable.HashMap(initialState -> 0)

    var currStateId = 0
    while states.sizeIs > currStateId do {
      val currState = states(currStateId)

      for (stepSymbol, cores) <- currState.itemsByNextSymbol do {
        val newState = LR0State.nextState(cores, productionsByLhs)

        val stateId = stateIndex.getOrElseUpdate(
          newState, {
            val newId = states.length
            states += newState
            kernels += cores.map(_.nextCore)
            gotoRows += mutable.HashMap.empty
            newId
          },
        )
        gotoRows(currStateId).update(stepSymbol, stateId)
      }

      currStateId += 1
    }

    LR0Automaton(states.toIndexedSeq, kernels.toIndexedSeq, gotoRows.map(_.toMap).toIndexedSeq)
  }
