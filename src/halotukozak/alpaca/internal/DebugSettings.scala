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
  private final val Directory = "debugDirectory"

  val default: DebugSettings = DebugSettings(
    debugDirectory = None,
  )

  // $COVERAGE-OFF$
  given (quotes: Quotes) => DebugSettings =
    import quotes.reflect.*

    val settings = CompilationInfo.XmacroSettings
      .flatMap:
        case s"$key=$value" => Some((key, value))
        case value =>
          report.warning(s"Invalid debug setting: $value")
          None
      .toMap

    DebugSettings(
      debugDirectory = settings.get(Directory),
    )
// $COVERAGE-ON$
