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
 * Supported subset (see [[Regex]] doc): literals, escapes (\d \D \s \S \w \W \t \n \r \f
 * \a \e \v \cX \0[n[n]] \xhh \x{h...h} \uhhhh \Q...\E \R and meta-escapes \. \* \+ \? \(
 * \) \[ \] \{ \} \| \^ \$ \-), `.`, char classes `[...]` `[^...]` with ranges (`\b` inside
 * a class means backspace, matching Java), alternation `|`, groups `(...)` (capturing or
 * `(?:...)`, flag groups are NOT supported), quantifiers `*` `+` `?` `{n}` `{n,}` `{n,m}`
 * (bounds capped at [[Regex.maxRepeatBound]]).
 *
 * Unsupported: anchors `^` `$` `\b` `\B` `\A` `\Z` `\z` `\G`, lookaround, backreferences
 * `\1`..`\9` `\k<name>` `\g{...}`, Unicode properties `\p{...}`, grapheme clusters `\X`,
 * named groups, flag groups. Any other undefined letter escape (e.g. `\m`, `\y`, `\q`) is
 * rejected as invalid syntax, matching `java.util.regex.Pattern`'s own behavior.
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

  /** What `\R` matches as a single code point, i.e. everything but the two-char `\r\n` case. */
  private val linebreakSet: CharSet = CharSet.normalize(
    Range('\n', '\n'),
    Range(0x0b, 0x0b),
    Range('\f', '\f'),
    Range('\r', '\r'),
    Range(0x85, 0x85),
    Range(0x2028, 0x2029),
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

    private def fail(msg: String): Nothing =
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
      if lo > Regex.maxRepeatBound || (hi != Int.MaxValue && hi > Regex.maxRepeatBound) then
        fail(s"quantifier bound exceeds maximum supported value of ${Regex.maxRepeatBound} (got {$lo,$hi})")
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
          Regex(CharSet.dotDefault)
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
      Regex(finalSet)

    private def readClassChar(): Int =
      if eof then fail("unterminated character class")
      cur match
        case '\\' =>
          pos += 1
          readEscapedChar(inClass = true) match
            case Left(_) => fail("shorthand escapes (\\d, \\s, \\w, ...) are not supported inside character classes")
            case Right(c) => c
        case _ => consume().toInt

    private def parseEscape(): Regex =
      expect('\\')
      if eof then fail("dangling backslash")
      cur match
        case 'Q' =>
          pos += 1
          parseQuoted()
        case 'R' =>
          pos += 1
          Regex.literal("\r\n") | Regex(linebreakSet)
        case _ =>
          readEscapedChar(inClass = false) match
            case Left(set) => Regex(set)
            case Right(c) => Regex(CharSet.single(c))

    /** Consumes literal text up to (and including) `\E`, or to the end of the pattern if absent. */
    private def parseQuoted(): Regex =
      val end = src.indexOf("\\E", pos)
      val text = if end < 0 then src.substring(pos) else src.substring(pos, end)
      pos = if end < 0 then src.length else end + 2
      Regex.literal(text)

    /**
     * Reads char following `\\`. Either expands to a [[CharSet]] (shorthand) or yields a
     * single code point. `inClass` mirrors Java's rule that `\b` means backspace inside a
     * character class, rather than a word boundary.
     */
    private def readEscapedChar(inClass: Boolean): Either[CharSet, Int] =
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
        case 'a' => Right(0x07)
        case 'e' => Right(0x1b)
        case 'v' => Right(0x0b)
        case 'b' if inClass => Right(0x08)
        case 'b' | 'B' => unsupported(s"word boundary `\\$c`")
        case 'A' | 'Z' | 'z' | 'G' => unsupported(s"anchor `\\$c`")
        case 'p' | 'P' => unsupported("Unicode property")
        case 'k' => unsupported("named backreference `\\k`")
        case 'g' => unsupported("backreference `\\g`")
        case 'X' => unsupported("grapheme cluster `\\X`")
        case 'c' => Right(readControlEscape())
        case 'x' => Right(readHexEscape())
        case 'u' => Right(readUnicodeEscape())
        case '0' => Right(readOctalEscape())
        case d if d.isDigit => unsupported(s"backreference `\\$d`")
        case l if l.isLetter => fail(s"illegal/unsupported escape sequence `\\$l` at position $pos")
        case _ => Right(c.toInt)

    /** `\cx`: control character `x XOR 0x40`. */
    private def readControlEscape(): Int =
      if eof then fail(s"incomplete `\\c` escape at position $pos")
      consume().toInt ^ 0x40

    /** `\xhh` (exactly 2 hex digits) or `\x{h...h}` (1+ hex digits, a valid code point). */
    private def readHexEscape(): Int =
      if !eof && cur == '{' then
        pos += 1
        val start = pos
        while !eof && cur != '}' do pos += 1
        if eof then fail("unterminated `\\x{...}` escape")
        val text = src.substring(start, pos)
        pos += 1
        if text.isEmpty then fail("empty `\\x{}` escape")
        val cp =
          try Integer.parseInt(text, 16)
          catch case _: NumberFormatException => fail(s"invalid hexadecimal escape sequence `\\x{$text}`")
        if cp < 0 || cp > CharSet.maxCodePoint then fail(s"invalid code point `\\x{$text}`")
        cp
      else
        if pos + 2 > src.length then fail("incomplete `\\x` escape")
        val text = src.substring(pos, pos + 2)
        try
          val v = Integer.parseInt(text, 16)
          pos += 2
          v
        catch case _: NumberFormatException => fail(s"invalid hexadecimal escape sequence `\\x$text`")

    /** `\uhhhh`: exactly 4 hex digits. */
    private def readUnicodeEscape(): Int =
      if pos + 4 > src.length then fail("incomplete `\\u` escape")
      val text = src.substring(pos, pos + 4)
      try
        val v = Integer.parseInt(text, 16)
        pos += 4
        v
      catch case _: NumberFormatException => fail(s"invalid unicode escape sequence `\\u$text`")

    /** `\0n`, `\0nn` or `\0mnn` octal escape, mirroring `java.util.regex.Pattern`'s own grammar. */
    private def readOctalEscape(): Int =
      def isOctal(ch: Char): Boolean = ch >= '0' && ch <= '7'
      if eof || !isOctal(cur) then fail("illegal octal escape sequence")
      val n = consume() - '0'
      if !eof && isOctal(cur) then
        val m = consume() - '0'
        if !eof && isOctal(cur) && (n << 6) + (m << 3) + (cur - '0') <= 0xff then
          val o = consume() - '0'
          n * 64 + m * 8 + o
        else n * 8 + m
      else n
