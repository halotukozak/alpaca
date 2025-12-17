package alpaca
package internal

private[internal] trait Default:
  type Self
  def apply(): Self

private[internal] object Default:
  type Aux[A] = A has Default

  given Unit has Default = () => ()
  given Boolean has Default = () => false
  given Int has Default = () => 0
  given String has Default = () => ""
  given Nothing has Default = () => throw new NoSuchElementException("Default is not defined for Nothing")

  given [T] => ((Quotes) ?=> Expr[T] has Default) = () => '{ ??? }
  given [T] => ((Quotes) ?=> List[T] has Default) = () => Nil

  given ((quotes: Quotes) ?=> quotes.reflect.Tree has Default) =
    import quotes.reflect.*
    () => '{ ??? }.asTerm

  given ((quotes: Quotes) ?=> quotes.reflect.TypeRepr has Default) =
    import quotes.reflect.*
    () => TypeRepr.of[Nothing]

  given [T <: Tuple] => ((defaults: Tuple.Map[T, Default.Aux]) ?=> T has Default) =
    () =>
      defaults.toList
        .asInstanceOf[List[Default]]
        .map(_.apply())
        .toArray
        .|>(Tuple.fromArray)
        .asInstanceOf[T]
