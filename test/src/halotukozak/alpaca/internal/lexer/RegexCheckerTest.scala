package halotukozak
package alpaca.internal.lexer

import halotukozak.alpaca.internal.lexer.{RegexChecker, ShadowException}
import halotukozak.regex.{Regex, RegexParser}
import org.scalatest.LoneElement
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

final class RegexCheckerTest extends AnyFunSuite with Matchers with LoneElement:

  private def items(patterns: String*): List[(name: String, regex: Regex)] =
    patterns.toList.map: p =>
      RegexParser.parse(p) match
        case Right(r) => (name = p, regex = r)
        case Left(err) => fail(s"expected successful parse of /$p/, got $err")

  test("checkRegexes should pass for non-overlapping patterns") {
    noException shouldBe thrownBy:
      RegexChecker.checkRegexes(
        items(
          "[a-zA-Z_][a-zA-Z0-9_]*",
          "[+-]?[0-9]+",
          "=",
          "[ \\t\\n]+",
        ),
      )
  }

  test("checkRegexes should throw ShadowException for overlapping patterns") {
    intercept[ShadowException]:
      RegexChecker.checkRegexes(
        items(
          "[a-zA-Z_][a-zA-Z0-9_]*",
          "\\*",
          "=",
          "[a-zA-Z]+",
          "[ \\t\\n]+",
        ),
      )
    .getMessage should include("Pattern [a-zA-Z]+ is shadowed by [a-zA-Z_][a-zA-Z0-9_]*")
  }

  test("checkRegexes should report prefix shadowing") {
    intercept[ShadowException]:
      RegexChecker.checkRegexes(
        items(
          "i",
          "\\*",
          "if",
          "=",
          "[a-zA-Z]+",
          "[ \\t\\n]+",
        ),
      )
    .getMessage should include("Pattern if is shadowed by i")
  }

  test("checkRegexes should report identical patterns as overlapping") {
    intercept[ShadowException]:
      RegexChecker.checkRegexes(
        items(
          "[a-zA-Z_][a-zA-Z0-9_]*",
          "\\*",
          "=",
          "[a-zA-Z_][a-zA-Z0-9_]*",
          "[ \\t\\n]+",
        ),
      )
    .getMessage should include("Pattern [a-zA-Z_][a-zA-Z0-9_]* is shadowed by [a-zA-Z_][a-zA-Z0-9_]*")
  }

  test("checkRegexes should not report patterns in proper order") {
    noException shouldBe thrownBy:
      RegexChecker.checkRegexes(
        items(
          "if",
          "\\*",
          "when",
          "i",
          "=",
          "[a-zA-Z_][a-zA-Z0-9_]*",
          "[ \\t\\n]+",
        ),
      )
  }

  test("checkRegexes should handle empty pattern list") {
    noException shouldBe thrownBy:
      RegexChecker.checkRegexes(Nil)
  }

  test("checkRegexes should handle single pattern") {
    noException shouldBe thrownBy:
      RegexChecker.checkRegexes(items("[a-zA-Z_][a-zA-Z0-9_]*"))
  }

  test("handles CalcLexer-like patterns") {
    noException shouldBe thrownBy:
      RegexChecker.checkRegexes(
        items(
          " ",
          "\\t",
          "[a-zA-Z_][a-zA-Z0-9_]*",
          "\\+",
          "-",
          "\\*",
          "/",
          "=",
          ",",
          "\\(",
          "\\)",
          "\\d+",
          "#.*",
          "\n+",
        ),
      )
  }
