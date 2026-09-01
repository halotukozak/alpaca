package halotukozak
package alpaca
package internal
package parser

/**
 * Writes a parser's grammar (its productions: left-hand side, right-hand
 * side symbols, optional name) to disk as JSON during macro expansion,
 * gated by [[GrammarExportSettings]] (`ALPACA_GRAMMAR_EXPORT_DIR`). No-op
 * unless that directory is configured.
 */
private[parser] object GrammarExport:

// $COVERAGE-OFF$
  def maybeWrite(parserName: String, productions: List[Production])(
    using settings: GrammarExportSettings,
  ): Unit =
    settings.exportDirectory.foreach: dir =>
      JsonExport.maybeWrite(dir, s"$parserName.productions.json", toJson(productions))

  private def toJson(productions: List[Production]): String =
    productions.map(toJson).mkString("[", ",", "]")

  /** Shared with [[TableExport]], whose reduce actions embed the same production shape. */
  private[parser] def toJson(production: Production): String =
    val (lhs, rhs, name) = production match
      case Production.NonEmpty(lhs, rhs, name) => (lhs.name, rhs.toList, name)
      case Production.Empty(lhs, name) => (lhs.name, Nil, name)

    val rhsJson = rhs.map(toJson).mkString("[", ",", "]")
    val nameJson = if name == null then "null" else JsonExport.quote(name)
    s"""{"lhs":${JsonExport.quote(lhs)},"rhs":$rhsJson,"name":$nameJson}"""

  /** Shared with [[TableExport]], whose action-table rows are keyed by the same symbol shape. */
  private[parser] def toJson(symbol: Symbol): String =
    val kind = symbol match
      case _: NonTerminal => "nonterminal"
      case _: Terminal => "terminal"
    s"""{"kind":"$kind","name":${JsonExport.quote(symbol.name)}}"""
// $COVERAGE-ON$
