package alpaca.internal

import scala.collection.{Factory, immutable, mutable}
import scala.reflect.ClassTag

opaque private[alpaca] type RevertedArray[T] = Array[T]

private[alpaca] object RevertedArray:
  def empty[T: ClassTag]: RevertedArray[T] = Array.empty[T]

  def apply[T](arr: Array[T]): RevertedArray[T] = arr

  def unapplySeq[T](x: RevertedArray[T]): Array.UnapplySeqWrapper[T] = Array.unapplySeq(x)

  extension [T](self: RevertedArray[T])
    def apply(idx: Int): T = self(self.length - 1 - idx)
    def head: T = self(0)

  import scala.language.implicitConversions
  implicit def toFactory[A: ClassTag](dummy: RevertedArray.type): Factory[A, RevertedArray[A]] = Array.toFactory(Array)
