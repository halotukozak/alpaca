package alpaca
package internal

import alpaca.{lexer, Token, LexerCtx}
import alpaca.internal.lexer.{Lexem, PositionTracking, LineTracking}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

final class LexerErrorRecoveryTest extends AnyFunSuite with Matchers {

  test("lexer without error recovery throws exception for unexpected character") {
    val Lexer = lexer { case number @ "[0-9]+" => Token["NUMBER"](number.toInt) }

    val exception = intercept[RuntimeException] {
      Lexer.tokenize("123abc")
    }
    assert(exception.getMessage.contains("Unexpected character: 'a'"))
  }

  test("lexer with error recovery collects errors and continues") {
    // Create a lexer context with error recovery
    final case class RecoveringCtx(
      var text: CharSequence = "",
      var position: Int = 1,
      var line: Int = 1,
    ) extends LexerCtx
        with PositionTracking
        with LineTracking
        with LexerErrorRecovery

    given Empty[RecoveringCtx] = Empty.derived

    val Lexer = lexer[RecoveringCtx] {
      case number @ "[0-9]+" => Token["NUMBER"](number.toInt)
      case "\\s+" => Token.Ignored
    }

    val result = Lexer.tokenize("123 abc 456")

    // Should have tokens for 123 and 456
    result.should(contain(Lexem("NUMBER", 123)))
    result.should(contain(Lexem("NUMBER", 456)))

    // Errors should be collected via context (not accessible from result)
    // The key thing is that it doesn't throw and continues lexing
  }

  test("lexer error recovery skips multiple unexpected characters") {
    final case class RecoveringCtx(
      var text: CharSequence = "",
      var position: Int = 1,
      var line: Int = 1,
    ) extends LexerCtx
        with PositionTracking
        with LineTracking
        with LexerErrorRecovery

    given Empty[RecoveringCtx] = Empty.derived

    val Lexer = lexer[RecoveringCtx] {
      case number @ "[0-9]+" => Token["NUMBER"](number.toInt)
    }

    // "abc" has 3 unexpected characters, but 123 should still be tokenized
    val result = Lexer.tokenize("abc123")

    result.should(have.size(1))
    result.should(contain(Lexem("NUMBER", 123)))
  }

  test("lexer error recovery works with only unexpected characters") {
    final case class RecoveringCtx(
      var text: CharSequence = "",
      var position: Int = 1,
      var line: Int = 1,
    ) extends LexerCtx
        with PositionTracking
        with LineTracking
        with LexerErrorRecovery

    given Empty[RecoveringCtx] = Empty.derived

    val Lexer = lexer[RecoveringCtx] {
      case number @ "[0-9]+" => Token["NUMBER"](number.toInt)
    }

    // No numbers at all - all unexpected characters
    val result = Lexer.tokenize("abc")

    result.shouldBe(empty)
  }
}
