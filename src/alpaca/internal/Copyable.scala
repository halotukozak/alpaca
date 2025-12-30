package alpaca
package internal

import scala.annotation.implicitNotFound
import scala.deriving.Mirror

/**
 * A type class for creating copies of case class instances.
 *
 * This trait provides a function to copy an instance of type T.
 * It is primarily used internally for creating modified copies of context objects.
 *
 * @tparam T the type to copy, must be a case class
 */
@implicitNotFound("${T} should be a case class.")
private[alpaca] trait Copyable[T] extends (T => T)

private[alpaca] object Copyable:

  /**
   * Automatically derives a Copyable instance for any Product type (case class).
   *
   * @tparam T the Product type to derive Copyable for
   * @param m the Mirror.ProductOf for type T
   * @return a Copyable instance that can create copies
   */
  given derived[T <: Product: Mirror.ProductOf as m]: Copyable[T] = t => m.fromTuple(Tuple.fromProductTyped(t))
