package alpaca
package internal

import ox.*

import java.io.{BufferedWriter, FileWriter}
import java.nio.file.{Files, Path}
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.ConcurrentHashMap

import Log.*

class Log(using val debugSettings: DebugSettings)(using Ox) extends AutoCloseable:
  private given Log = this

  private val writerCache = new ConcurrentHashMap[Path, AtomicReference[BufferedWriter]]

  fork:
    sleep(debugSettings.compilationTimeout.runtimeChecked)
    writerCache.values.forEach: writer =>
      try writer.get.flush()
      catch case _: Exception => ()

  override def close(): Unit = writerCache.values.forEach(_.get.close())

  private def createWriter(path: Path, replace: Boolean): AtomicReference[BufferedWriter] =
    if path.getParent != null then Files.createDirectories(path.getParent)
    new AtomicReference(new BufferedWriter(new FileWriter(path.toFile, !replace)))

  def append(path: Path)(content: Shown): Unit =
    writerCache.computeIfAbsent(path, p => createWriter(p, false)).get.write(content)

  def replace(path: Path)(content: Shown): Unit =
    writerCache
      .compute(
        path,
        (p, existing) =>
          if existing != null then existing.synchronized(existing.get.close())
          createWriter(p, true),
      )
      .get
      .write(content)

  inline def toFile(path: String, replace: Boolean)(content: Shown): Unit =
    val file = Path.of(debugSettings.debugDirectory).resolve(path)
    if replace then this.replace(file)(content) else this.append(file)(content)

  private def log(level: Level, msg: Shown)(using pos: DebugPosition): Unit = debugSettings.logOut(level) match
    case Out.stdout => println(show"$level: $pos\t$msg")
    case Out.file => toFile(show"${pos.file}.log", false)(show"at ${pos.line}\t$msg\n")
    case Out.disabled => ()

object Log:
  inline def materialize = ${ materializeImpl }
  private def materializeImpl(using quotes: Quotes): Expr[Log] =
    val debugSettings = Expr(summon[DebugSettings])
    '{ supervised(new Log(using $debugSettings)) }

  inline def trace(inline msg: Shown)(using DebugPosition, Log): Unit = summon[Log].log(Level.trace, msg)
  inline def debug(inline msg: Shown)(using DebugPosition, Log): Unit = summon[Log].log(Level.debug, msg)
  inline def info(inline msg: Shown)(using DebugPosition, Log): Unit = summon[Log].log(Level.info, msg)
  inline def warn(inline msg: Shown)(using DebugPosition, Log): Unit = summon[Log].log(Level.warn, msg)
  inline def error(inline msg: Shown)(using DebugPosition, Log): Unit = summon[Log].log(Level.error, msg)

  inline def toFile(path: String, replace: Boolean)(content: Shown)(using Log): Unit =
    summon[Log].toFile(path, replace)(content)

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
