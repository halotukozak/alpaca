package alpaca
package internal
package lexer
package regex

import alpaca.internal.lexer.regex.Regex.Eps

import scala.annotation.tailrec

/** Reason a [[RegexParser.parse]] call did not produce a [[Regex]]. */
private[lexer] sealed trait RegexParseError:
  def pattern: String
  def position: Int
  def message: String

private[lexer] object RegexParseError:

  /** Pattern is syntactically malformed (unterminated group, dangling backslash, etc.). */
  final case class InvalidSyntax(pattern: String, position: Int, message: String) extends RegexParseError

  /** Pattern uses a recognized but unsupported feature (anchors, lookaround, backreferences, ...). */
  final case class UnsupportedFeature(pattern: String, position: Int, feature: String) extends RegexParseError:
    def message: String = s"unsupported regex feature `$feature`"

/**
 * Java-regex-style parser producing a normalized [[Regex]].
 *
 * Supported subset (see [[Regex]] doc): literals, escapes (\d \D \s \S \w \W \t \n \r \\
 * and meta-escapes \. \* \+ \? \( \) \[ \] \{ \} \| \^ \$ \-), `.`, char classes
 * `[...]` `[^...]` with ranges, alternation `|`, groups `(...)` (capturing or `(?:...)`,
 * flag groups are NOT supported), quantifiers `*` `+` `?` `{n}` `{n,}` `{n,m}`.
 *
 * Unsupported: anchors `^` `$` `\b` `\B`, lookaround, backreferences `\1`..`\9`, Unicode
 * properties `\p{...}`, named groups, flag groups.
 */
