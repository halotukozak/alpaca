package alpaca.core

import scala.annotation.implicitNotFound
import scala.deriving.Mirror

@implicitNotFound("${T} should be a case class.")
trait Copyable[T] extends (T => T)

object Copyable {
  def derived[T <: Product: Mirror.ProductOf as m]: Copyable[T] = t => m.fromTuple(Tuple.fromProductTyped(t))
}
