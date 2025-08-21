package alpaca.lexer

import org.scalatest.funsuite.AnyFunSuite

final class LexerTest extends AnyFunSuite {

  test("tokenize simple identifier") {
    val Lexer = lexer { case "[a-zA-Z][a-zA-Z0-9]*" => Token["IDENTIFIER"] }

    val result = Lexer.tokenize("hello")

    assert(result == List(Lexem("IDENTIFIER", "hello", 0)))
  }

  test("tokenize with whitespace ignored") {
    val Lexer = lexer {
      case "[0-9]+" => Token["NUMBER"]
      case "\\+" => Token["PLUS"]
      case "\\s+" => Token.Ignored
    }
    val result = Lexer.tokenize("42 + 13")

    assert(
      result == List(
        Lexem("NUMBER", "42", 0),
        Lexem("PLUS", "+", 3),
        Lexem("NUMBER", "13", 5),
      )
    )
  }

  test("tokenize empty string") {
    val Lexer = lexer { case "[a-zA-Z][a-zA-Z0-9]*" => Token["IDENTIFIER"] }
    val result = Lexer.tokenize("")

    assert(result == List.empty)
  }

  test("throw exception for unexpected character") {
    val Lexer = lexer { case "[0-9]+" => Token["NUMBER"] }

    val exception = intercept[RuntimeException] {
      Lexer.tokenize("123abc")
    }
    assert(exception.getMessage.contains("Unexpected character at position 3"))
  }

  test("tokenize complex expression") {
    val Lexer = lexer {
      case "[0-9]+" => Token["NUMBER"]
      case "[a-zA-Z][a-zA-Z0-9]*" => Token["IDENTIFIER"]
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
        Lexem("LPAREN", "(", 0),
        Lexem("IDENTIFIER", "x", 1),
        Lexem("PLUS", "+", 3),
        Lexem("NUMBER", "42", 5),
        Lexem("RPAREN", ")", 7),
        Lexem("MULTIPLY", "*", 9),
        Lexem("IDENTIFIER", "y", 11),
        Lexem("MINUS", "-", 13),
        Lexem("NUMBER", "1", 15),
      )
    )
  }
}
