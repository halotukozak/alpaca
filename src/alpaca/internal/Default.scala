package alpaca
package internal

import scala.collection.IterableFactory
import scala.collection.Factory
import scala.annotation.nowarn

private[internal] trait Default[+T] extends (() => T):
  def transform[A](f: T => A): Default[A] = () => f(apply())

private[internal] object Default:
  def apply[T](using d: Default[T]): Default[T] = d

  given Default[Unit] = () => ()
  given Default[Boolean] = () => false
  given Default[Int] = () => 0
  given Default[String] = () => ""
  given Default[ValidName] = () => ""
  given [I[X] <: Iterable[X]](using factory: Factory[Nothing, I[Nothing]]): Default[I[Nothing]] =
    () => factory.fromSpecific(Nil)
  given Default[IterableOnce[Nothing]] = () => Iterable.empty
  given Default[Option[Nothing]] = () => None

  given [T](using quotes: Quotes): Default[Expr[T]] = () => '{ ??? }

  given [T <: AnyKind](using quotes: Quotes): Default[Type[T]] = () => Type.of[Nothing].asInstanceOf[Type[T]]

  given (using quotes: Quotes): Default[quotes.reflect.TypeRepr] =
    import quotes.reflect.*
    () => TypeRepr.of[Nothing]

  given (using quotes: Quotes): Default[quotes.reflect.Symbol] =
    import quotes.reflect.*
    () => Symbol.noSymbol

  given [H: Default as head, T <: Tuple: Default as tail]: Default[H *: T] = () => head() *: tail()

  given Default[EmptyTuple] = () => EmptyTuple
