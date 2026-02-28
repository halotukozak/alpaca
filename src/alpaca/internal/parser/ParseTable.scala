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

private[parser] object ParseTable:
  extension (table: ParseTable)
    /**
     * Gets the parse action for a given state and symbol.
     *
     * @param state the current parser state
     * @param symbol the symbol being processed
     * @return the parse action to take
     * @throws AlgorithmError if no action is defined for this state/symbol combination
     */
    def apply(state: Int, symbol: Symbol)(using Log): ParseAction =
      try table((state, symbol))
      catch case _: NoSuchElementException => throw AlgorithmError(show"No action for state $state and symbol $symbol")

    /** Runtime lookup without Log dependency -- uses plain string interpolation for error messages. */
    def runtimeApply(state: Int, symbol: Symbol): ParseAction =
      try table((state, symbol))
      catch case _: NoSuchElementException => throw AlgorithmError(s"No action for state $state and symbol ${symbol.name}")

    /**
     * Converts the parse table to CSV format for debugging.
     *
     * Creates a table with states as rows and symbols as columns,
     * showing the action for each state/symbol combination.
     *
     * @return a Csv representation of the parse table
     */
    // it shouldn't be eager
    def toCsv(using Log): Csv =
      val symbols = table.keysIterator.map(_.stepSymbol).distinct.toList
      val states = table.keysIterator.map(_.state).distinct.toList.sorted // todo: SortedSet?

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
  // todo: can be parallelized with Ox? https://github.com/halotukozak/alpaca/issues/31
  def apply(productions: List[Production], conflictResolutionTable: ConflictResolutionTable)(using Log): ParseTable =
    logger.trace("building first set...")
    val firstSet = FirstSet(productions)
    logger.trace("building states and parse table...")
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
              logger.trace(show"Conflict resolved: $action")
              table.update((currStateId, symbol), action)
            case None =>
              val path = toPath(currStateId, List(symbol))
              (existingAction, action).runtimeChecked match
                case (red1: Reduction, red2: Reduction) => throw ReduceReduceConflict(red1, red2, path)
                case (_: Shift, red: Reduction) => throw ShiftReduceConflict(symbol, red, path)
                case (red: Reduction, _: Shift) => throw ShiftReduceConflict(symbol, red, path)
                case (_: Shift, _: Shift) => throw AlgorithmError("Shift-Shift conflict should never happen")

    @tailrec def toPath(stateId: Int, acc: List[Symbol]): List[Symbol] =
      if stateId == 0 then acc
      else
        val (sourceStateId, symbol) = table.collectFirst { case (key, Shift(`stateId`)) => key }.get
        if sourceStateId == stateId then
          logger.debug(show"Unable to trace back path for state, cycle detected near symbol: $symbol")
          symbol :: acc
        else toPath(sourceStateId, symbol :: acc)

    while states.sizeIs > currStateId do
      val currState = states(currStateId)
      logger.trace(show"processing state $currStateId")

      for item <- currState if item.isLastItem do addToTable(item.lookAhead, Reduction(item.production))

      for stepSymbol <- currState.possibleSteps do
        val newState = currState.nextState(stepSymbol, productions, firstSet)

        states.indexOf(newState) match
          case -1 =>
            addToTable(stepSymbol, Shift(states.length))
            states += newState
          case stateId =>
            addToTable(stepSymbol, Shift(stateId))

      currStateId += 1

    table.toMap

  given Showable[ParseTable] = Showable: table =>
    val symbols = table.keysIterator.map(_.stepSymbol).distinct.toList
    val states = table.keysIterator.map(_.state).distinct.toList.sorted // todo: SortedSet?

    def centerText(text: String, width: Int = 10): String =
      if text.length >= width then text
      else
        val padding = width - text.length
        val leftPad = padding / 2
        val rightPad = padding - leftPad
        (" " * leftPad) + text + (" " * rightPad)

    val result = new StringBuilder
    result.append(centerText("State"))
    result.append("|")
    for s <- symbols do
      result.append(centerText(s.show))
      result.append("|")

    for i <- states do
      result.append('\n')
      result.append(centerText(i.toString))
      result.append("|")
      for s <- symbols do
        result.append(centerText(table.get((i, s)).fold("")(_.show)))
        result.append("|")
    result.append('\n')
    result.result()

  // $COVERAGE-OFF$
  given ToExpr[ParseTable] with
    def apply(entries: ParseTable)(using quotes: Quotes): Expr[ParseTable] =
      import quotes.reflect.*

      type BuilderTpe = mutable.Builder[
        ((state: Int, stepSymbol: parser.Symbol), Shift | Reduction),
        Map[(state: Int, stepSymbol: parser.Symbol), Shift | Reduction],
      ]

      val symbol = Symbol.newVal(
        Symbol.spliceOwner,
        Symbol.freshName("builder"),
        TypeRepr.of[BuilderTpe],
        Flags.Synthetic,
        Symbol.noSymbol,
      )

      val valDef = ValDef(symbol, Some('{ Map.newBuilder: BuilderTpe }.asTerm))

      val builder = Ref(symbol).asExprOf[BuilderTpe]

      val additions = entries
        .map: entry =>
          '{
            def avoidTooLargeMethod(): Unit = $builder += ${ Expr(entry) }
            avoidTooLargeMethod()
          }.asTerm
        .toList

      val result = '{ $builder.result() }.asTerm

      Block(valDef :: additions, result).asExprOf[ParseTable]
// $COVERAGE-ON$
