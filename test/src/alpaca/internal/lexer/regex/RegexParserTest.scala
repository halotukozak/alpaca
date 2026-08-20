package alpaca
package internal
package lexer
package regex

import alpaca.internal.lexer.regex.Regex.*

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

final class RegexParserTest extends AnyFunSuite with Matchers:

  private def parse(pattern: String): Regex = RegexParser.parse(pattern) match
    case Right(r) => r
    case Left(err) => fail(s"expected successful parse of /$pattern/, got $err")

  test("parses literal string") {
    parse("abc") shouldBe Regex.literal("abc")
  }

  test("parses single literal") {
    parse("a") shouldBe Regex.lit('a')
  }

  test("parses dot") {
    parse(".") shouldBe Regex(CharSet.dotDefault)
  }

  test("parses character class with range") {
    parse("[a-z]") shouldBe Regex.range('a', 'z')
  }

  test("parses negated character class") {
    parse("[^a-z]") shouldBe Regex(CharSet.range('a', 'z').complement)
  }

  test("parses character class with multiple ranges") {
    parse("[a-zA-Z0-9_]") shouldBe Regex(
      CharSet.normalize(
        Range('a', 'z'),
        Range('A', 'Z'),
        Range('0', '9'),
        Range('_', '_'),
      ),
    )
  }

  test("parses Kleene star") {
    parse("a*") shouldBe Regex.lit('a').star
  }

  test("parses plus quantifier") {
    val a = Regex.lit('a')
    parse("a+") shouldBe (a.concat(a.star))
  }

  test("parses optional quantifier") {
    parse("a?") shouldBe (Eps | Regex.lit('a'))
  }

  test("parses bounded repetition {2}") {
    val a = Regex.lit('a')
    parse("a{2}") shouldBe (a.concat(a))
  }

  test("parses unbounded repetition {2,}") {
    val a = Regex.lit('a')
    parse("a{2,}") shouldBe (a.concat(a).concat(a.star))
  }

  test("parses alternation") {
    parse("a|b") shouldBe (Regex.lit('a') | Regex.lit('b'))
  }

  test("parses group") {
    parse("(ab)") shouldBe Regex.literal("ab")
  }

  test("parses non-capturing group") {
    parse("(?:ab)") shouldBe Regex.literal("ab")
  }

  test("parses escapes") {
    parse("\\.") shouldBe Regex.lit('.')
    parse("\\*") shouldBe Regex.lit('*')
    parse("\\\\") shouldBe Regex.lit('\\')
    parse("\\t") shouldBe Regex.lit('\t')
    parse("\\n") shouldBe Regex.lit('\n')
  }

  test("parses \\d shorthand") {
    parse("\\d") shouldBe Regex.range('0', '9')
  }

  test("rejects anchors") {
    RegexParser.parse("^a") shouldBe a[Left[RegexParseError.UnsupportedFeature, ?]]
    RegexParser.parse("a$") shouldBe a[Left[RegexParseError.UnsupportedFeature, ?]]
  }

  test("rejects lookahead") {
    RegexParser.parse("(?=a)") shouldBe a[Left[RegexParseError.UnsupportedFeature, ?]]
    RegexParser.parse("(?!a)") shouldBe a[Left[RegexParseError.UnsupportedFeature, ?]]
  }

  test("rejects lookbehind") {
    RegexParser.parse("(?<=a)") shouldBe a[Left[RegexParseError.UnsupportedFeature, ?]]
  }

  test("rejects backreferences") {
    RegexParser.parse("(a)\\1") shouldBe a[Left[RegexParseError.UnsupportedFeature, ?]]
  }

  test("rejects empty character class") {
    RegexParser.parse("[]") shouldBe a[Left[RegexParseError.InvalidSyntax, ?]]
  }

  test("rejects unclosed group") {
    RegexParser.parse("(abc") shouldBe a[Left[RegexParseError.InvalidSyntax, ?]]
  }

  test("parses nested groups") {
    parse("(a(bc))") shouldBe Regex.literal("abc")
  }

  test("parses alternation inside groups") {
    parse("(a|b)") shouldBe (Regex.lit('a') | Regex.lit('b'))
  }

  test("parses quantified group") {
    val ab = Regex.literal("ab")
    parse("(ab)+") shouldBe (ab.concat(ab.star))
  }

  test("parses bounded {n,m} repetition") {
    parse("a{2,3}") shouldBe Regex.lit('a').repeat(2, 3)
  }

  test("parses character class with single char") {
    parse("[a]") shouldBe Regex.lit('a')
  }

  test("parses character class with mixed escapes") {
    parse("[\\t\\n ]") shouldBe Regex(
      CharSet.normalize(
        Range('\t', '\t'),
        Range('\n', '\n'),
        Range(' ', ' '),
      ),
    )
  }

  test("parses \\s and \\w shorthands") {
    parse("\\s") shouldBe Regex(
      CharSet.normalize(
        Range(' ', ' '),
        Range('\t', '\t'),
        Range('\n', '\n'),
        Range(0x0b, 0x0b),
        Range('\f', '\f'),
        Range('\r', '\r'),
      ),
    )
    parse("\\w") shouldBe Regex(
      CharSet.normalize(
        Range('a', 'z'),
        Range('A', 'Z'),
        Range('0', '9'),
        Range('_', '_'),
      ),
    )
  }

  test("rejects unclosed char class") {
    RegexParser.parse("[abc") shouldBe a[Left[RegexParseError.InvalidSyntax, ?]]
  }

  test("rejects unclosed quantifier") {
    RegexParser.parse("a{2") shouldBe a[Left[RegexParseError.InvalidSyntax, ?]]
  }

  test("rejects dangling backslash") {
    RegexParser.parse("a\\") shouldBe a[Left[RegexParseError.InvalidSyntax, ?]]
  }

  test("rejects trailing input after pattern") {
    RegexParser.parse("*") shouldBe a[Left[RegexParseError.InvalidSyntax, ?]]
  }

  test("parses empty pattern as Eps") {
    parse("") shouldBe Eps
  }

  test("parses \\xhh hex escape") {
    parse("\\x41") shouldBe Regex.lit('A')
  }

  test("parses \\x{h...h} hex escape") {
    parse("\\x{41}") shouldBe Regex.lit('A')
    parse("\\x{1F600}") shouldBe Regex(CharSet.single(0x1f600))
  }

  test("rejects \\x{} escape above the max code point") {
    RegexParser.parse("\\x{110000}") shouldBe a[Left[RegexParseError.InvalidSyntax, ?]]
  }

  test("rejects incomplete \\x escape") {
    RegexParser.parse("\\x4") shouldBe a[Left[RegexParseError.InvalidSyntax, ?]]
    RegexParser.parse("\\x4g") shouldBe a[Left[RegexParseError.InvalidSyntax, ?]]
  }

  test("parses \\uhhhh unicode escape") {
    parse("\\u0041") shouldBe Regex.lit('A')
  }

  test("parses \\0 octal escape") {
    parse("\\01") shouldBe Regex.lit(0x01.toChar)
    parse("\\012") shouldBe Regex.lit(0x0a.toChar)
    parse("\\0101") shouldBe Regex.lit('A')
  }

  test("rejects \\0 with no following octal digit") {
    RegexParser.parse("\\0") shouldBe a[Left[RegexParseError.InvalidSyntax, ?]]
  }

  test("\\0 octal escape stops at 2 digits when a 3rd would overflow 0377") {
    // digits after the leading `0` are "777": 0o77 (63) would overflow to 0o777 (511 > 0o377),
    // so only the first two digits are consumed and the trailing `7` is a separate literal
    parse("\\0777") shouldBe Regex.lit(0x3f.toChar).concat(Regex.lit('7'))
  }

  test("parses \\cX control escape") {
    parse("\\cA") shouldBe Regex.lit(0x01.toChar)
  }

  test("rejects incomplete \\c escape") {
    RegexParser.parse("\\c") shouldBe a[Left[RegexParseError.InvalidSyntax, ?]]
  }

  test("parses \\Q...\\E quoted literal") {
    parse("\\Qa.b*\\E") shouldBe Regex.literal("a.b*")
  }

  test("backslash has no special meaning inside \\Q...\\E") {
    parse("\\Qa\\b\\E") shouldBe Regex.literal("a\\b")
  }

  test("parses unterminated \\Q as literal to end of pattern") {
    parse("\\Qa.b") shouldBe Regex.literal("a.b")
  }

  test("parses hex/unicode/octal/control escapes inside a character class") {
    parse("[\\x41\\u0042\\0103\\cA]") shouldBe Regex(
      CharSet.normalize(
        Range('A', 'A'),
        Range('B', 'B'),
        Range('C', 'C'),
        Range(0x01, 0x01),
      ),
    )
  }

  test("parses \\R linebreak matcher") {
    val linebreak = Regex(
      CharSet.normalize(
        Range('\n', '\n'),
        Range(0x0b, 0x0b),
        Range('\f', '\f'),
        Range('\r', '\r'),
        Range(0x85, 0x85),
        Range(0x2028, 0x2029),
      ),
    )
    parse("\\R") shouldBe (Regex.literal("\r\n") | linebreak)
  }

  test("\\b inside a character class means backspace") {
    parse("[\\b]") shouldBe Regex.lit(0x08.toChar)
  }

  test("\\b outside a character class is an unsupported word boundary") {
    RegexParser.parse("\\b") shouldBe a[Left[RegexParseError.UnsupportedFeature, ?]]
  }

  test("rejects undefined letter escapes") {
    RegexParser.parse("\\m") shouldBe a[Left[RegexParseError.InvalidSyntax, ?]]
    RegexParser.parse("\\y") shouldBe a[Left[RegexParseError.InvalidSyntax, ?]]
  }

  test("rejects unsupported but recognized Java escapes") {
    RegexParser.parse("\\G") shouldBe a[Left[RegexParseError.UnsupportedFeature, ?]]
    RegexParser.parse("\\k") shouldBe a[Left[RegexParseError.UnsupportedFeature, ?]]
    RegexParser.parse("\\X") shouldBe a[Left[RegexParseError.UnsupportedFeature, ?]]
  }

  test("accepts quantifier bounds up to the cap") {
    RegexParser.parse(s"a{${Regex.maxRepeatBound}}") shouldBe a[Right[?, Regex]]
  }

  test("rejects quantifier bounds above the cap") {
    RegexParser.parse(s"a{${Regex.maxRepeatBound + 1}}") shouldBe a[Left[RegexParseError.InvalidSyntax, ?]]
    RegexParser.parse(s"a{0,${Regex.maxRepeatBound + 1}}") shouldBe a[Left[RegexParseError.InvalidSyntax, ?]]
  }

  test("parses complex realistic pattern") {
    val idStart = Regex(
      CharSet.normalize(
        Range('a', 'z'),
        Range('A', 'Z'),
        Range('_', '_'),
      ),
    )
    val idRest = Regex(
      CharSet.normalize(
        Range('a', 'z'),
        Range('A', 'Z'),
        Range('0', '9'),
        Range('_', '_'),
      ),
    )
    parse("[a-zA-Z_][a-zA-Z0-9_]*") shouldBe (idStart.concat(idRest.star))
  }
