package halotukozak
package alpaca
package internal
package parser

import alpaca.internal.parser.ParseAction.*
import halotukozak.made.annotation.name
import halotukozak.mcodec.MCodec
import halotukozak.mcodec.annotation.flatten

/**
 * Writes a parser's resolved LR(1) parse table to disk as JSON during macro
 * expansion, gated by [[GrammarExportSettings]] (`ALPACA_GRAMMAR_EXPORT_DIR`).
 * No-op unless that directory is configured.
 *
 * Unlike the raw productions written by [[GrammarExport]], this table is already
 * conflict-resolved: shift/reduce and reduce/reduce conflicts have been settled via the
 * grammar's `resolutions`, so a consumer can drive it directly as a shift-reduce automaton
 * without re-deriving precedence or associativity from the bare grammar.
 */
private[parser] object TableExport:

// $COVERAGE-OFF$
  @flatten("type")
  private enum ActionExport derives MCodec:
    @name("shift") case Shift(state: Int)
    @name("reduce") case Reduce(production: GrammarExport.ProductionExport)

  private case class RowEntryExport(symbol: GrammarExport.SymbolExport, action: ActionExport) derives MCodec

  def maybeWrite(
    parserName: String,
    table: ParseTable,
  )(using settings: GrammarExportSettings,
  ): Unit =
    JsonExport.maybeWrite(parserName, "table", table.rows.toList.map(toExport))

  private def toExport(row: Map[Symbol, ParseAction]): List[RowEntryExport] =
    row.iterator.map((symbol, action) => RowEntryExport(GrammarExport.toExport(symbol), toExport(action))).toList

  private def toExport(action: ParseAction): ActionExport = action match
    case Shift(state) => ActionExport.Shift(state)
    case Reduction(production) => ActionExport.Reduce(GrammarExport.toExport(production))
// $COVERAGE-ON$
