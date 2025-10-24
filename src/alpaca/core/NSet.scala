package alpaca.core

opaque type NSet[N <: Int, A] <: Set[A] = Set[A]

object NSet:
  def apply[T <: Tuple](tuple: T)(using size: ValueOf[Tuple.Size[T]]): NSet[Tuple.Size[T], Tuple.Union[T]] =
    tuple.toList.toSet
      .tap: set =>
        assert(set.size == size.value, s"Elements in NSet must be unique, but got: $set")
      .asInstanceOf[NSet[Tuple.Size[T], Tuple.Union[T]]]
