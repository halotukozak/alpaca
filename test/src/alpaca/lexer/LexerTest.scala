package alpaca.lexer

import org.scalatest.funsuite.AnyFunSuite

final class LexerTest extends AnyFunSuite {

  test("tokenize simple identifier") {
    val Lexer = lexer[EmptyCtx] { case "[a-zA-Z][a-zA-Z0-9]*" => Token["IDENTIFIER"] }

    val result = Lexer.tokenize("hello", EmptyCtx.create)

    assert(result == List(Lexem("IDENTIFIER", "hello", ???)))
  }

  test("tokenize with whitespace ignored") {
    val Lexer = lexer {
      case "[0-9]+" => Token["NUMBER"]
      case "\\+" => Token["PLUS"]
      case "\\s+" => Token.Ignored["WHITESPACE"]
    }
    val result = Lexer.tokenize("42 + 13", EmptyCtx.create)

    assert(
      result == List(
        Lexem("NUMBER", "42", ???),
        Lexem("PLUS", "+", ???),
        Lexem("NUMBER", "13", ???),
      ),
    )
  }

  test("tokenize empty string") {
    val Lexer = lexer { case "[a-zA-Z][a-zA-Z0-9]*" => Token["IDENTIFIER"] }
    val result = Lexer.tokenize("", EmptyCtx.create)

    assert(result == List.empty)
  }

  test("throw exception for unexpected character") {
    val Lexer = lexer { case "[0-9]+" => Token["NUMBER"] }

    val exception = intercept[RuntimeException] {
      Lexer.tokenize("123abc", EmptyCtx.create)
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
      case "\\s+" => Token.Ignored["WHITESPACE"]
    }
    val result = Lexer.tokenize("(x + 42) * y - 1", EmptyCtx.create)

    assert(
      result == List(
        Lexem("LPAREN", "(", ???),
        Lexem("IDENTIFIER", "x", ???),
        Lexem("PLUS", "+", ???),
        Lexem("NUMBER", "42", ???),
        Lexem("RPAREN", ")", ???),
        Lexem("MULTIPLY", "*", ???),
        Lexem("IDENTIFIER", "y", ???),
        Lexem("MINUS", "-", ???),
        Lexem("NUMBER", "1", ???),
      ),
    )
  }
}
