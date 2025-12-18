package alpaca
package internal

private[internal] trait Default:
  type Self
  def apply(): Self

private[internal] object Default:
  given Any has Default = () => ()
  given Unit has Default = () => ()
  given Boolean has Default = () => false
  given Int has Default = () => 0
  given String has Default = () => ""
  given [T]: ((T | Null) has Default) = () => null
  given [T](using quotes: Quotes): (Expr[T] has Default) = () => '{ ??? }
  given [T](using quotes: Quotes): (Type[T] has Default) = () => Type.of[Nothing].asInstanceOf[Type[T]]
  given [T]: (IterableOnce[T] has Default) = () => Iterable.empty
  given [T]: (List[T] has Default) = () => Nil
  given [T]: (Option[T] has Default) = () => None

  given (using quotes: Quotes): (quotes.reflect.Tree has Default) =
    import quotes.reflect.*
    () => '{ ??? }.asTerm

  given (using quotes: Quotes): (quotes.reflect.TypeRepr has Default) =
    import quotes.reflect.*
    () => TypeRepr.of[Nothing]

  inline given [T <: Tuple]: Tuple.Map[T, [X] =>> X has Default] =
    compiletime.summonAll[Tuple.Map[T, [X] =>> X has Default]]

  given [T <: Tuple](using defaults: Tuple.Map[T, [X] =>> X has Default]): (T has Default) = () =>
    defaults.toList
      .asInstanceOf[List[Default]]
      .map(_.apply())
      .toArray
      .|>(Tuple.fromArray)
      .asInstanceOf[T]

  given [R](using default: R has Default): (Function0[R] has Default) =
    () => () => default()

  given [R](using default: R has Default): (Function1[?, R] has Default) =
    () => _ => default()

  given [R](using default: R has Default): (Function2[?, ?, R] has Default) =
    () => (_, _) => default()
