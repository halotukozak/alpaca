package halotukozak
package alpaca.internal

/**
 * Configuration for exporting a lexer's (and, eventually, a parser's) grammar
 * as JSON during compilation, for external tooling (e.g. an IDE plugin) that
 * needs a grammar's rules without running the compiled lexer/parser.
 *
 * @param exportDirectory optional directory to write `<name>.tokens.json` files into
 */
private[internal] final case class GrammarExportSettings(exportDirectory: Option[String])

private[internal] object GrammarExportSettings:
  private final val DirectoryEnvVar = "ALPACA_GRAMMAR_EXPORT_DIR"

  // $COVERAGE-OFF$
  // Read from an env var for the same reason as DebugSettings: -Xmacro-settings/
  // CompilationInfo.XmacroSettings is @experimental, and being @experimental is
  // contagious to every caller of the lexer/parser macros.
  given GrammarExportSettings = GrammarExportSettings(
    exportDirectory = sys.env.get(DirectoryEnvVar),
  )
// $COVERAGE-ON$
