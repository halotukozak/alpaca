package alpaca.lexer

import org.scalatest.funsuite.AnyFunSuite
import alpaca.core.:=
import alpaca.core.:=.given
import alpaca.core.Copyable.given

final class LexerTest extends AnyFunSuite {

  test("tokenize simple identifier") {
    val Lexer = lexer { case "[a-zA-Z][a-zA-Z0-9]*" => Token["IDENTIFIER"] }

    val result = Lexer.tokenize("hello", EmptyGlobalCtx())

    assert(result == List(DefaultLexem("IDENTIFIER", "hello")))
  }

  test("tokenize with whitespace ignored") {
    val Lexer = lexer {
      case "[0-9]+" => Token["NUMBER"]
      case "\\+" => Token["PLUS"]
      case "\\s+" => Token.Ignored["WHITESPACE"]
    }
    val result = Lexer.tokenize("42 + 13", EmptyGlobalCtx())

    assert(
      result == List(
        DefaultLexem("NUMBER", "42"),
        DefaultLexem("PLUS", "+"),
        DefaultLexem("NUMBER", "13"),
      ),
    )
  }

  test("tokenize empty string") {
    val Lexer = lexer { case "[a-zA-Z][a-zA-Z0-9]*" => Token["IDENTIFIER"] }
    val result = Lexer.tokenize("", EmptyGlobalCtx())

    assert(result == List.empty)
  }

  // todo: https://github.com/halotukozak/alpaca/issues/51
  // test("throw exception for unexpected character") {
  //   val Lexer = lexer { case "[0-9]+" => Token["NUMBER"] }

  //   val exception = intercept[RuntimeException] {
  //     Lexer.tokenize("123abc", NoCtx.create)
  //   }
  //   assert(exception.getMessage.contains("Unexpected character at position 3"))
  // }

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
    val result = Lexer.tokenize("(x + 42) * y - 1", EmptyGlobalCtx())

    assert(
      result == List(
        DefaultLexem("LPAREN", "("),
        DefaultLexem("IDENTIFIER", "x"),
        DefaultLexem("PLUS", "+"),
        DefaultLexem("NUMBER", "42"),
        DefaultLexem("RPAREN", ")"),
        DefaultLexem("MULTIPLY", "*"),
        DefaultLexem("IDENTIFIER", "y"),
        DefaultLexem("MINUS", "-"),
        DefaultLexem("NUMBER", "1"),
      ),
    )
  }
}
