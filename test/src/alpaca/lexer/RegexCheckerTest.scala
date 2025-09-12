package alpaca.lexer

import org.scalatest.funsuite.AnyFunSuite

class RegexCheckerTest extends AnyFunSuite {
  test("checkPatterns should return None for non-overlapping patterns") {
    val patterns = List(
      "[a-zA-Z_][a-zA-Z0-9_]*", // identifier
      "[+-]?[0-9]+",            // integer
      "=",                      // equals sign
      "[ \\t\\n]+",             // whitespace
    )
    assert(RegexChecker.checkPatterns(patterns).isEmpty)
  }

  test("checkPatterns should return Some(i, j) for overlapping patterns") {
    val patterns = List(
      "[a-zA-Z_][a-zA-Z0-9_]*", // identifier
      "\\*",                    // asterisk
      "=",                      // equals sign
      "[a-zA-Z]+",              // alphabetic strings (overlaps with identifier)
      "[ \\t\\n]+",             // whitespace
    )
    assert(RegexChecker.checkPatterns(patterns).contains((0, 3)))
  }

  test("chackPatterns should report identical patterns as overlapping") {
    val patterns = List(
      "[a-zA-Z_][a-zA-Z0-9_]*", // identifier
      "\\*",                    // asterisk
      "=",                      // equals sign
      "[a-zA-Z_][a-zA-Z0-9_]*", // identical to identifier
      "[ \\t\\n]+",             // whitespace
    )
    assert(RegexChecker.checkPatterns(patterns).contains((0, 3)))
  }

  test("checkPatterns should not report patterns in proper order") {
    val patterns = List(
      "if",                     // keyword
      "\\*",                    // asterisk
      "when",                   // another keyword
      "=",                      // equals sign
      "[a-zA-Z_][a-zA-Z0-9_]*", // identifier
      "[ \\t\\n]+",             // whitespace
    )
    assert(RegexChecker.checkPatterns(patterns).isEmpty)
  }

  test("checkPatterns should handle empty pattern list") {
    assert(RegexChecker.checkPatterns(Nil).isEmpty)
  }

  test("checkPatterns should handle single pattern") {
    assert(RegexChecker.checkPatterns(List("[a-zA-Z_][a-zA-Z0-9_]*")).isEmpty)
  }
}