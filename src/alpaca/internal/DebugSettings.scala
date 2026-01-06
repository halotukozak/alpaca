package alpaca
package internal

import scala.concurrent.duration.{Duration, DurationInt}

final case class DebugSettings(
  debugDirectory: String | Null,
  compilationTimeout: Duration,
  enableVerboseNames: Boolean,
  logOut: Map[logger.Level, logger.Out],
)

object DebugSettings:
  private final val Directory = "debugDirectory"
  private final val Timeout = "compilationTimeout"
  private final val EnableVerboseNames = "enableVerboseNames"

  inline def materialize = ${ materializeImpl }
  private def materializeImpl(using quotes: Quotes): Expr[DebugSettings] = Expr(summon)

  private[alpaca] given (quotes: Quotes) => DebugSettings =
    import quotes.reflect.*
    val settings = CompilationInfo.XmacroSettings
      .flatMap:
        case s"$key=$value" => Some((key, value))
        case value =>
          report.warning(show"Invalid debug setting: $value")
          None
      .toMap

    DebugSettings(
      debugDirectory = settings.get(Directory).orNull,
      compilationTimeout = settings.get(Timeout).map(Duration.create).getOrElse(90.seconds),
      enableVerboseNames = settings.get(EnableVerboseNames).exists(_.toBoolean),
      logOut = logger.Level.values
        .map(level =>
          (
            level,
            settings
              .get(level.toString)
              .map: name =>
                try logger.Out.valueOf(name)
                catch case _: IllegalArgumentException => level.default
              .getOrElse(level.default),
          ),
        )
        .toMap,
    )

  private[alpaca] given ToExpr[DebugSettings]:
    def apply(x: DebugSettings)(using Quotes): Expr[DebugSettings] = '{
      DebugSettings(
        ${ Expr(x.debugDirectory) },
        Duration.fromNanos(${ Expr(x.compilationTimeout.toNanos) }),
        ${ Expr(x.enableVerboseNames) },
        ${ Expr(x.logOut) },
      )
    }
