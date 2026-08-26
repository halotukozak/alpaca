package halotukozak
package alpaca.internal

/**
 * Configuration for debugging and compilation settings.
 *
 * This case class holds various configuration options that control how Alpaca
 * behaves during compilation, including logging, timeouts, and verbose output.
 *
 * @param debugDirectory optional directory for debug output files
 * @param compilationTimeout maximum time allowed for macro compilation
 * @param enableVerboseNames whether to use verbose names in generated code
 * @param logOut mapping of log levels to output destinations
 */
private[internal] final case class DebugSettings(
  debugDirectory: Option[String],
)

private[internal] object DebugSettings:
  private final val DirectoryEnvVar = "ALPACA_DEBUG_DIR"

  val default: DebugSettings = DebugSettings(
    debugDirectory = None,
  )

  // $COVERAGE-OFF$
  // Read from an env var rather than -Xmacro-settings/CompilationInfo.XmacroSettings:
  // that API is @experimental, and being @experimental is contagious to every caller of
  // the lexer/parser macros -- forcing consumers of this library onto -experimental too.
  given DebugSettings = DebugSettings(
    debugDirectory = sys.env.get(DirectoryEnvVar),
  )
// $COVERAGE-ON$
