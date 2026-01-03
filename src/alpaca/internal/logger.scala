package alpaca
package internal

import java.io.{BufferedWriter, FileWriter}
import java.nio.file.{Files, Path}
import java.util.concurrent.ConcurrentHashMap

private[internal] object logger:
  private val writerCache = new ConcurrentHashMap[Path, BufferedWriter]().tap: cache =>
    sys.addShutdownHook(cache.values().forEach(_.close()))

  private def log(level: Level, msg: Shown)(using debugSettings: DebugSettings, pos: DebugPosition): Unit =
    lazy val content = show"$level: $pos\t$msg"
    debugSettings.logOut(level) match
      case Out.Stdout => println(content)
      case Out.File => toFile("alpaca.log")(s"$content\n")
      case Out.Disabled => ()

  def trace(msg: Shown)(using DebugSettings, DebugPosition): Unit = log(Level.trace, msg)
  def debug(msg: Shown)(using DebugSettings, DebugPosition): Unit = log(Level.debug, msg)
  def info(msg: Shown)(using DebugSettings, DebugPosition): Unit = log(Level.info, msg)
  def warn(msg: Shown)(using DebugSettings, DebugPosition): Unit = log(Level.warn, msg)
  def error(msg: Shown)(using DebugSettings, DebugPosition): Unit = log(Level.error, msg)

  def toFile(path: String)(content: Shown)(using debugSettings: DebugSettings): Unit =
    val file = Path.of(debugSettings.debugDirectory).resolve(path)
    val writer = writerCache.computeIfAbsent(
      file,
      p =>
        if p.getParent != null then Files.createDirectories(p.getParent)
        new BufferedWriter(new FileWriter(p.toFile, true)),
    )

    writer.synchronized(writer.write(content))

  enum Level:
    case trace, debug, info, warn, error

    lazy val default: Out = this match
      case Level.trace | Level.debug | Level.info => Out.Disabled
      case Level.warn | Level.error => Out.Stdout

  object Level:
    given Showable[Level] = Showable(_.toString)

    given ToExpr[Level]:
      def apply(x: Level)(using Quotes): Expr[Level] =
        x match
          case Level.trace => '{ Level.trace }
          case Level.debug => '{ Level.debug }
          case Level.info => '{ Level.info }
          case Level.warn => '{ Level.warn }
          case Level.error => '{ Level.error }

    given FromExpr[Level]:
      def unapply(x: Expr[Level])(using Quotes): Option[Level] =
        x match
          case '{ Level.trace } => Some(Level.trace)
          case '{ Level.debug } => Some(Level.debug)
          case '{ Level.info } => Some(Level.info)
          case '{ Level.warn } => Some(Level.warn)
          case '{ Level.error } => Some(Level.error)
          case _ => None

  enum Out:
    case Stdout, File, Disabled

  object Out:
    given Showable[Out] = Showable(_.toString)

    given ToExpr[Out]:
      def apply(x: Out)(using Quotes): Expr[Out] = x match
        case Out.Stdout => '{ Out.Stdout }
        case Out.File => '{ Out.File }
        case Out.Disabled => '{ Out.Disabled }

    given FromExpr[Out]:
      def unapply(x: Expr[Out])(using Quotes): Option[Out] = x match
        case '{ Out.Stdout } => Some(Out.Stdout)
        case '{ Out.File } => Some(Out.File)
        case '{ Out.Disabled } => Some(Out.Disabled)
        case _ => None
