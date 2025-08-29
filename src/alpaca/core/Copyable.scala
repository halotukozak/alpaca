package alpaca.core

import scala.annotation.implicitNotFound
import scala.deriving.Mirror

@implicitNotFound("${T} should be a case class.")
trait Copyable[T] {
  def apply(t: T): T
}

//todo: tests https://github.com/halotukozak/alpaca/issues/54
object Copyable {
  // todo: replace with semi-auto derivation https://github.com/halotukozak/alpaca/issues/54
  given [T <: Product: Mirror.ProductOf as m]: Copyable[T] = t => m.fromTuple(Tuple.fromProductTyped(t))
}
