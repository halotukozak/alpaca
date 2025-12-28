package alpaca
package internal

import scala.collection.IterableOnceOps
import scala.quoted.*

inline private[alpaca] def raiseShouldNeverBeCalled[E](elem: E)[T]: T =
  val showable = compiletime.summonFrom:
    case s: Showable[E] => s
    case _ => Showable.fromToString

  val message = "This code should never be called: " + showable.show(elem)

  compiletime.summonFrom:
    case quotes: Quotes => quotes.reflect.report.error(message)
    case _ => throw new AlgorithmError(message)

  compiletime.summonInline[Default[T]]()

extension [A, CC[_], C <: IterableOnceOps[A, CC, CC[A]]](col: C)
  inline private[alpaca] def unsafeMap[B: Default](inline f: PartialFunction[A, B]): CC[B] =
    col.map(f.unsafeApply)

  inline private[alpaca] def unsafeFlatMap[B](inline f: PartialFunction[A, IterableOnce[B]]): CC[B] =
    col.flatMap(f.unsafeApply)

  inline private[alpaca] def unsafeFoldLeft[B: Default](z: B)(inline op: PartialFunction[(B, A), B]): B =
    col.foldLeft(z)((b, a) => op.unsafeApply((b, a)))

extension [A: Default](opt: Option[A])
  inline private[alpaca] def unsafeGet: A =
    opt.getOrElse(raiseShouldNeverBeCalled("None"))

extension [A, B: Default](pf: PartialFunction[A, B])
  inline private[alpaca] def unsafeApply(a: A): B =
    pf.applyOrElse(a, raiseShouldNeverBeCalled)
