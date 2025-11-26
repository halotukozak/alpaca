package alpaca
package internal.lexer

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

final class LexerTest extends AnyFunSuite with Matchers {

  test("tokenize simple identifier") {
    val Lexer = lexer { case id @ "[a-zA-Z][a-zA-Z0-9]*" => Token["IDENTIFIER"](id) }
    val result = Lexer.tokenize("hello")
    assert(result == List(Lexeme("IDENTIFIER", "hello", Map.empty)))
  }

  test("tokenize with whitespace ignored") {
    val Lexer = lexer {
      case number @ "[0-9]+" => Token["NUMBER"](number)
      case "\\+" => Token["PLUS"]
      case "\\s+" => Token.Ignored
    }
    val result = Lexer.tokenize("42 + 13")

    assert(
      result == List(
        Lexeme("NUMBER", "42", Map.empty),
        Lexeme("PLUS", (), Map.empty),
        Lexeme("NUMBER", "13", Map.empty),
      ),
    )
  }

  test("tokenize empty string") {
    val Lexer = lexer { case id @ "[a-zA-Z][a-zA-Z0-9]*" => Token["IDENTIFIER"](id) }
    val result = Lexer.tokenize("")
    assert(result == List.empty)
  }

  test("throw exception for unexpected character") {
    val Lexer = lexer { case number @ "[0-9]+" => Token["NUMBER"](number.toInt) }

    val exception = intercept[RuntimeException] {
      Lexer.tokenize("123abc")
    }
    assert(exception.getMessage.contains("Unexpected character: 'a'"))
  }

  test("tokenize complex expression") {
    val Lexer = lexer {
      case number @ "[0-9]+" => Token["NUMBER"](number)
      case id @ "[a-zA-Z][a-zA-Z0-9]*" => Token["IDENTIFIER"](id)
      case "\\+" => Token["PLUS"]
      case "-" => Token["MINUS"]
      case "\\*" => Token["MULTIPLY"]
      case "\\(" => Token["LPAREN"]
      case "\\)" => Token["RPAREN"]
      case "\\s+" => Token.Ignored
    }
    val result = Lexer.tokenize("(x + 42) * y - 1")

    assert(
      result == List(
        Lexeme("LPAREN", (), Map.empty),
        Lexeme("IDENTIFIER", "x", Map.empty),
        Lexeme("PLUS", (), Map.empty),
        Lexeme("NUMBER", "42", Map.empty),
        Lexeme("RPAREN", (), Map.empty),
        Lexeme("MULTIPLY", (), Map.empty),
        Lexeme("IDENTIFIER", "y", Map.empty),
        Lexeme("MINUS", (), Map.empty),
        Lexeme("NUMBER", "1", Map.empty),
      ),
    )
  }

  test("throws on overlapping regex patterns") {
    """val Lexer = lexer {
      case "[a-zA-Z_][a-zA-Z0-9_]*" => Token["IDENTIFIER"]
      case "[a-zA-Z]+" => Token["ALPHABETIC"]
    }""" shouldNot compile
  }

  test("tokenize file") {
    val Lexer = lexer {
      case number @ "[0-9]+" => Token["NUMBER"](number)
      case id @ "[a-zA-Z][a-zA-Z0-9]*" => Token["IDENTIFIER"](id)
      case "\\+" => Token["PLUS"]
      case "-" => Token["MINUS"]
      case "\\*" => Token["MULTIPLY"]
      case "\\(" => Token["LPAREN"]
      case "\\)" => Token["RPAREN"]
      case "\\s+" => Token.Ignored
    }

    withLazyReader("(x + 42) * y - 1") { reader =>
      val result = Lexer.tokenize(reader)

      assert(
        result == List(
          Lexeme("LPAREN", (), Map.empty),
          Lexeme("IDENTIFIER", "x", Map.empty),
          Lexeme("PLUS", (), Map.empty),
          Lexeme("NUMBER", "42", Map.empty),
          Lexeme("RPAREN", (), Map.empty),
          Lexeme("MULTIPLY", (), Map.empty),
          Lexeme("IDENTIFIER", "y", Map.empty),
          Lexeme("MINUS", (), Map.empty),
          Lexeme("NUMBER", "1", Map.empty),
        ),
      )
    }
  }
}
