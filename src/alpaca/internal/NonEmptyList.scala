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
opaque type NonEmptyList[+A] <: List[A] = List[A]

object NonEmptyList {

  /**
   * Creates a non-empty list from a head element and optional tail elements.
   *
   * @param head the first element (required)
   * @param tail additional elements (optional)
   * @return a new NonEmptyList
   */
  inline def apply[A](inline head: A, inline tail: A*): NonEmptyList[A] = head :: tail.toList

  /**
   * Pattern matching extractor for NonEmptyList.
   *
   * Extracts the head and tail of a non-empty list.
   *
   * @param list the list to deconstruct
   * @return Some((head, tail)) if the list is non-empty, None otherwise
   */
  def unapply[A](list: NonEmptyList[A]): Option[(A, List[A])] = list match
    case head :: tail => Some((head, tail))
    case _ => None

  /**
   * Unsafely converts a List to a NonEmptyList.
   *
   * This method performs a runtime check to ensure the list is non-empty.
   * Use with caution as it can throw an exception.
   *
   * @param list the list to convert
   * @return the list as a NonEmptyList
   * @throws IllegalArgumentException if the list is empty
   */
  private[internal] def unsafe[A](list: List[A]): NonEmptyList[A] = list match
    case head :: tail => list
    case Nil => throw IllegalArgumentException("Empty list cannot be converted to NonEmptyList")

  private[internal] given [A: {Type, ToExpr}]: ToExpr[NonEmptyList[A]] with
    def apply(x: NonEmptyList[A])(using Quotes): Expr[NonEmptyList[A]] =
      '{ NonEmptyList(${ Expr(x.head) }, ${ ToExpr.ListToExpr(x.tail) }*) }
}
