package alpaca
package internal

import scala.collection.IterableOnceOps
import scala.quoted.*

/**
 * Raises a compilation error or runtime exception for unreachable code.
 *
 * This function is used to mark code paths that should theoretically never
 * be reached. If they are reached, it indicates a bug in the library.
 * During compilation, it reports an error; at runtime, it throws an exception.
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
 * elements in the collection. If not, they raise a compilation error or
 * runtime exception.
 */
extension [A, CC[_], C <: IterableOnceOps[A, CC, CC[A]]](col: C)(using Log, DebugPosition)
  /**
   * Maps over a collection using a partial function.
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
   * @tparam B the result element type
   * @param f the partial function to apply
   * @return the flat-mapped collection
   */
  inline private[alpaca] def unsafeFlatMap[B](inline f: PartialFunction[A, IterableOnce[B]]): CC[B] =
    col.flatMap(f.unsafeApply)

  /**
   * Folds over a collection using a partial function.
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
 */
extension [A: Default](opt: Option[A])(using Log, DebugPosition)
  /**
   * Gets the value from an Option, raising an error if None.
   *
   * @return the value
   */
  inline private[alpaca] def unsafeGet: A =
    opt.getOrElse(raiseShouldNeverBeCalled("None"))

/**
 * Extension method for partial functions.
 */
extension [A, B: Default](pf: PartialFunction[A, B])(using Log, DebugPosition)
  /**
   * Applies a partial function, raising an error if undefined.
   *
   * @param a the input value
   * @return the result
   */
  inline private[alpaca] def unsafeApply(a: A): B =
    pf.applyOrElse(a, raiseShouldNeverBeCalled)
