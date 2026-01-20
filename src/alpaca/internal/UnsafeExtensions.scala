package alpaca
package internal

import scala.collection.IterableOnceOps
import scala.quoted.*

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
inline private[alpaca] def raiseShouldNeverBeCalled[T: Default](elem: Any)(using Log, DebugPosition): T =
  val shown =
    compiletime
      .summonFrom:
        case s: Showable[elem.type] => s
        case _ => Showable.fromToString
      .show(elem)

  val message = show"This code should never be called: $shown at ${summon[DebugPosition]}"

  compiletime.summonFrom:
    case quotes: Quotes => quotes.reflect.report.error(message)
    case _ => throw new AlgorithmError(message)

  Default[T]()

/**
 * Extension methods for collections that use partial functions unsafely.
 *
 * These methods assume the partial function will always be defined for all
 * elements in the collection. If not, they raise a compilation error during 
 * macro expansion (compile-time) or throw an AlgorithmError at runtime.
 */
extension [A, CC[_], C <: IterableOnceOps[A, CC, CC[A]]](col: C)(using Log, DebugPosition)
  /**
   * Maps over a collection using a partial function.
   *
   * The partial function must be defined for all elements, otherwise
   * a compilation error or runtime exception will be raised.
   *
   * @tparam B the result element type
   * @param f the partial function to apply
   * @return the mapped collection
   */
  inline private[alpaca] def unsafeMap[B: Default](inline f: PartialFunction[A, B]): CC[B] =
    col.map(f.unsafeApply)

  /**
   * FlatMaps over a collection using a partial function.
   *
   * The partial function must be defined for all elements, otherwise
   * a compilation error or runtime exception will be raised.
   *
   * @tparam B the result element type
   * @param f the partial function to apply
   * @return the flat-mapped collection
   */
  inline private[alpaca] def unsafeFlatMap[B](inline f: PartialFunction[A, IterableOnce[B]]): CC[B] =
    col.flatMap(f.unsafeApply)

  /**
   * Folds over a collection using a partial function.
   *
   * The partial function must be defined for all (accumulator, element) pairs,
   * otherwise a compilation error or runtime exception will be raised.
   *
   * @tparam B the result type
   * @param z the initial value
   * @param op the partial function to apply
   * @return the folded result
   */
  inline private[alpaca] def unsafeFoldLeft[B: Default](z: B)(inline op: PartialFunction[(B, A), B]): B =
    col.foldLeft(z)((b, a) => op.unsafeApply((b, a)))

/**
 * Extension method for Option that assumes the value is present.
 *
 * Raises a compilation error or runtime exception if the Option is None.
 */
extension [A: Default](opt: Option[A])(using Log, DebugPosition)
  /**
   * Gets the value from an Option.
   *
   * If the Option is None, raises a compilation error during macro expansion
   * or throws an AlgorithmError at runtime.
   *
   * @return the value
   */
  inline private[alpaca] def unsafeGet: A =
    opt.getOrElse(raiseShouldNeverBeCalled("None"))

/**
 * Extension method for partial functions.
 *
 * Allows applying a partial function as if it were total, with error handling
 * for undefined cases.
 */
extension [A, B: Default](pf: PartialFunction[A, B])(using Log, DebugPosition)
  /**
   * Applies a partial function.
   *
   * If the partial function is not defined for the input, raises a compilation 
   * error during macro expansion or throws an AlgorithmError at runtime.
   *
   * @param a the input value
   * @return the result
   */
  inline private[alpaca] def unsafeApply(a: A): B =
    pf.applyOrElse(a, raiseShouldNeverBeCalled)
