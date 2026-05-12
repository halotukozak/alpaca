package alpaca
package internal
package lexer
package regex

import alpaca.internal.lexer.regex.Regex.*

import scala.collection.immutable.SortedSet
import scala.util.control.TailCalls.{done, tailcall, TailRec}

/**
 * Opaque view over a [[Regex]] exposing Brzozowski-derivative based language emptiness
 * and subset operations.
 *
 * `a.subset(b)` decides whether `L(a) ⊆ L(b)` by checking emptiness of `a ∩ ¬b`.
 * Termination relies on smart-constructor normalization in [[Regex]] keeping the
 * derivative state set finite up to similarity.
 */
opaque private[lexer] type Subset = Regex

private[lexer] object Subset:

  /** Lifts an existing [[Regex]] into [[Subset]]. */
  def of(r: Regex): Subset = r

  /** Parses a pattern into a [[Subset]]. */
  def parse(pattern: String): Either[RegexParseError, Subset] = RegexParser.parse(pattern).map(of)

  /** The empty-language subset; reference-equal to [[Regex.Empty]] under the opaque type. */
  val empty: Subset = of(Regex.Empty)

  extension (a: Subset)
    /** Underlying [[Regex]]. */
    def underlying: Regex = a

    /** `Σ*`-extended view — matches every string having `a` as prefix. */
    def withAnySuffix: Subset = a.concat(Regex.all)

    /** `true` iff `L(a) ⊆ L(b)`. */
    def subset(b: Subset): Boolean = (a & !b).isEmpty

    /** `true` iff `L(a) = ∅`. */
    def isEmpty: Boolean = isEmptyImpl(a)

    /** `true` iff `ε ∈ L(a)`. */
    def nullable: Boolean = nullableImpl(a).result

    /** Brzozowski derivative of `a` with respect to code point `c`. */
    def derive(c: Int): Subset = deriveImpl(a, c).result

  private def isEmptyImpl(r: Regex): Boolean =
    def loop(queue: List[Regex], visited: Set[Regex]): TailRec[Boolean] = queue match
      case Nil => done(true)
      case s :: rest =>
        for
          isNull <- tailcall(nullableImpl(s))
          out <-
            if isNull then done(false)
            else
              for
                derived <- tailcall(deriveAt(partitionReps(s), s, Nil))
                next = derived.filterNot(visited.contains)
                r <- tailcall(loop(rest ::: next, visited ++ next))
              yield r
        yield out
    loop(List(r), Set(r)).result

  private def nullableImpl(r: Regex): TailRec[Boolean] = r match
    case Eps => done(true)
    case Empty | Chars(_) => done(false)
    case Concat(a, b) =>
      for
        na <- tailcall(nullableImpl(a))
        out <- if na then tailcall(nullableImpl(b)) else done(false)
      yield out
    case Alt(parts) => anyNullable(parts.toList)
    case Inter(parts) => allNullable(parts.toList)
    case Star(_) => done(true)
    case Compl(inner) => tailcall(nullableImpl(inner)).map(!_)

  private def anyNullable(parts: List[Regex]): TailRec[Boolean] = parts match
    case Nil => done(false)
    case head :: tail =>
      for
        hd <- tailcall(nullableImpl(head))
        out <- if hd then done(true) else tailcall(anyNullable(tail))
      yield out

  private def allNullable(parts: List[Regex]): TailRec[Boolean] = parts match
    case Nil => done(true)
    case head :: tail =>
      for
        hd <- tailcall(nullableImpl(head))
        out <- if !hd then done(false) else tailcall(allNullable(tail))
      yield out

  private def deriveImpl(r: Regex, c: Int): TailRec[Regex] = r match
    case Eps | Empty => done(Empty)
    case Chars(set) => done(if set.contains(c) then Eps else Empty)
    case Concat(a, b) =>
      for
        da <- tailcall(deriveImpl(a, c))
        na <- tailcall(nullableImpl(a))
        out <-
          if na then tailcall(deriveImpl(b, c)).map(db => (da.concat(b)) | db)
          else done(da.concat(b))
      yield out
    case Alt(parts) => deriveAll(parts.toList, c, Nil).map(Regex.alt)
    case Inter(parts) => deriveAll(parts.toList, c, Nil).map(Regex.inter)
    case s @ Star(inner) => tailcall(deriveImpl(inner, c)).map(d => d.concat(s))
    case Compl(inner) => tailcall(deriveImpl(inner, c)).map(!_)

  private def deriveAll(parts: List[Regex], c: Int, acc: List[Regex]): TailRec[List[Regex]] = parts match
    case Nil => done(acc)
    case head :: tail =>
      for
        d <- tailcall(deriveImpl(head, c))
        out <- tailcall(deriveAll(tail, c, d :: acc))
      yield out

  private def deriveAt(reps: List[Int], r: Regex, acc: List[Regex]): TailRec[List[Regex]] = reps match
    case Nil => done(acc)
    case c :: tail =>
      for
        d <- tailcall(deriveImpl(r, c))
        out <- tailcall(deriveAt(tail, r, d :: acc))
      yield out

  /**
   * Returns one representative code point per equivalence class of the alphabet
   * partition induced by the character sets in `r`. Within a class, derivatives
   * yield the same residual, so testing one representative suffices.
   */
  private def partitionReps(r: Regex): List[Int] =
    val boundaries = collectBoundaries(r, SortedSet[Int](0, CharSet.maxCodePoint + 1)).result.toVector
    boundaries.init.toList

  private def collectBoundaries(r: Regex, acc: SortedSet[Int]): TailRec[SortedSet[Int]] = r match
    case Eps | Empty => done(acc)
    case Chars(set) => tailcall(addBoundaries(set.ranges.toList, acc))
    case Concat(a, b) =>
      for
        a1 <- tailcall(collectBoundaries(a, acc))
        a2 <- tailcall(collectBoundaries(b, a1))
      yield a2
    case Alt(parts) => collectAllBoundaries(parts.toList, acc)
    case Inter(parts) => collectAllBoundaries(parts.toList, acc)
    case Star(inner) => tailcall(collectBoundaries(inner, acc))
    case Compl(inner) => tailcall(collectBoundaries(inner, acc))

  private def addBoundaries(ranges: List[Range], acc: SortedSet[Int]): TailRec[SortedSet[Int]] =
    ranges match
      case Nil => done(acc)
      case head :: tail => tailcall(addBoundaries(tail, acc + head.lo + (head.hi + 1)))

  private def collectAllBoundaries(parts: List[Regex], acc: SortedSet[Int]): TailRec[SortedSet[Int]] =
    parts match
      case Nil => done(acc)
      case head :: tail =>
        for
          next <- tailcall(collectBoundaries(head, acc))
          out <- tailcall(collectAllBoundaries(tail, next))
        yield out
