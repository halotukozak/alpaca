package alpaca.core

import scala.deriving.Mirror
import scala.annotation.implicitNotFound

@implicitNotFound("${T} should be a case class.")
trait Copyable[T] {
  def apply(t: T): T
}

object Copyable {
  given [T <: Product: Mirror.ProductOf as m]: Copyable[T] = t => m.fromTuple(Tuple.fromProductTyped(t))
}
