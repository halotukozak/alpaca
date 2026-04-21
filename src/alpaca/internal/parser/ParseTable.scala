package alpaca
package internal
package parser

import alpaca.internal.parser.ParseAction.*

import scala.annotation.tailrec
import scala.collection.immutable.SortedSet
import scala.collection.mutable

/**
 * An opaque type representing the LR parse table.
 *
 * The parse table is a nested map: state -> symbol -> action. This avoids
 * allocating a tuple key on every lookup and keeps the hash of the hot
 * `parseTable(state, symbol)` call to a single symbol hash.
 */
opaque private[parser] type ParseTable = Map[Int, Map[Symbol, ParseAction]]

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
    def apply(state: Int, symbol: Symbol): ParseAction =
      val row = table.getOrElse(state, Map.empty)
      row.getOrElse(
        symbol, {
          val expected = row.keysIterator.map(_.name).to(SortedSet).mkString(", ")
          throw AlgorithmError(s"Unexpected symbol '${symbol.name}' in state $state. Expected one of: $expected")
        },
      )

    private def allSymbols: List[Symbol] =
      table.valuesIterator.flatMap(_.keysIterator).distinct.toList

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
      val symbols = table.allSymbols
      val states = table.keysIterator.toList.sorted

      val headers = show"State" :: symbols.map(_.show)
      val rows = states.map: i =>
        val row = table(i)
        show"$i" :: symbols.map(s => row.get(s).fold[Shown]("")(_.show))

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
    val initialState = State.fromItem(
      State.empty,
      productions.find(_.lhs == parser.Symbol.Start).get.toItem(),
      productions,
      firstSet,
    )
    val states = mutable.ArrayBuffer(initialState)
    val stateIndex = mutable.HashMap(initialState -> 0)
    val table = mutable.HashMap.empty[Int, mutable.HashMap[Symbol, ParseAction]]

    def rowFor(stateId: Int): mutable.HashMap[Symbol, ParseAction] =
      table.getOrElseUpdate(stateId, mutable.HashMap.empty)

    def addToTable(symbol: Symbol, action: ParseAction): Unit =
      val row = rowFor(currStateId)
      row.get(symbol) match
        case None => row.update(symbol, action)
        case Some(existingAction) =>
          conflictResolutionTable.get(existingAction, action)(symbol) match
            case Some(action) =>
              logger.trace(show"Conflict resolved: $action")
              row.update(symbol, action)
            case None =>
              val path = toPath(currStateId, List(symbol))
              (existingAction, action) match
                case (red1: Reduction, red2: Reduction) => throw ReduceReduceConflict(red1, red2, path)
                case (Shift(_), red: Reduction) => throw ShiftReduceConflict(symbol, red, path)
                case (red: Reduction, Shift(_)) => throw ShiftReduceConflict(symbol, red, path)
                case (Shift(_), Shift(_)) => throw AlgorithmError("Shift-Shift conflict should never happen")

    @tailrec def toPath(stateId: Int, acc: List[Symbol]): List[Symbol] =
      if stateId == 0 then acc
      else
        val (sourceStateId, symbol) = table.iterator
          .flatMap { case (srcId, row) =>
            row.collectFirst { case (sym, Shift(`stateId`)) => (srcId, sym) }
          }
          .next()
        if sourceStateId == stateId then
          logger.debug(show"Unable to trace back path for state, cycle detected near symbol: $symbol")
          symbol :: acc
        else toPath(sourceStateId, symbol :: acc)

    while states.sizeIs > currStateId do
      val currState = states(currStateId)
      rowFor(currStateId) // ensure row exists even for states with no entries
      logger.trace(show"processing state $currStateId")

      for item <- currState if item.isLastItem do addToTable(item.lookAhead, Reduction(item.production))

      for stepSymbol <- currState.possibleSteps do
        val newState = currState.nextState(stepSymbol, productions, firstSet)

        val stateId = stateIndex.getOrElseUpdate(
          newState, {
            val newId = states.length
            states += newState
            newId
          },
        )
        addToTable(stepSymbol, Shift(stateId))

      currStateId += 1

    table.iterator.map { case (i, row) => (i, row.toMap) }.toMap

  given Showable[ParseTable] = Showable: table =>
    val symbols = table.allSymbols
    val states = table.keysIterator.toList.sorted

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
  given ToExpr[ParseTable] with
    def apply(entries: ParseTable)(using quotes: Quotes): Expr[ParseTable] =
      import quotes.reflect.*

      type RowBuilderTpe = mutable.Builder[(parser.Symbol, ParseAction), Map[parser.Symbol, ParseAction]]
      type OuterBuilderTpe = mutable.Builder[
        (Int, Map[parser.Symbol, ParseAction]),
        Map[Int, Map[parser.Symbol, ParseAction]],
      ]

      def rowExpr(row: Map[parser.Symbol, ParseAction]): Expr[Map[parser.Symbol, ParseAction]] =
        if row.isEmpty then '{ Map.empty[parser.Symbol, ParseAction] }
        else
          val rowSym = Symbol.newVal(
            Symbol.spliceOwner,
            Symbol.freshName("rowBuilder"),
            TypeRepr.of[RowBuilderTpe],
            Flags.Synthetic,
            Symbol.noSymbol,
          )
          val rowValDef = ValDef(rowSym, Some('{ Map.newBuilder: RowBuilderTpe }.asTerm))
          val rowBuilder = Ref(rowSym).asExprOf[RowBuilderTpe]

          val additions = row.toList.map: entry =>
            '{
              def avoidTooLargeMethod(): Unit = $rowBuilder += ${ Expr(entry) }
              avoidTooLargeMethod()
            }.asTerm

          Block(rowValDef :: additions, '{ $rowBuilder.result() }.asTerm)
            .asExprOf[Map[parser.Symbol, ParseAction]]

      val outerSym = Symbol.newVal(
        Symbol.spliceOwner,
        Symbol.freshName("tableBuilder"),
        TypeRepr.of[OuterBuilderTpe],
        Flags.Synthetic,
        Symbol.noSymbol,
      )
      val outerValDef = ValDef(outerSym, Some('{ Map.newBuilder: OuterBuilderTpe }.asTerm))
      val outerBuilder = Ref(outerSym).asExprOf[OuterBuilderTpe]

      val rowAdditions = entries.toList.map: (stateId, row) =>
        '{
          def avoidTooLargeMethod(): Unit =
            $outerBuilder += ((${ Expr(stateId) }, ${ rowExpr(row) }))
          avoidTooLargeMethod()
        }.asTerm

      val result = '{ $outerBuilder.result() }.asTerm

      Block(outerValDef :: rowAdditions, result).asExprOf[ParseTable]
// $COVERAGE-ON$
