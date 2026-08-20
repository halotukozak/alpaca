package halotukozak
package alpaca
package internal

/**
 * Raises a compilation error or runtime exception for unreachable code.
 *
 * This function is used to mark code paths that should theoretically never
 * be reached. During macro expansion (compile-time), it reports a compilation error.
 * If the code path is somehow reached at runtime, it throws an AlgorithmError.
 *
 * @tparam T the return type (a default value is provided)
 * @param elem the element that triggered the error
 * @return a default value of type T (never actually returns)
 */
inline private[alpaca] def raiseShouldNeverBeCalled[T](
  elem: Any,
)(using
  showable: Showable[elem.type] = Showable.fromToString,
  pos: Position,
): T =
  val message = show"This code should never be called: $elem at $pos"
  throw AlgorithmError(message)
