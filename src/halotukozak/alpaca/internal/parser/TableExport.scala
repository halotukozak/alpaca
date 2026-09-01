package halotukozak
package alpaca
package internal
package parser

import alpaca.internal.parser.ParseAction.*

/**
 * Writes a parser's resolved LR(1) parse table to disk as JSON during macro
 * expansion, gated by [[GrammarExportSettings]] (`ALPACA_GRAMMAR_EXPORT_DIR`).
 * No-op unless that directory is configured.
 *
 * Unlike the raw productions written by [[GrammarExport]], this table is
 * already conflict-resolved -- shift/reduce and reduce/reduce conflicts have
 * been settled via the grammar's `resolutions` -- so a consumer can drive it
 * directly as a shift-reduce automaton without re-deriving precedence or
 * associativity from the bare grammar.
 */
private[parser] object TableExport:

// $COVERAGE-OFF$
  def maybeWrite(parserName: String, table: ParseTable)(
    using settings: GrammarExportSettings,
  ): Unit =
    settings.exportDirectory.foreach: dir =>
      JsonExport.maybeWrite(dir, s"$parserName.table.json", toJson(table))

  private def toJson(table: ParseTable): String =
    table.rows.map(toJson).mkString("[", ",", "]")

  private def toJson(row: Map[Symbol, ParseAction]): String =
    row.iterator.map(toJson).mkString("[", ",", "]")

  private def toJson(entry: (Symbol, ParseAction)): String =
    val (symbol, action) = entry
    s"""{"symbol":${GrammarExport.toJson(symbol)},"action":${toJson(action)}}"""

  private def toJson(action: ParseAction): String = action match
    case Shift(state) => s"""{"type":"shift","state":$state}"""
    case Reduction(production) => s"""{"type":"reduce","production":${GrammarExport.toJson(production)}}"""
// $COVERAGE-ON$
