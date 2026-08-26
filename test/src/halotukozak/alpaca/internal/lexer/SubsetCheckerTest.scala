package halotukozak
package alpaca.internal.lexer

import halotukozak.alpaca.internal.lexer.{ShadowException, SubsetChecker}
import halotukozak.regex.Subset
import org.scalatest.LoneElement
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

final class SubsetCheckerTest extends AnyFunSuite with Matchers with LoneElement:

  private def check(patterns: String*) =
    SubsetChecker.checkRegexes(
      patterns.iterator
        .map: p =>
          Subset.parse(p) match
            case Right(subset) => (name = p, subset = subset.withAnySuffix)
            case Left(err) => fail(s"expected successful parse of /$p/, got $err")
        .toList,
    )

  test("checkRegexes should pass for non-overlapping patterns") {
    noException shouldBe thrownBy:
      check(
        "[a-zA-Z_][a-zA-Z0-9_]*",
        "[+-]?[0-9]+",
        "=",
        "[ \\t\\n]+",
      )
  }

  test("checkRegexes should throw ShadowException for overlapping patterns") {
    intercept[ShadowException]:
      check(
        "[a-zA-Z_][a-zA-Z0-9_]*",
        "\\*",
        "=",
        "[a-zA-Z]+",
        "[ \\t\\n]+",
      )
    .getMessage should include("Pattern [a-zA-Z]+ is shadowed by [a-zA-Z_][a-zA-Z0-9_]*")
  }

  test("checkRegexes should report prefix shadowing") {
    intercept[ShadowException]:
      check(
        "i",
        "\\*",
        "if",
        "=",
        "[a-zA-Z]+",
        "[ \\t\\n]+",
      )
    .getMessage should include("Pattern if is shadowed by i")
  }

  test("checkRegexes should report identical patterns as overlapping") {
    intercept[ShadowException]:
      check(
        "[a-zA-Z_][a-zA-Z0-9_]*",
        "\\*",
        "=",
        "[a-zA-Z_][a-zA-Z0-9_]*",
        "[ \\t\\n]+",
      )
    .getMessage should include("Pattern [a-zA-Z_][a-zA-Z0-9_]* is shadowed by [a-zA-Z_][a-zA-Z0-9_]*")
  }

  test("checkRegexes should not report patterns in proper order") {
    noException shouldBe thrownBy:
      check(
        "if",
        "\\*",
        "when",
        "i",
        "=",
        "[a-zA-Z_][a-zA-Z0-9_]*",
        "[ \\t\\n]+",
      )
  }

  test("checkRegexes should handle empty pattern list") {
    noException shouldBe thrownBy:
      SubsetChecker.checkRegexes(Nil)
  }

  test("checkRegexes should handle single pattern") {
    noException shouldBe thrownBy:
      check("[a-zA-Z_][a-zA-Z0-9_]*")
  }

  test("handles CalcLexer-like patterns") {
    noException shouldBe thrownBy:
      check(
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
      )
  }
