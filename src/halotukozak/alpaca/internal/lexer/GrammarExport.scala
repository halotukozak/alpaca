package halotukozak
package alpaca
package internal
package lexer

/**
 * Writes a lexer's grammar (its token names, patterns, and ignored flags) to
 * disk as JSON during macro expansion, gated by [[GrammarExportSettings]]
 * (`ALPACA_GRAMMAR_EXPORT_DIR`). No-op unless that directory is configured.
 */
private[lexer] object GrammarExport:

// $COVERAGE-OFF$
  def maybeWrite(
    lexerName: String,
    tokens: List[(name: String, pattern: String, ignored: Boolean)],
  )(using settings: GrammarExportSettings,
  ): Unit =
    settings.exportDirectory.foreach: dir =>
      JsonExport.maybeWrite(dir, s"$lexerName.tokens.json", toJson(tokens))

  private def toJson(tokens: List[(name: String, pattern: String, ignored: Boolean)]): String =
    tokens
      .map(t =>
        s"""{"name":${JsonExport.quote(t.name)},"pattern":${JsonExport.quote(t.pattern)},"ignored":${t.ignored}}""",
      )
      .mkString("[", ",", "]")
// $COVERAGE-ON$
