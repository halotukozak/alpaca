package example

import scala.collection.Factory
import scala.collection.immutable.SortedSet

enum Message:
  val msg: String
  val line: Int | Null

  case Error(msg: String, line: Int | Null = null)
  case Warning(msg: String, line: Int | Null = null)

  override def toString: String = this match
    case Message.Error(msg, line) => s"Error: $msg at line $line"
    case Message.Warning(msg, line) => s"Warn: $msg at line $line"

object Message:
  given Ordering[Message] = Ordering.fromLessThan: (m1, m2) =>
    if m1.line == null || m2.line == null then false
    else m1.line < m2.line

enum Result[+T]:
  case Success(value: T)
  case Failure(value: T, messages: SortedSet[Message])

  def get: T = this match
    case Success(value) => value
    case Failure(_, messages) =>
      throw RuntimeException(messages.toList.map(_.toString).mkString("\n")) // todo: can we not use .toList?

  def map[U](f: T => U): Result[U] = this match
    case Success(value) => Success(f(value))
    case Failure(value, messages) => Failure(f(value), messages)

  def flatMap[U](f: T => Result[U]): Result[U] = this match
    case Success(value) => f(value)
    case Failure(value, messages) =>
      f(value) match
        case Success(newValue) => Failure(newValue, messages)
        case Failure(newValue, newmessages) => Failure(newValue, messages ++ newmessages)

  def filter(predicate: T => Boolean, message: Message): Result[T] = this match
    case Success(value) if predicate(value) => this
    case Success(value) => Failure(value, SortedSet(message))
    case Failure(value, messages) if predicate(value) => Failure(value, messages)
    case Failure(value, messages) => Failure(value, messages + message)

  override def toString: String = this match
    case Result.Success(value) => s"$value"
    case Result.Failure(value, messages) =>
      s"""|$value
          |${messages.toList.mkString("\n").trim} //todo: can we not use .toList?
          |""".stripMargin

object Result:
  def apply[T](value: T) = Result.Success(value)

  given [T]: Conversion[T, Result[T]] = Result.Success(_)

  def warn[T](line: Int | Null = null)(value: T, warns: String*): Result[T] =
    Result.Failure(value, warns.map(Message.Warning(_, line)).to(SortedSet))

  def error[T](line: Int | Null = null)(value: T, errors: String*): Result[T] =
    Result.Failure(value, errors.map(Message.Error(_, line)).to(SortedSet))

  def warn[T](value: T, warns: String*) =
    Result.Failure(value, warns.map(Message.Warning(_)).to(SortedSet))

  def error[T](value: T, errors: String*) =
    Result.Failure(value, errors.map(Message.Error(_)).to(SortedSet.evidenceIterableFactory))

  inline def sequence[T, CC[X] <: IterableOnce[X]](col: CC[Result[T]]): Result[CC[T]] =
    traverse(col)(x => x)

  inline def traverse[T, U, CC[X] <: IterableOnce[X]](
    col: CC[T],
  )(
    inline f: T => Result[U],
  ): Result[CC[U]] =
    col.iterator
      .foldLeft(Result.Success(Seq.empty[U]): Result[Seq[U]]): (acc, res) =>
        for
          accValues <- acc
          value <- f(res)
        yield accValues :+ value
      .map(_.to(compiletime.summonInline[Factory[U, CC[U]]]))

  val unit = Success(())

extension (res: Result[?]) def discard: Result[Unit] = res.map(_ => ())
