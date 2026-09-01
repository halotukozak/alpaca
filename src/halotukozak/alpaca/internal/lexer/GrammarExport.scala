package halotukozak
package alpaca
package internal
package lexer

import halotukozak.mcodec.MCodec

/**
 * Writes a lexer's grammar (its token names, patterns, and ignored flags) to
 * disk as JSON during macro expansion, gated by [[GrammarExportSettings]]
 * (`ALPACA_GRAMMAR_EXPORT_DIR`). No-op unless that directory is configured.
 */
private[lexer] object GrammarExport:

// $COVERAGE-OFF$
  private case class TokenExport(name: String, pattern: String, ignored: Boolean) derives MCodec

  def maybeWrite(
    lexerName: String,
    tokens: List[(name: String, pattern: String, ignored: Boolean)],
  )(using settings: GrammarExportSettings,
  ): Unit =
    JsonExport.maybeWrite(lexerName, "tokens", tokens.map(t => TokenExport(t.name, t.pattern, t.ignored)))
// $COVERAGE-ON$
