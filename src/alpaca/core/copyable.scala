package alpaca.core

import scala.deriving.Mirror

trait Copyable[T] {
  def apply(t: T): T
}

given [T <: Product: Mirror.ProductOf as m]: Copyable[T] = t => m.fromTuple(Tuple.fromProductTyped(t))
