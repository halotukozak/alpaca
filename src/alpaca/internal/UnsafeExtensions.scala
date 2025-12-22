package alpaca
package internal

import scala.collection.IterableOnceOps
import scala.quoted.*

inline private[alpaca] def raiseShouldNeverBeCalled[T: Default as default](elem: Any) =
  val elemShown = compiletime
    .summonFrom:
      case s: (elem.type is Showable) => s
      case _ => Showable.fromToString
    .show(elem)

  val pos = compiletime.summonInline[DebugPosition]

  val message = show"This code should never be called: $elemShown $pos"

  compiletime.summonFrom:
    case quotes: Quotes => quotes.reflect.report.error(message)
    case _ => throw new AlgorithmError(message)

  default()

extension [A, CC[_], C <: IterableOnceOps[A, CC, CC[A]]](col: C)
  inline private[alpaca] def unsafeMap[B: Default](inline f: PartialFunction[A, B]): CC[B] =
    col.map(f.applyOrElse(_, raiseShouldNeverBeCalled))

  inline private[alpaca] def unsafeFlatMap[B](
    inline f: PartialFunction[A, IterableOnce[B]],
  )(using IterableOnce[B] has Default,
  ): CC[B] =
    col.flatMap(f.applyOrElse(_, raiseShouldNeverBeCalled))

  inline private[alpaca] def unsafeFoldLeft[B: Default](z: B)(inline op: PartialFunction[(B, A), B]): B =
    col.foldLeft(z)((b, a) => op.applyOrElse((b, a), raiseShouldNeverBeCalled))

extension [A: Default](opt: Option[A])
  inline private[alpaca] def unsafeGet: A = opt.getOrElse(raiseShouldNeverBeCalled("None"))

extension [A, B: Default](pf: PartialFunction[A, B])
  inline private[alpaca] def unsafeApply(a: A): B = pf.applyOrElse(a, raiseShouldNeverBeCalled)