private[lexer] object RegexParser:

  private final class InvalidSyntaxSignal(val msg: String, val pos: Int) extends RuntimeException(msg)
  private final class UnsupportedSignal(val feature: String, val pos: Int) extends RuntimeException(feature)

  private val whitespaceSet: CharSet = CharSet.normalize(
    Range(' ', ' '),
    Range('\t', '\t'),
    Range('\n', '\n'),
    Range(0x0b, 0x0b),
    Range('\f', '\f'),
    Range('\r', '\r'),
  )

  private val wordSet: CharSet = CharSet.normalize(
    Range('a', 'z'),
    Range('A', 'Z'),
    Range('0', '9'),
    Range('_', '_'),
  )

  /**
   * Parse `pattern` into a [[Regex]]. Returns [[Left]] with structured error info if the
   * pattern is malformed or uses an unsupported feature.
   */
  def parse(pattern: String): Either[RegexParseError, Regex] =
    try
      val p = new Parser(pattern)
      val r = p.parseAlt()
      if p.pos != pattern.length then
        Left(RegexParseError.InvalidSyntax(pattern, p.pos, s"unexpected trailing input at position ${p.pos}"))
      else Right(r)
    catch
      case e: InvalidSyntaxSignal => Left(RegexParseError.InvalidSyntax(pattern, e.pos, e.msg))
      case e: UnsupportedSignal => Left(RegexParseError.UnsupportedFeature(pattern, e.pos, e.feature))

  private final class Parser(private val src: String):
    var pos: Int = 0

    def fail(msg: String): Nothing =
      throw new InvalidSyntaxSignal(msg, pos)

    private def unsupported(feature: String): Nothing =
      throw new UnsupportedSignal(feature, pos)

    private def eof: Boolean = pos >= src.length

    private def cur: Char = src.charAt(pos)

    private def consume(): Char =
      val c = src.charAt(pos)
      pos += 1
      c

    private def expect(c: Char): Unit =
      if eof || cur != c then fail(s"expected `$c` at position $pos")
      pos += 1

    /** alt = concat ('|' concat)* */
    def parseAlt(): Regex =
      @tailrec def loop(acc: Vector[Regex]): Vector[Regex] =
        if !eof && cur == '|' then
          pos += 1
          loop(acc :+ parseConcat())
        else acc
      val parts = loop(Vector(parseConcat()))
      if parts.sizeIs == 1 then parts.head else Regex.alt(parts)

    /** concat = factor* */
    private def parseConcat(): Regex =
      @tailrec def loop(acc: Vector[Regex]): Vector[Regex] =
        if eof then acc
        else
          cur match
            case ')' | '|' => acc
            case _ => loop(acc :+ parseFactor())
      val parts = loop(Vector.empty)
      if parts.isEmpty then Eps else parts.reduceRight(_ concat _)

    /** factor = atom quantifier? */
    private def parseFactor(): Regex =
      val a = parseAtom()
      if eof then a
      else
        cur match
          case '*' =>
            pos += 1
            a.star
          case '+' =>
            pos += 1
            a.concat(a.star)
          case '?' =>
            pos += 1
            Eps | a
          case '{' =>
            pos += 1
            parseRepeat(a)
          case _ => a

    private def parseRepeat(a: Regex): Regex =
      val lo = readNumber()
      if lo < 0 then fail(s"expected number after `{` at position $pos")
      if eof then fail(s"unterminated `{` at position $pos")
      val hi = cur match
        case ',' =>
          pos += 1
          if !eof && cur == '}' then Int.MaxValue
          else
            val n = readNumber()
            if n < 0 then fail(s"expected number or `}` after `,` at position $pos")
            n
        case '}' => lo
        case _ => fail(s"expected `,` or `}` at position $pos")
      expect('}')
      if hi < lo then fail(s"invalid quantifier bounds {$lo,$hi}: upper bound must be >= lower bound")
      a.repeat(lo, hi)

    private def readNumber(): Int =
      @tailrec def loop(p: Int): Int = if p < src.length && src.charAt(p).isDigit then loop(p + 1) else p

      val end = loop(pos)
      if end == pos then -1
      else
        val text = src.substring(pos, end)
        pos = end
        try text.toInt
        catch case _: NumberFormatException => fail(s"quantifier value `$text` does not fit in an Int")

    /** atom = group | charClass | `.` | escape | char */
    private def parseAtom(): Regex =
      if eof then fail("unexpected end of pattern")
      cur match
        case '(' => parseGroup()
        case '[' => parseCharClass()
        case '.' =>
          pos += 1
          Regex.chars(CharSet.dotDefault)
        case '\\' => parseEscape()
        case '^' | '$' => unsupported(s"anchor `${consume()}`")
        case ')' | '|' | '*' | '+' | '?' | '{' | '}' | ']' =>
          fail(s"unexpected `$cur` at position $pos")
        case c =>
          pos += 1
          Regex.lit(c)

    private def parseGroup(): Regex =
      expect('(')
      if !eof && cur == '?' then
        pos += 1
        consumeGroupHeader()
      val inner = parseAlt()
      expect(')')
      inner

    private def consumeGroupHeader(): Unit =
      if eof then unsupported("incomplete group header")
      cur match
        case ':' => pos += 1
        case '=' | '!' => unsupported("lookahead")
        case '<' =>
          pos += 1
          if !eof && (cur == '=' || cur == '!') then unsupported("lookbehind")
          else unsupported("named group")
        case _ => unsupported("flag group")

    private def parseCharClass(): Regex =
      expect('[')
      val negated = !eof && cur == '^'
      if negated then pos += 1
      @tailrec def loop(acc: Vector[Range]): Vector[Range] =
        if !eof && cur != ']' then
          val lo = readClassChar()
          val hi =
            if !eof && cur == '-' && pos + 1 < src.length && src.charAt(pos + 1) != ']' then
              pos += 1
              readClassChar()
            else lo
          if hi < lo then fail(s"invalid character-class range `${lo.toChar}-${hi.toChar}`: end must be >= start")
          loop(acc :+ Range(lo, hi))
        else acc
      val ranges = loop(Vector.empty)
      expect(']')
      if ranges.isEmpty then fail("empty character class")
      val set = CharSet.normalize(ranges)
      val finalSet = if negated then set.complement else set
      Regex.chars(finalSet)

    private def readClassChar(): Int =
      if eof then fail("unterminated character class")
      cur match
        case '\\' =>
          pos += 1
          readEscapedChar() match
            case Left(_) => fail("shorthand escapes (\\d, \\s, \\w, ...) are not supported inside character classes")
            case Right(c) => c
        case _ => consume().toInt

    private def parseEscape(): Regex =
      expect('\\')
      readEscapedChar() match
        case Left(set) => Regex.chars(set)
        case Right(c) => Regex.chars(CharSet.single(c))

    /** Reads char following `\\`. Either expands to a [[CharSet]] (shorthand) or yields a single code point. */
    private def readEscapedChar(): Either[CharSet, Int] =
      if eof then fail("dangling backslash")
      val c = src.charAt(pos)
      pos += 1
      c match
        case 'd' => Left(CharSet.range('0', '9'))
        case 'D' => Left(CharSet.range('0', '9').complement)
        case 's' => Left(whitespaceSet)
        case 'S' => Left(whitespaceSet.complement)
        case 'w' => Left(wordSet)
        case 'W' => Left(wordSet.complement)
        case 't' => Right('\t'.toInt)
        case 'n' => Right('\n'.toInt)
        case 'r' => Right('\r'.toInt)
        case 'f' => Right('\f'.toInt)
        case '0' => Right(0)
        case 'b' | 'B' => unsupported(s"word boundary `\\$c`")
        case 'A' | 'z' | 'Z' => unsupported(s"anchor `\\$c`")
        case 'p' | 'P' => unsupported("Unicode property")
        case d if d.isDigit => unsupported(s"backreference `\\$d`")
        case _ => Right(c.toInt)
