package alpaca.core

import scala.annotation.implicitNotFound
import scala.deriving.Mirror

@implicitNotFound("${T} should be a case class.")
private[alpaca] trait Copyable[T] extends (T => T)

private[alpaca] object Copyable {
  given derived[T <: Product: Mirror.ProductOf as m]: Copyable[T] = t => m.fromTuple(Tuple.fromProductTyped(t))
}
