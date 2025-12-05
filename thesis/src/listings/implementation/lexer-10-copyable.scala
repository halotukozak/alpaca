@implicitNotFound("${T} should be a case class.")
trait Copyable[T] extends (T => T)
