package example

import scala.collection.{Factory, IterableFactory}

enum Message:
  val text: String
  val line: Int | Null

  case Error(msg: String, line: Int | Null = null)
  case Warning(msg: String, line: Int | Null = null)

enum Result[+T]:
  def value: T

  case Success(value: T)
  case Failure(value: T, messages: Message*)

  def map[U](f: T => U): Result[U] = this match
    case Success(value) => Success(f(value))
    case Failure(value, messages*) => Failure(f(value), messages*)

  def flatMap[U](f: T => Result[U]): Result[U] = this match
    case Success(value) => f(value)
    case Failure(value, messages*) =>
      f(value) match
        case Success(newValue) => Failure(newValue, messages*)
        case Failure(newValue, newmessages*) => Failure(newValue, messages ++ newmessages*)

  def filter(predicate: T => Boolean, message: Message): Result[T] =
    this match
      case r: Result[?] if predicate(r.value) => this
      case Success(value) => Failure(value, message)
      case Failure(_, messages*) => Failure(value, (messages :+ message)*)

object Result:
  def apply[T](value: T) = Result.Success(value)
  def warn[T](line: Int | Null = null)(value: T, warns: String*): Result[T] =
    Result.Failure(value, warns.map(Message.Warning(_, line))*)
  def error[T](line: Int | Null = null)(value: T, errors: String*): Result[T] =
    Result.Failure(value, errors.map(Message.Error(_, line))*)
  def warn[T <: AST.Tree](tree: T, warns: String*) =
    Result.Failure(tree, warns.map(Message.Warning(_, tree.line))*)
  def error[T <: AST.Tree](tree: T, errors: String*) =
    Result.Failure(tree, errors.map(Message.Error(_, tree.line))*)
  def warn[T](value: T, warns: String*) =
    Result.Failure(value, warns.map(Message.Warning(_))*)
  def error[T](value: T, errors: String*) =
    Result.Failure(value, errors.map(Message.Error(_))*)

  inline def sequence[T, CC[X] <: IterableOnce[X]](col: CC[Result[T]]): Result[CC[T]] =
    traverse(col)(identity)

  inline def traverse[T, U, CC[X] <: IterableOnce[X]](
    col: CC[Result[T]],
  )(
    f: T => U,
  ): Result[CC[U]] =
    col.iterator
      .foldLeft(Result.Success(Seq.empty[U]): Result[Seq[U]]) { (acc, res) =>
        for
          accValues <- acc
          value <- res
        yield accValues :+ f(value)
      }
      .to(compiletime.summonInline)

extension (res: Result[?]) def discard: Result[Unit] = res.map(_ => ())
