package alpaca
package internal
package lexer
package regex

import alpaca.internal.lexer.regex.Regex.{Compl, Star}

import scala.annotation.tailrec
import scala.quoted.{Expr, Quotes, ToExpr, Varargs}
import scala.util.control.TailCalls.{done, tailcall, TailRec}

/**
 * Cross-platform symbolic regex algebra used by [[RegexChecker]] for shadow detection.
 *
 * Construct via smart factories on the companion ([[Regex.lit]], [[Regex.literal]],
 * [[Regex.range]], [[Regex.apply]], [[Regex.alt]], [[Regex.inter]], [[Regex.all]]) and
 * member combinators (`a concat b`, `a | b`, `a & b`, `!a`, `r.star`, `r.repeat(n, m)`).
 * Raw case class constructors are inaccessible — only pattern matching via the
 * generated `unapply` methods is allowed.
 *
 * Supports literals, escapes (\d \D \s \S \w \W \t \n \r and meta-escapes),
 * character classes (including ranges and negation), `.`, alternation `|`,
 * non-capturing-style groups `(...)`, quantifiers `*` `+` `?` `{n}` `{n,}` `{n,m}`.
 *
 * Unsupported (parser returns [[RegexParseError.UnsupportedFeature]]):
 * anchors `^` `$` `\b` `\B`, lookaround `(?=` `(?!` `(?<=` `(?<!`, backreferences `\1`..`\9`.
 */
private[lexer] sealed trait Regex

private[lexer] object Regex:
  def apply(set: CharSet): Regex = if set.isEmpty then Empty else Chars(set)

  /** Matches the empty string ε. */
  case object Eps extends Regex

  /** Matches no string. */
  case object Empty extends Regex

  /** Matches any single code point in `set`. */
  final case class Chars private[Regex] (set: CharSet) extends Regex

  /** Concatenation `a · b`. */
  final case class Concat private[Regex] (a: Regex, b: Regex) extends Regex

  /** Alternation. Stored as a [[Set]] for ACI normalization. Always size ≥ 2. */
  final case class Alt private[Regex] (parts: Set[Regex]) extends Regex

  /** Intersection. Stored as a [[Set]] for ACI normalization. Always size ≥ 2. */
  final case class Inter private[Regex] (parts: Set[Regex]) extends Regex

  /** Kleene star `r*`. */
  final case class Star private[Regex] (r: Regex) extends Regex

  /** Complement `¬r`. */
  final case class Compl private[Regex] (r: Regex) extends Regex

  extension (r: Regex)

    /** Concatenation: `this · other`. */
    infix def concat(other: Regex): Regex = Regex.concatImpl(r, other).result

    /** Alternation: `this | other`. */
    infix def |(other: Regex): Regex = Regex.alt(Set(r, other))

    /** Intersection: `this ∩ other`. */
    infix def &(other: Regex): Regex = Regex.inter(Set(r, other))

    /** Complement: `¬this`. */
    def unary_! : Regex = r match
      case Compl(inner) => inner
      case _ => Compl(r)

    /** Kleene star: `this*`. */
    def star: Regex = r match
      case Eps | Empty => Eps
      case s: Star => s
      case _ => Star(r)

    /** Quantifier `this{n,m}` where m can be `Int.MaxValue` for unbounded. */
    def repeat(lo: Int, hi: Int): Regex =
      require(lo >= 0 && hi >= lo, s"invalid bounds {$lo,$hi}")
      val mandatory = (1 to lo).foldLeft[Regex](Regex.Eps)((acc, _) => r.concat(acc))
      if hi == Int.MaxValue then mandatory.concat(star)
      else mandatory.concat((1 to (hi - lo)).foldLeft[Regex](Regex.Eps)((acc, _) => Regex.Eps | (r.concat(acc))))

  /** Alternation of a collection. */
  def alt(parts: Iterable[Regex]): Regex =
    val flat = parts.iterator
      .flatMap:
        case Alt(p) => p
        case Empty => Iterator.empty
        case r => Iterator.single(r)
      .toSet
    if flat.isEmpty then Empty
    else if flat.sizeIs == 1 then flat.head
    else
      val (chars, rest) = flat.partition(_.isInstanceOf[Chars])
      val merged: Set[Regex] =
        if chars.sizeIs <= 1 then flat
        else
          val union = chars.iterator.collect { case Chars(s) => s }.foldLeft(CharSet.empty)(_ union _)
          if union.isEmpty then rest else rest + Chars(union)
      if merged.sizeIs == 1 then merged.head else Alt(merged)

  /** Intersection of a collection. */
  def inter(parts: Iterable[Regex]): Regex =
    val seq = parts.iterator
      .flatMap:
        case Inter(p) => p
        case r => Iterator.single(r)
      .toSet
    if seq.isEmpty || seq.contains(Empty) then Empty
    else
      val (chars, rest) = seq.partition(_.isInstanceOf[Chars])
      val merged =
        if chars.sizeIs <= 1 then Some(seq)
        else
          val isect = chars.iterator.collect { case Chars(s) => s }.reduce(_ intersect _)
          Option.unless(isect.isEmpty)(rest + Chars(isect))
      merged match
        case None => Empty
        case Some(m) if m.sizeIs == 1 => m.head
        case Some(m) => Inter(m)

  private def concatImpl(a: Regex, b: Regex): TailRec[Regex] = (a, b) match
    case (Empty, _) | (_, Empty) => done(Empty)
    case (Eps, x) => done(x)
    case (x, Eps) => done(x)
    case (Concat(x, y), z) => tailcall(concatImpl(y, z)).map(Concat(x, _))
    case _ => done(Concat(a, b))

  /** All-strings regex `Σ*`. */
  val all: Regex = Regex(CharSet.all).star

  /** Convenience: literal char. */
  def lit(c: Char): Regex = Regex(CharSet.single(c))

  /** Convenience: char range. */
  def range(lo: Char, hi: Char): Regex = Regex(CharSet.range(lo, hi))

  /** Convenience: literal string. */
  def literal(s: String): Regex =
    if s.isEmpty then Eps
    else s.foldRight(Eps: Regex)((c, acc) => lit(c).concat(acc))

  // $COVERAGE-OFF$
  given ToExpr[Regex]:
    def apply(r: Regex)(using Quotes): Expr[Regex] = r match
      case Eps => '{ Regex.Eps }
      case Empty => '{ Regex.Empty }
      case Chars(set) => '{ Regex(${ Expr(set) }) }
      case Concat(a, b) => '{ ${ Expr(a) }.concat(${ Expr(b) }) }
      case Alt(parts) =>
        val partsExpr = Expr.ofSeq(parts.toSeq.map(Expr(_)))
        '{ Regex.alt($partsExpr) }
      case Inter(parts) =>
        val partsExpr = Expr.ofSeq(parts.toSeq.map(Expr(_)))
        '{ Regex.inter($partsExpr) }
      case Star(inner) => '{ ${ Expr(inner) }.star }
      case Compl(inner) => '{ ! ${ Expr(inner) } }
  // $COVERAGE-ON$

