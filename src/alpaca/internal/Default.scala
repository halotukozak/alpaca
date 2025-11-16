package alpaca
package internal

private[internal] trait Default[+T] extends (() => T)

private[internal] object Default:
  given Default[Unit] = () => ()
  given Default[Boolean] = () => false
  given Default[Int] = () => 0
  given Default[String] = () => ""
  given Default[Nothing] = () => throw new NoSuchElementException("Default[Nothing] is not defined")

  given [T](using quotes: Quotes): Default[Expr[T]] = () => '{ ??? }
  given [T](using quotes: Quotes): Default[List[T]] = () => Nil

  given (using quotes: Quotes): Default[quotes.reflect.Tree] =
    import quotes.reflect.*
    () => '{ ??? }.asTerm

  given (using quotes: Quotes): Default[quotes.reflect.TypeRepr] =
    import quotes.reflect.*
    () => TypeRepr.of[Nothing]

  given [T <: Tuple](using defaults: Tuple.Map[T, Default]): Default[T] =
    () =>
      defaults.toList
        .asInstanceOf[List[Default[?]]]
        .map(_.apply())
        .toArray
        .|>(Tuple.fromArray)
        .asInstanceOf[T]
