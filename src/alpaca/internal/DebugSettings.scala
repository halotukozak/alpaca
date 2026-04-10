package alpaca
package internal

import scala.concurrent.duration.{Duration, DurationInt}

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
  compilationTimeout: Duration,
  enableVerboseNames: Boolean,
  logOut: Map[logger.Level, logger.Out],
)

private[internal] object DebugSettings:
  private final val Directory = "debugDirectory"
  private final val Timeout = "compilationTimeout"
  private final val EnableVerboseNames = "enableVerboseNames"

  val default: DebugSettings = DebugSettings(
    debugDirectory = None,
    compilationTimeout = 90.seconds,
    enableVerboseNames = false,
    logOut = logger.Level.values.map(l => (l, l.default)).toMap,
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
      compilationTimeout = settings.get(Timeout).map(Duration.create).getOrElse(90.seconds),
      enableVerboseNames = settings.get(EnableVerboseNames).exists(_.toBoolean),
      logOut = logger.Level.values
        .map: level =>
          (
            level,
            settings
              .get(level.toString)
              .map: name =>
                try logger.Out.valueOf(name)
                catch case _: IllegalArgumentException => level.default
              .getOrElse(level.default),
          )
        .toMap,
    )
// $COVERAGE-ON$
