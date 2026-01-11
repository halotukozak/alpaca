package alpaca
package internal.lexer

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.util.regex.Pattern

final class ErrorHandlingStrategyTest extends AnyFunSuite with Matchers:

  test("Strategy.Throw should throw the provided exception") {
    class MyException extends RuntimeException("test")
    given ErrorHandling[LexerCtx.Default] = _ => ErrorHandling.Strategy.Throw(new MyException)

    val L = lexer:
      case "a" => Token["A"]

    intercept[MyException]:
      L.tokenize("b")
  }

  test("Strategy.Stop should stop tokenization and return collected lexemes") {
    given ErrorHandling[LexerCtx.Default] = _ => ErrorHandling.Strategy.Stop

    val L = lexer:
      case "a" => Token["A"]

    val (ctx, res) = L.tokenize("aaabaa")
    res.map(_.name) shouldBe List("A", "A", "A")
    ctx.text.toString shouldBe "" // Stop currently sets text to empty string
  }

  test("Strategy.IgnoreChar should skip one character and continue") {
    var ignoredChars = 0
    given ErrorHandling[LexerCtx.Default] = _ =>
      ignoredChars += 1
      ErrorHandling.Strategy.IgnoreChar

    val L = lexer:
      case "a" => Token["A"]

    val (ctx, res) = L.tokenize("aabaa")
    res.map(_.name) shouldBe List("A", "A", "A", "A")
    ignoredChars shouldBe 1
  }

  test("Strategy.IgnoreToken should skip until next match and continue") {
    var ignoredTokens = 0
    given ErrorHandling[LexerCtx.Default] = _ =>
      ignoredTokens += 1
      ErrorHandling.Strategy.IgnoreToken

    val L = lexer:
      case "a" => Token["A"]

    val (ctx, res) = L.tokenize("aa...aa..a")
    res.map(_.name) shouldBe List("A", "A", "A", "A", "A")
    ignoredTokens shouldBe 2
  }

  test("Strategy.IgnoreToken should skip only char if no token matched") {
    var ignoredTokens = 0
    given ErrorHandling[LexerCtx.Default] = _ =>
      ignoredTokens += 1
      ErrorHandling.Strategy.IgnoreToken

    val L = lexer:
      case "a" => Token["A"]

    val (ctx, res) = L.tokenize("aab")
    res.map(_.name) shouldBe List("A", "A")
    ignoredTokens shouldBe 1
  }

  test("Strategy.IgnoreChar should update position correctly") {
    given ErrorHandling[LexerCtx.Default] = _ => ErrorHandling.Strategy.IgnoreChar

    val L = lexer:
      case "a" => Token["A"]

    val (ctx, res) = L.tokenize("a!a")
    res.map(_.name) shouldBe List("A", "A")
    // Default context has position tracking
    ctx.position shouldBe 4 // 'a' (1) + '!' (2) + 'a' (3) -> next is 4
  }