/** Sorted, non-overlapping, merged ranges over Int code points. */
private[regex] final case class CharSet(ranges: Vector[Range]):

  def contains(c: Int): Boolean = ranges.exists(r => c >= r.lo && c <= r.hi)

  def isEmpty: Boolean = ranges.isEmpty

  infix def union(other: CharSet): CharSet = CharSet.normalize(ranges ++ other.ranges)

  infix def intersect(other: CharSet): CharSet =
    @tailrec
    def loop(i: Int, j: Int, acc: Vector[Range]): Vector[Range] =
      if i >= ranges.length || j >= other.ranges.length then acc
      else
        val a = ranges(i)
        val b = other.ranges(j)
        val lo = math.max(a.lo, b.lo)
        val hi = math.min(a.hi, b.hi)
        val acc1 = if lo <= hi then acc :+ Range(lo, hi) else acc
        if a.hi < b.hi then loop(i + 1, j, acc1) else loop(i, j + 1, acc1)
    CharSet(loop(0, 0, Vector.empty))

  def complement: CharSet =
    @tailrec
    def loop(rs: Vector[Range], prev: Int, acc: Vector[Range]): CharSet =
      if rs.isEmpty then
        if prev <= CharSet.maxCodePoint then CharSet(acc :+ Range(prev, CharSet.maxCodePoint)) else CharSet(acc)
      else
        val head = rs.head
        val acc1 = if prev <= head.lo - 1 then acc :+ Range(prev, head.lo - 1) else acc
        loop(rs.tail, head.hi + 1, acc1)
    loop(ranges, 0, Vector.empty)

private[regex] object CharSet:

  /** Upper bound used for complement. Covers all valid Unicode code points. */
  val maxCodePoint: Int = 0x10ffff

  val empty: CharSet = CharSet(Vector.empty)
  val all: CharSet = CharSet(Vector(Range(0, maxCodePoint)))

  /** What `.` matches in Java regex without DOTALL flag — all code points except line terminators. */
  val dotDefault: CharSet = normalize(
    Range(0, '\n'.toInt - 1),
    Range('\n'.toInt + 1, '\r'.toInt - 1),
    Range('\r'.toInt + 1, 0x84),
    Range(0x86, 0x2027),
    Range(0x202a, maxCodePoint),
  )

  def single(c: Char): CharSet = single(c.toInt)
  def single(c: Int): CharSet = CharSet(Vector(Range(c, c)))
  def range(lo: Char, hi: Char): CharSet = range(lo.toInt, hi.toInt)
  def range(lo: Int, hi: Int): CharSet = CharSet(Vector(Range(lo, hi)))

  /** Builds a normalized [[CharSet]] from arbitrary (possibly overlapping) ranges. */
  def normalize(rs: Iterable[Range]): CharSet =
    val (acc, last) = rs.toVector
      .sortBy(_.lo)
      .foldLeft((Vector.empty[Range], Option.empty[Range])):
        case ((acc, None), r) => (acc, Some(r))
        case ((acc, Some(cur)), r) =>
          if r.lo <= cur.hi + 1 then (acc, Some(Range(cur.lo, math.max(cur.hi, r.hi))))
          else (acc :+ cur, Some(r))
    CharSet(last match
      case None => acc
      case Some(r) => acc :+ r)

  def normalize(ranges: Range*): CharSet = normalize(ranges)

  // $COVERAGE-OFF$
  given ToExpr[CharSet]:
    def apply(s: CharSet)(using Quotes): Expr[CharSet] =
      val rangeExprs = s.ranges.map(r => '{ Range(${ Expr(r.lo) }, ${ Expr(r.hi) }) })
      '{ CharSet(Vector(${ Varargs(rangeExprs) }*)) }
  // $COVERAGE-ON$

/** Single closed code-point range. */
private[regex] type Range = (lo: Int, hi: Int)
private[regex] object Range:
  def apply(lo: Int, hi: Int): Range =
    require(0 <= lo && lo <= hi && hi <= CharSet.maxCodePoint, s"invalid code-point range [$lo, $hi]")
    (lo, hi)
  def apply(lo: Char, hi: Char): Range = Range(lo.toInt, hi.toInt)
