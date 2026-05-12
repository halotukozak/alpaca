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
