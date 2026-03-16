package alpaca
package internal

import alpaca.internal.logger.*

import java.io.{BufferedWriter, FileWriter}
import java.nio.file.{Files, Path}
import java.util.concurrent.ConcurrentHashMap

/**
 * A logging facility for Alpaca macro compilation.
 *
 * This class provides logging capabilities during macro expansion, supporting
 * both console and file-based logging. It manages file writers with automatic
 * flushing based on the compilation timeout.
 *
 * @param debugSettings the debug configuration
 */
private[internal] class Log(using val debugSettings: DebugSettings) extends AutoCloseable:
  private given Log = this

  private val writerCache = new ConcurrentHashMap[Path, BufferedWriter]

  private val flushing: Thread = new Thread:
    override def run(): Unit =
      if debugSettings.compilationTimeout.isFinite then
        try Thread.sleep(5000)
        catch case _: InterruptedException => return
        writerCache.forEach(
          threads,
          (_, writer) =>
            try writer.flush()
            catch case _: Exception => (),
        )
  .tap: t =>
    t.setDaemon(true)
    t.start()

  override def close(): Unit =
    flushing.interrupt()
    writerCache.forEach(threads, (_, writer) => writer.close())

  private def createWriter(path: Path, replace: Boolean): BufferedWriter =
    if path.getParent != null then Files.createDirectories(path.getParent)
    new BufferedWriter(new FileWriter(path.toFile, !replace))

  def append(path: Path)(content: Shown): Unit = writerCache.compute(
    path,
    (p, existing) => (if existing == null then createWriter(p, false) else existing).tap(_.write(content)),
  )

  def replace(path: Path)(content: Shown): Unit = writerCache
    .compute(
      path,
      (p, existing) =>
        if existing != null then existing.close()
        createWriter(p, true).tap(_.write(content)),
    )

  // noinspection AccessorLikeMethodIsUnit
  inline def toFile(path: String, replace: Boolean)(content: Shown): Unit =
    val file = Path.of(debugSettings.debugDirectory).resolve(path)
    if replace then this.replace(file)(content) else this.append(file)(content)

  /**
   * Logs a message at the given level.
   *
   * @param level the severity level
   * @param msg   the message to log
   * @param pos   the source position (provided implicitly by the compiler)
   */
  def log(level: Level, msg: Shown)(using pos: DebugPosition): Unit = debugSettings.logOut(level) match
    case Out.stdout => println(show"$level: $pos\t$msg")
    case Out.file => toFile(show"${pos.file}.log", false)(show"at ${pos.line}\t$msg\n")
    case Out.disabled => ()

inline private[internal] def withLog[T](inline op: Log ?=> T): T =
  given log: Log = logger.materialize
  try op
  finally log.close()

private[internal] object logger:

  inline private[internal] def materialize = ${ materializeImpl }
  // $COVERAGE-OFF$
  private def materializeImpl(using quotes: Quotes): Expr[Log] =
    val debugSettings = Expr(summon[DebugSettings])
    '{ new Log(using $debugSettings) }
  // $COVERAGE-ON$

  inline def trace(inline msg: Shown)(using DebugPosition, Log): Unit = summon[Log].log(Level.trace, msg)
  inline def debug(inline msg: Shown)(using DebugPosition, Log): Unit = summon[Log].log(Level.debug, msg)
  inline def info(inline msg: Shown)(using DebugPosition, Log): Unit = summon[Log].log(Level.info, msg)
  inline def warn(inline msg: Shown)(using DebugPosition, Log): Unit = summon[Log].log(Level.warn, msg)
  inline def error(inline msg: Shown)(using DebugPosition, Log): Unit = summon[Log].log(Level.error, msg)

  // noinspection AccessorLikeMethodIsUnit
  inline def toFile(path: String, replace: Boolean)(content: Shown)(using Log): Unit =
    summon[Log].toFile(path, replace)(content)

  /**
   * Logging severity levels, ordered from most to least verbose.
   */
  enum Level:
    /** Finest-grained informational events. Disabled by default. */
    case trace

    /** Detailed debug information. Disabled by default. */
    case debug

    /** General informational messages. Disabled by default. */
    case info

    /** Potentially harmful situations. Logged to stdout by default. */
    case warn

    /** Error events that might still allow the compilation to continue. Logged to stdout by default. */
    case error

    lazy val default: Out = this match
      case Level.trace | Level.debug | Level.info => Out.disabled
      case Level.warn | Level.error => Out.stdout

  object Level:
    given Showable[Level] = Showable(_.toString)
    // $COVERAGE-OFF$

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
  // $COVERAGE-ON$

  enum Out:
    case stdout, file, disabled

  object Out:
    given Showable[Out] = Showable(_.toString)
    // $COVERAGE-OFF$

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
  // $COVERAGE-ON$
