package alpaca
package internal


/**
 * An opaque type representing a non-empty list.
 *
 * This is a type-safe wrapper around List that guarantees at compile time
 * that the list contains at least one element. It is a subtype of List[A]
 * so it can be used wherever a List is expected.
 *
 * @tparam A the element type
 */
opaque type NEL[+A] <: List[A] = List[A]

object NEL:

  /**
   * Creates a non-empty list from a head element and optional tail elements.
   *
   * @param head the first element (required)
   * @param tail additional elements (optional)
   * @return a new NEL
   */
  inline def apply[A](inline head: A, inline tail: A*): NEL[A] = head :: tail.toList

  /**
   * Pattern matching extractor for NEL.
   *
   * Extracts the head and tail of a non-empty list.
   *
   * @param list the list to deconstruct
   * @return (head, tail)
   */
  def unapply[A](list: NEL[A]): (A, List[A]) = (list.head, list.tail)

  /**
   * Unsafely converts a List to a NEL.
   *
   * This method performs a runtime check to ensure the list is non-empty.
   * Use with caution as it can throw an exception.
   *
   * @param list the list to convert
   * @return the list as a NEL
   * @throws IllegalArgumentException if the list is empty
   */
  private[internal] def unsafe[A](list: List[A]): NEL[A] =
    if list.isEmpty then throw IllegalArgumentException("Empty list cannot be converted to NEL")
    list

  // $COVERAGE-OFF$
  private[internal] given [A: {Type, ToExpr}]: ToExpr[NEL[A]] with
    def apply(x: NEL[A])(using Quotes): Expr[NEL[A]] =
      '{ NEL(${ Expr(x.head) }, ${ ToExpr.ListToExpr(x.tail) }*) }
// $COVERAGE-ON$