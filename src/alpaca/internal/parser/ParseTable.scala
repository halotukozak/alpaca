package alpaca
package internal
package parser

import alpaca.internal.parser.ParseAction.{Reduction, Shift}

import scala.annotation.tailrec
import scala.collection.mutable

/**
 * An opaque type representing the LR parse table.
 *
 * The parse table maps from (state, symbol) pairs to parse actions.
 * This table is generated at compile time from the grammar.
 */
opaque private[parser] type ParseTable = Map[(state: Int, stepSymbol: Symbol), ParseAction]

private[parser] object ParseTable {
  extension (table: ParseTable)
    /**
     * Gets the parse action for a given state and symbol.
     *
     * @param state the current parser state
     * @param symbol the symbol being processed
     * @return the parse action to take
     * @throws AlgorithmError if no action is defined for this state/symbol combination
     */
    def apply(state: Int, symbol: Symbol): ParseAction =
      try table((state, symbol))
      catch case e: NoSuchElementException => throw AlgorithmError(s"No action for state $state and symbol $symbol")

    /**
     * Gets the parse action for a given state and symbol, returning None if not found.
     *
     * @param state the current parser state
     * @param symbol the symbol being processed
     * @return Some(action) if found, None otherwise
     */
    def get(state: Int, symbol: Symbol): Option[ParseAction] =
      table.get((state, symbol))

    /**
     * Gets the set of expected terminal symbols for a given state.
     *
     * This is useful for error recovery and error message generation.
     *
     * @param state the current parser state
     * @return the set of terminal symbol names that have valid actions in this state
     */
    def expectedTerminals(state: Int): Set[String] =
      table.keysIterator
        .filter(key => key.state == state && key.stepSymbol.isInstanceOf[Terminal])
        .map(_.stepSymbol.name)
        .toSet

    /**
     * Converts the parse table to CSV format for debugging.
     *
     * Creates a table with states as rows and symbols as columns,
     * showing the action for each state/symbol combination.
     *
     * @return a Csv representation of the parse table
     */
    def toCsv: Csv =
      val symbols = table.keysIterator.map(_.stepSymbol).distinct.toList
      val states = table.keysIterator.map(_.state).distinct.toList.sorted

      val headers = show"State" :: symbols.map(_.show)
      val rows = states.map(i => show"$i" :: symbols.map(s => table.get((i, s)).fold[Shown]("")(_.show)))

      Csv(headers, rows)

  /**
   * Constructs the LR(1) parse table from a list of productions.
   *
   * This implements the LR(1) parser construction algorithm. It builds
   * states by computing closures of item sets and constructs the parse
   * table that maps (state, symbol) pairs to actions (shift or reduce).
   *
   * @param productions the grammar productions
   * @return the constructed parse table
   * @throws ConflictException if the grammar has shift/reduce or reduce/reduce conflicts
   */
  def apply(
    productions: List[Production],
    conflictResolutionTable: ConflictResolutionTable,
  )(using Quotes,
  ): ParseTable = {
    val firstSet = FirstSet(productions)
    var currStateId = 0
    val states =
      mutable.ListBuffer(
        State.fromItem(
          State.empty,
          productions.find(_.lhs == parser.Symbol.Start).get.toItem(),
          productions,
          firstSet,
        ),
      )
    val table = mutable.Map.empty[(state: Int, stepSymbol: Symbol), ParseAction]

    def addToTable(symbol: Symbol, action: ParseAction): Unit =
      table.get((currStateId, symbol)) match
        case None => table.update((currStateId, symbol), action)
        case Some(existingAction) =>
          conflictResolutionTable.get(existingAction, action)(symbol) match
            case Some(action) =>
              table.update((currStateId, symbol), action)
            case None =>
              val path = toPath(currStateId, List(symbol))
              (existingAction, action).runtimeChecked match
                case (red1: Reduction, red2: Reduction) => throw ReduceReduceConflict(red1, red2, path)
                case (_: Shift, red: Reduction) => throw ShiftReduceConflict(symbol, red, path)
                case (red: Reduction, _: Shift) => throw ShiftReduceConflict(symbol, red, path)
                case (_: Shift, _: Shift) => throw AlgorithmError("Shift-Shift conflict should never happen")

    @tailrec
    def toPath(stateId: Int, acc: List[Symbol] = Nil): List[Symbol] =
      if stateId == 0 then acc
      else
        val (sourceStateId, symbol) = table.collectFirst { case (key, Shift(`stateId`)) => key }.get
        toPath(sourceStateId, symbol :: acc)

    while states.sizeIs > currStateId do {
      val currState = states(currStateId)

      for item <- currState if item.isLastItem do {
        addToTable(item.lookAhead, Reduction(item.production))
      }

      for stepSymbol <- currState.possibleSteps do {
        val newState = currState.nextState(stepSymbol, productions, firstSet)

        states.indexOf(newState) match
          case -1 =>
            addToTable(stepSymbol, Shift(states.length))
            states += newState
          case stateId =>
            addToTable(stepSymbol, Shift(stateId))
      }

      currStateId += 1
    }

    table.toMap
  }

  given Showable[ParseTable] = { table =>
    val symbols = table.keysIterator.map(_.stepSymbol).distinct.toList
    val states = table.keysIterator.map(_.state).distinct.toList.sorted

    def centerText(text: String, width: Int = 10): String =
      if text.length >= width then text
      else {
        val padding = width - text.length
        val leftPad = padding / 2
        val rightPad = padding - leftPad
        (" " * leftPad) + text + (" " * rightPad)
      }

    val result = new StringBuilder
    result.append(centerText("State"))
    result.append("|")
    for (s <- symbols) {
      result.append(centerText(s.show))
      result.append("|")
    }

    for (i <- states) {
      result.append('\n')
      result.append(centerText(i.toString))
      result.append("|")
      for (s <- symbols) {
        result.append(centerText(table.get((i, s)).fold("")(_.show)))
        result.append("|")
      }
    }
    result.append('\n')
    result.result()
  }

  given ToExpr[ParseTable] with
    def apply(entries: ParseTable)(using quotes: Quotes): Expr[ParseTable] = {
      import quotes.reflect.*

      type BuilderTpe = mutable.Builder[
        ((state: Int, stepSymbol: parser.Symbol), Shift | Reduction),
        Map[(state: Int, stepSymbol: parser.Symbol), Shift | Reduction],
      ]

      val symbol = Symbol.newVal(
        Symbol.spliceOwner,
        Symbol.freshName("builder"),
        TypeRepr.of[BuilderTpe],
        Flags.Mutable,
        Symbol.noSymbol,
      )

      val valDef = ValDef(symbol, Some('{ Map.newBuilder: BuilderTpe }.asTerm))

      val builder = Ref(symbol).asExprOf[BuilderTpe]

      val additions = entries
        .map(entry =>
          '{
            def avoidTooLargerMethod(): Unit = $builder += ${ Expr(entry) }
            avoidTooLargerMethod()
          }.asTerm,
        )
        .toList

      val result = '{ $builder.result() }.asTerm

      Block(valDef :: additions, result).asExprOf[ParseTable]
    }
}
