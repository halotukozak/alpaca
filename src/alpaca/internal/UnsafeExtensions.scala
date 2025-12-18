package alpaca
package internal

import scala.collection.IterableOnceOps
import scala.quoted.*

inline private[alpaca] def raiseShouldNeverBeCalled[E](inline elem: E)[T](using default: T has Default) =
  val elemShown = compiletime
    .summonFrom:
      case s: (E is Showable) => s
      case _ => Showable.fromToString
    .show(elem)

  val line = compiletime.summonInline[DebugPosition]

  val message = show"This code should never be called: $elemShown at line $line"

  compiletime.summonFrom:
    case quotes: Quotes => quotes.reflect.report.error(message)
    case _ => throw new AlgorithmError(message)

  default()

extension [A, CC[_], C <: IterableOnceOps[A, CC, CC[A]]](col: C)
  inline private[alpaca] def unsafeMap[B](
    inline f: PartialFunction[A, B],
  )(using B has Default,
  ): CC[B] =
    col.map(f.applyOrElse(_, raiseShouldNeverBeCalled))

  inline private[alpaca] def unsafeFlatMap[B](
    inline f: PartialFunction[A, IterableOnce[B]],
  )(using IterableOnce[B] has Default,
  ): CC[B] =
    col.flatMap(f.applyOrElse(_, raiseShouldNeverBeCalled))

  inline private[alpaca] def unsafeFoldLeft[B](z: B)(inline op: PartialFunction[(B, A), B])(using B has Default): B =
    col.foldLeft(z)((b, a) => op.applyOrElse((b, a), raiseShouldNeverBeCalled))

extension [A](opt: Option[A])
  inline private[alpaca] def unsafeGet(using A has Default): A =
    opt.getOrElse(raiseShouldNeverBeCalled("None"))

extension [A, B](pf: PartialFunction[A, B])
  inline private[alpaca] def unsafeApply(a: A)(using B has Default): B =
    pf.applyOrElse(a, raiseShouldNeverBeCalled)
