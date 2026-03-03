package example

import scala.collection.Factory
import scala.collection.immutable.SortedSet

enum Message:
  val msg: String
  val line: Int

  case Error(msg: String, line: Int)
  case Warning(msg: String, line: Int)

  override def toString: String = this match
    case Message.Error(msg, line) => s"Error: $msg at line $line"
    case Message.Warning(msg, line) => s"Warn: $msg at line $line"

object Message:
  given Ordering[Message] = Ordering.by(_.line)

enum Result[+T]:
  case Success(value: T)
  case Failure(value: T, messages: SortedSet[Message])

  def get: T = this match
    case Success(value) => value
    case Failure(_, messages) => throw RuntimeException(messages.toList.map(_.toString).mkString("\n"))

  def map[U](f: T => U): Result[U] = this match
    case Success(value) => Success(f(value))
    case Failure(value, messages) => Failure(f(value), messages)

  def flatMap[U](f: T => Result[U]): Result[U] = this match
    case Success(value) => f(value)
    case Failure(value, messages) =>
      f(value) match
        case Success(newValue) => Failure(newValue, messages)
        case Failure(newValue, newMessages) => Failure(newValue, messages ++ newMessages)

  def filter(predicate: T => Boolean, message: Message): Result[T] = this match
    case Success(value) if predicate(value) => this
    case Success(value) => Failure(value, SortedSet(message))
    case Failure(value, messages) if predicate(value) => Failure(value, messages)
    case Failure(value, messages) => Failure(value, messages + message)

  override def toString: String = this match
    case Result.Success(value) => s"$value"
    case Result.Failure(value, messages) =>
      s"""|$value
          |${messages.toList.mkString("\n").trim}
          |""".stripMargin

object Result:
  def apply[T](value: T) = Result.Success(value)

  def warn[T](line: Int)(value: T, warns: String*): Result[T] =
    Result.Failure(value, warns.map(Message.Warning(_, line)).to(SortedSet))

  def error[T](line: Int)(value: T, errors: String*): Result[T] =
    Result.Failure(value, errors.map(Message.Error(_, line)).to(SortedSet))

  inline def sequence[T, CC[X] <: IterableOnce[X]](col: CC[Result[T]]): Result[CC[T]] =
    traverse(col)(identity)

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
