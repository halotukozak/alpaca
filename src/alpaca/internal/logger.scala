package alpaca
package internal

import java.io.{BufferedWriter, FileWriter}
import java.nio.file.{Files, Path}
import java.util.concurrent.{ConcurrentHashMap, Executors, TimeUnit}

private[internal] object logger:
  private val writerCache = new ConcurrentHashMap[Path, BufferedWriter]().tap: cache =>
    sys.addShutdownHook(cache.values.forEach(_.close()))

    val scheduler = Executors.newSingleThreadScheduledExecutor()

    val task: Runnable = () =>
      cache.values.forEach: writer =>
        try writer.synchronized(writer.flush())
        catch case e: Exception => ()

    scheduler.scheduleAtFixedRate(task, 0, 4, TimeUnit.SECONDS)
    sys.addShutdownHook(scheduler.shutdown())

  private def log(level: Level, msg: Shown)(using debugSettings: DebugSettings, pos: DebugPosition): Unit =
    debugSettings.logOut(level) match
      case Out.stdout => println(show"$level: $pos\t$msg")
      case Out.file => toFile(show"${pos.file}.log", false)(show"at ${pos.line}\t$msg\n")
      case Out.disabled => ()

  def trace(msg: Shown)(using DebugSettings, DebugPosition): Unit = log(Level.trace, msg)
  def debug(msg: Shown)(using DebugSettings, DebugPosition): Unit = log(Level.debug, msg)
  def info(msg: Shown)(using DebugSettings, DebugPosition): Unit = log(Level.info, msg)
  def warn(msg: Shown)(using DebugSettings, DebugPosition): Unit = log(Level.warn, msg)
  def error(msg: Shown)(using DebugSettings, DebugPosition): Unit = log(Level.error, msg)

  // noinspection AccessorLikeMethodIsUnit
  def toFile(path: String, replace: Boolean)(content: Shown)(using debugSettings: DebugSettings): Unit =
    val file = Path.of(debugSettings.debugDirectory).resolve(path)
    val writer = writerCache.compute(
      file,
      (_, existing) =>
        if file.getParent != null then Files.createDirectories(file.getParent)
        if replace && existing != null then existing.synchronized(existing.close())
        new BufferedWriter(new FileWriter(file.toFile, !replace)),
    )

    writer.synchronized(writer.write(content))

  enum Level:
    case trace, debug, info, warn, error

    lazy val default: Out = this match
      case Level.trace | Level.debug | Level.info => Out.disabled
      case Level.warn | Level.error => Out.stdout

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
    case stdout, file, disabled

  object Out:
    given Showable[Out] = Showable(_.toString)

    given ToExpr[Out]:
      def apply(x: Out)(using Quotes): Expr[Out] = x match
        case Out.stdout => '{ Out.stdout }
        case Out.file => '{ Out.file }
        case Out.disabled => '{ Out.disabled }

    given FromExpr[Out]:
      def unapply(x: Expr[Out])(using Quotes): Option[Out] = x match
        case '{ Out.stdout } => Some(Out.stdout)
        case '{ Out.file } => Some(Out.file)
        case '{ Out.disabled } => Some(Out.disabled)
        case _ => None
