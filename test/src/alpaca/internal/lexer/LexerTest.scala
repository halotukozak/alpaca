package alpaca
package internal.lexer

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import scala.annotation.unchecked.uncheckedVariance

final class LexerTest extends AnyFunSuite with Matchers {

  test("tokenize simple identifier") {
    val Lexer = lexer { case id @ "[a-zA-Z][a-zA-Z0-9]*" => Token["IDENTIFIER"](id) }
    val (_, lexemes) = Lexer.tokenize("hello")
    assert(lexemes == List(Lexeme("IDENTIFIER", "hello", Map("text" -> "hello", "position" -> 6, "line" -> 1))))
  }

  test("tokenize with whitespace ignored") {
    val Lexer = lexer {
      case number @ "[0-9]+" => Token["NUMBER"](number)
      case "\\+" => Token["PLUS"]
      case "\\s+" => Token.Ignored
    }
    val (_, lexemes) = Lexer.tokenize("42 + 13")

    assert(
      lexemes == List(
        Lexeme("NUMBER", "42", Map("text" -> "42", "position" -> 3, "line" -> 1)),
        Lexeme("PLUS", (), Map("text" -> "+", "position" -> 5, "line" -> 1)),
        Lexeme("NUMBER", "13", Map("text" -> "13", "position" -> 8, "line" -> 1)),
      ),
    )
  }

  test("tokenize empty string") {
    val Lexer = lexer { case id @ "[a-zA-Z][a-zA-Z0-9]*" => Token["IDENTIFIER"](id) }
    val (_, lexemes) = Lexer.tokenize("")
    assert(lexemes == List.empty)
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
    val (_, lexemes) = Lexer.tokenize("(x + 42) * y - 1")

    assert(
      lexemes == List(
        Lexeme("LPAREN", (), Map("text" -> "(", "position" -> 2, "line" -> 1)),
        Lexeme("IDENTIFIER", "x", Map("text" -> "x", "position" -> 3, "line" -> 1)),
        Lexeme("PLUS", (), Map("text" -> "+", "position" -> 5, "line" -> 1)),
        Lexeme("NUMBER", "42", Map("text" -> "42", "position" -> 8, "line" -> 1)),
        Lexeme("RPAREN", (), Map("text" -> ")", "position" -> 9, "line" -> 1)),
        Lexeme("MULTIPLY", (), Map("text" -> "*", "position" -> 11, "line" -> 1)),
        Lexeme("IDENTIFIER", "y", Map("text" -> "y", "position" -> 13, "line" -> 1)),
        Lexeme("MINUS", (), Map("text" -> "-", "position" -> 15, "line" -> 1)),
        Lexeme("NUMBER", "1", Map("text" -> "1", "position" -> 17, "line" -> 1)),
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
      val (_, lexemes) = Lexer.tokenize(reader)

      assert(
        lexemes == List(
          Lexeme("LPAREN", (), Map("text" -> "(", "position" -> 2, "line" -> 1)),
          Lexeme("IDENTIFIER", "x", Map("text" -> "x", "position" -> 3, "line" -> 1)),
          Lexeme("PLUS", (), Map("text" -> "+", "position" -> 5, "line" -> 1)),
          Lexeme("NUMBER", "42", Map("text" -> "42", "position" -> 8, "line" -> 1)),
          Lexeme("RPAREN", (), Map("text" -> ")", "position" -> 9, "line" -> 1)),
          Lexeme("MULTIPLY", (), Map("text" -> "*", "position" -> 11, "line" -> 1)),
          Lexeme("IDENTIFIER", "y", Map("text" -> "y", "position" -> 13, "line" -> 1)),
          Lexeme("MINUS", (), Map("text" -> "-", "position" -> 15, "line" -> 1)),
          Lexeme("NUMBER", "1", Map("text" -> "1", "position" -> 17, "line" -> 1)),
        ),
      )
    }
  }
}
