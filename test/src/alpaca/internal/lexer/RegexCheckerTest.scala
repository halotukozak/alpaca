package alpaca.internal.lexer

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.LoneElement

final class RegexCheckerTest extends AnyFunSuite with Matchers with LoneElement {
  test("checkPatterns should return None for non-overlapping patterns") {
    val patterns = List(
      "[a-zA-Z_][a-zA-Z0-9_]*",
      "[+-]?[0-9]+",
      "=",
      "[ \\t\\n]+",
    )
    RegexChecker.checkPatterns(patterns) shouldBe empty
  }

  test("checkPatterns should return Iterable[String] for overlapping patterns") {
    val patterns = List(
      "[a-zA-Z_][a-zA-Z0-9_]*",
      "\\*",
      "=",
      "[a-zA-Z]+",
      "[ \\t\\n]+",
    )
    RegexChecker.checkPatterns(patterns).loneElement shouldBe (3, 0)
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
    RegexChecker.checkPatterns(patterns).loneElement shouldBe (2, 0)
  }

  test("checkPatterns should report identical patterns as overlapping") {
    val patterns = List(
      "[a-zA-Z_][a-zA-Z0-9_]*",
      "\\*",
      "=",
      "[a-zA-Z_][a-zA-Z0-9_]*",
      "[ \\t\\n]+",
    )
    RegexChecker.checkPatterns(patterns).loneElement shouldBe (3, 0)
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
    RegexChecker.checkPatterns(patterns) shouldBe empty
  }

  test("checkPatterns should handle empty pattern list") {
    RegexChecker.checkPatterns(Nil) shouldBe empty
  }

  test("checkPatterns should handle single pattern") {
    RegexChecker.checkPatterns(List("[a-zA-Z_][a-zA-Z0-9_]*")) shouldBe empty
  }
}
