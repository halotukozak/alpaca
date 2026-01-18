package alpaca
package internal
package lexer

import org.scalatest.LoneElement
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

final class RegexCheckerTest extends AnyFunSuite with Matchers with LoneElement:
  given Log = Log.materialize

  test("checkPatterns should return None for non-overlapping patterns") {
    val patterns = List(
      "[a-zA-Z_][a-zA-Z0-9_]*",
      "[+-]?[0-9]+",
      "=",
      "[ \\t\\n]+",
    )
    noException shouldBe thrownBy:
      RegexChecker.checkPatterns(patterns)
  }

  test("checkPatterns should return Iterable[String] for overlapping patterns") {
    val patterns = List(
      "[a-zA-Z_][a-zA-Z0-9_]*",
      "\\*",
      "=",
      "[a-zA-Z]+",
      "[ \\t\\n]+",
    )
    intercept[ShadowException]:
      RegexChecker.checkPatterns(patterns)
    .getMessage should include("Pattern [a-zA-Z]+ is shadowed by [a-zA-Z_][a-zA-Z0-9_]*")
  }

  test("checkPatterns should report prefix shadowing") {
    val patterns = List(
      "i",
      "\\*",
      "if",
      "=",
      "[a-zA-Z]+",
      "[ \\t\\n]+",
    )
    intercept[ShadowException]:
      RegexChecker.checkPatterns(patterns)
    .getMessage should include("Pattern if is shadowed by i")
  }

  test("checkPatterns should report identical patterns as overlapping") {
    val patterns = List(
      "[a-zA-Z_][a-zA-Z0-9_]*",
      "\\*",
      "=",
      "[a-zA-Z_][a-zA-Z0-9_]*",
      "[ \\t\\n]+",
    )
    intercept[ShadowException]:
      RegexChecker.checkPatterns(patterns)
    .getMessage should include("Pattern [a-zA-Z_][a-zA-Z0-9_]* is shadowed by [a-zA-Z_][a-zA-Z0-9_]*")
  }

  test("checkPatterns should not report patterns in proper order") {
    val patterns = List(
      "if",
      "\\*",
      "when",
      "i",
      "=",
      "[a-zA-Z_][a-zA-Z0-9_]*",
      "[ \\t\\n]+",
    )
    noException shouldBe thrownBy:
      RegexChecker.checkPatterns(patterns)
  }

  test("checkPatterns should handle empty pattern list") {
    noException shouldBe thrownBy:
      RegexChecker.checkPatterns(Nil)
  }

  test("checkPatterns should handle single pattern") {
    noException shouldBe thrownBy:
      RegexChecker.checkPatterns(List("[a-zA-Z_][a-zA-Z0-9_]*"))
  }
