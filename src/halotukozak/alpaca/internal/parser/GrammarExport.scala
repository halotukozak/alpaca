package halotukozak
package alpaca
package internal
package parser

import halotukozak.mcodec.MCodec

/**
 * Writes a parser's grammar (its productions: left-hand side, right-hand
 * side symbols, optional name) to disk as JSON during macro expansion,
 * gated by [[GrammarExportSettings]] (`ALPACA_GRAMMAR_EXPORT_DIR`). No-op
 * unless that directory is configured.
 */
private[parser] object GrammarExport:

// $COVERAGE-OFF$
  private[parser] case class SymbolExport(kind: String, name: String) derives MCodec

  // Both variants of `Production` (with or without a name) share this one flat shape rather than
  // a tagged union: a consumer never needs to distinguish `NonEmpty`/`Empty` on the wire, only
  // whether `rhs` is empty.
  private[parser] case class ProductionExport(lhs: String, rhs: List[SymbolExport], name: String | Null) derives MCodec

  private[parser] given MCodec[String | Null] = MCodec[String].nullable

  def maybeWrite(
    parserName: String,
    productions: List[Production],
  )(using settings: GrammarExportSettings,
  ): Unit =
    JsonExport.maybeWrite(parserName, "productions", productions.map(toExport))

  /** Shared with [[TableExport]], whose reduce actions embed the same production shape. */
  private[parser] def toExport(production: Production): ProductionExport = production match
    case Production.NonEmpty(lhs, rhs, name) => ProductionExport(lhs.name, rhs.toList.map(toExport), name)
    case Production.Empty(lhs, name) => ProductionExport(lhs.name, Nil, name)

  /** Shared with [[TableExport]], whose action-table rows are keyed by the same symbol shape. */
  private[parser] def toExport(symbol: Symbol): SymbolExport = symbol match
    case _: NonTerminal => SymbolExport("nonterminal", symbol.name)
    case _: Terminal => SymbolExport("terminal", symbol.name)
// $COVERAGE-ON$
