package halotukozak
package alpaca
package internal
package parser

import alpaca.internal.parser.ParseAction.*

import scala.annotation.tailrec
import scala.collection.immutable.SortedSet
import scala.collection.mutable
import scala.util.boundary
import boundary.break

/**
 * An opaque type representing the LR parse table.
 *
 * State IDs are dense consecutive integers starting at 0, so the table is
 * stored as an array indexed by state, with each cell holding a symbol ->
 * action map. Lookup is a single array index plus one symbol hash, with no
 * boxing on the state key.
 */
opaque private[parser] type ParseTable = Array[Map[Symbol, ParseAction]]

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
    def apply(state: Int, symbol: Symbol): ParseAction = table(state).get(symbol) match
      case Some(action) => action
      case None =>
        val expected = table(state).keysIterator.map(_.name).to(SortedSet).mkString(", ")
        throw AlgorithmError(s"Unexpected symbol '${symbol.name}' in state $state. Expected one of: $expected")

    private def allSymbols: List[Symbol] =
      table.iterator.flatMap(_.keysIterator).distinct.toList

    /**
     * Converts the parse table to CSV format for debugging.
     *
     * Creates a table with states as rows and symbols as columns,
     * showing the action for each state/symbol combination.
     *
     * @return a Csv representation of the parse table
     */
    // it shouldn't be eager
    def toCsv: Csv =
      val symbols = table.allSymbols

      val headers = show"State" :: symbols.map(s => show"${s.name}")
      val rows = table.indices
        .map: i =>
          val row = table(i)
          show"$i" :: symbols.map(s => row.get(s).fold[Shown]("")(_.show))
        .toList

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
  // Deliberately sequential (see #31): states are discovered incrementally as a side
  // effect of processing earlier ones, so parallelizing safely would need a frontier-based
  // BFS rewrite with concurrent state dedup -- not worth the correctness risk without a
  // measured compile-time bottleneck on a real grammar.
  def apply(productions: List[Production], conflictResolutionTable: ConflictResolutionTable)(using DebugSettings)
    : ParseTable =
    val firstSet = FirstSet(productions)
    val productionsByLhs = productions.groupBy(_.lhs)
    var currStateId = 0
    val initialState = State.fromItem(
      state = State.empty,
      item = productionsByLhs(parser.Symbol.Start).head.toItem(),
      productionsByLhs = productionsByLhs,
      firstSet = firstSet,
    )
    val states = mutable.ArrayBuffer(initialState)
    val stateIndex = mutable.HashMap(initialState -> 0)
    val tableRows = mutable.ArrayBuffer(mutable.HashMap.empty[Symbol, ParseAction])

    def addToTable(symbol: Symbol, action: ParseAction): Unit =
      val row = tableRows(currStateId)
      row.get(symbol) match
        case None => row.update(symbol, action)
        case Some(existingAction) =>
          conflictResolutionTable.get(existingAction, action)(symbol) match
            case Some(action) => row.update(symbol, action)
            case None =>
              val path = toPath(currStateId, List(symbol))
              (existingAction, action) match
                case (red1: Reduction, red2: Reduction) => throw ReduceReduceConflict(red1, red2, path)
                case (Shift(_), red: Reduction) => throw ShiftReduceConflict(symbol, red, path)
                case (red: Reduction, Shift(_)) => throw ShiftReduceConflict(symbol, red, path)
                case (Shift(_), Shift(_)) => throw AlgorithmError("Shift-Shift conflict should never happen")

    // noinspection ScalaUnreachableCode
    @tailrec def toPath(stateId: Int, acc: List[Symbol]): List[Symbol] =
      if stateId == 0 then acc
      else
        val (sourceStateId, symbol) = boundary[(Int, Symbol)]:
          for srcId <- tableRows.indices do
            tableRows(srcId).foreach:
              case (sym, Shift(`stateId`)) => break((srcId, sym))
              case _ =>
          throw AlgorithmError(show"No predecessor state found for state $stateId")

        if sourceStateId == stateId then symbol :: acc
        else toPath(sourceStateId, symbol :: acc)

    while states.sizeIs > currStateId do
      val currState = states(currStateId)

      for item <- currState if item.isLastItem do addToTable(item.lookAhead, Reduction(item.production))

      for (stepSymbol, items) <- currState.itemsByNextSymbol do
        val newState = State.nextState(items, productionsByLhs, firstSet)

        val stateId = stateIndex.getOrElseUpdate(
          newState, {
            val newId = states.length
            states += newState
            tableRows += mutable.HashMap.empty
            newId
          },
        )
        addToTable(stepSymbol, Shift(stateId))

      currStateId += 1

    Array.better.tabulate(tableRows.length)(tableRows(_).toMap)

  given Showable[ParseTable] = table =>
    val symbols = table.allSymbols

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

    for i <- table.indices do
      val row = table(i)
      result.append('\n')
      result.append(centerText(i.toString))
      result.append("|")
      for s <- symbols do
        result.append(centerText(row.get(s).fold("")(_.show)))
        result.append("|")
    result.append('\n')
    result.result()

  // $COVERAGE-OFF$
  given ToExpr[ParseTable]:
    def apply(entries: ParseTable)(using quotes: Quotes): Expr[ParseTable] =
      type Row = Map[Symbol, ParseAction]
      type RowBuilder = mutable.Builder[(Symbol, ParseAction), Row]

      def rowExpr(row: Row): Expr[Row] = avoidTooLargeMethod[(Symbol, ParseAction), Row, RowBuilder](
        builder = '{ Map.newBuilder },
        elements = row.map(Expr(_)),
        empty = '{ Map.empty },
      )

      avoidTooLargeMethod[Row, Array[Row], mutable.ArrayBuilder[Row]](
        builder = '{ mutable.ArrayBuilder.ofRef[Row].tap(_.sizeHint(${ Expr(entries.length) })) },
        elements = entries.map(rowExpr),
        empty = '{ Array.empty[Row] },
      )
// $COVERAGE-ON$
