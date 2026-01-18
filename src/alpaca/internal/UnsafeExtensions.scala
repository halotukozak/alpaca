package alpaca
package internal

import scala.collection.IterableOnceOps
import scala.quoted.*

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

extension [A, CC[_], C <: IterableOnceOps[A, CC, CC[A]]](col: C)(using Log, DebugPosition)
  inline private[alpaca] def unsafeMap[B: Default](inline f: PartialFunction[A, B]): CC[B] =
    col.map(f.unsafeApply)

  inline private[alpaca] def unsafeFlatMap[B](inline f: PartialFunction[A, IterableOnce[B]]): CC[B] =
    col.flatMap(f.unsafeApply)

  inline private[alpaca] def unsafeFoldLeft[B: Default](z: B)(inline op: PartialFunction[(B, A), B]): B =
    col.foldLeft(z)((b, a) => op.unsafeApply((b, a)))

extension [A: Default](opt: Option[A])(using Log, DebugPosition)
  inline private[alpaca] def unsafeGet: A =
    opt.getOrElse(raiseShouldNeverBeCalled("None"))

extension [A, B: Default](pf: PartialFunction[A, B])(using Log, DebugPosition)
  inline private[alpaca] def unsafeApply(a: A): B =
    pf.applyOrElse(a, raiseShouldNeverBeCalled)
