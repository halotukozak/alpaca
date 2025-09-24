package alpaca.lexer

import alpaca.lexer.context.default.DefaultLexem
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

final class LexerTest extends AnyFunSuite with Matchers {

  test("tokenize simple identifier") {
    val Lexer = lexer { case "[a-zA-Z][a-zA-Z0-9]*" => Token["IDENTIFIER"] }

//    val result = Lexer.tokenize("hello")
    // todo: https://github.com/halotukozak/alpaca/issues/51
    //   assert(result == List(DefaultLexem("IDENTIFIER", "hello")))
  }

  test("tokenize with whitespace ignored") {
    val Lexer = lexer {
      case "[0-9]+" => Token["NUMBER"]
      case "\\+" => Token["PLUS"]
      case "\\s+" => Token.Ignored
    }
//    val result = Lexer.tokenize("42 + 13")

    // todo: https://github.com/halotukozak/alpaca/issues/51
    // assert(
    //   result == List(
    //     DefaultLexem("NUMBER", "42"),
    //     DefaultLexem("PLUS", "+"),
    //     DefaultLexem("NUMBER", "13"),
    //   ),
    // )
  }

  test("tokenize empty string") {
    val Lexer = lexer { case "[a-zA-Z][a-zA-Z0-9]*" => Token["IDENTIFIER"] }
//    val result = Lexer.tokenize("")

    // todo: https://github.com/halotukozak/alpaca/issues/51
    // assert(result == List.empty)
  }

  test("throw exception for unexpected character") {
    val Lexer = lexer { case "[0-9]+" => Token["NUMBER"] }

    // todo: https://github.com/halotukozak/alpaca/issues/51
    // val exception = intercept[RuntimeException] {
    //   Lexer.tokenize("123abc")
    // }
    // assert(exception.getMessage.contains("Unexpected character at position 3"))
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
    // val result = Lexer.tokenize("(x + 42) * y - 1")

    // todo: https://github.com/halotukozak/alpaca/issues/51
    // assert(
    //   result == List(
    //     DefaultLexem("LPAREN", "("),
    //     DefaultLexem("IDENTIFIER", "x"),
    //     DefaultLexem("PLUS", "+"),
    //     DefaultLexem("NUMBER", "42"),
    //     DefaultLexem("RPAREN", ")"),
    //     DefaultLexem("MULTIPLY", "*"),
    //     DefaultLexem("IDENTIFIER", "y"),
    //     DefaultLexem("MINUS", "-"),
    //     DefaultLexem("NUMBER", "1"),
    //   ),
    // )
  }

  test("throws on overlapping regex patterns") {
    """val Lexer = lexer {
      case "[a-zA-Z_][a-zA-Z0-9_]*" => Token["IDENTIFIER"]
      case "[a-zA-Z]+" => Token["ALPHABETIC"]
    }""" shouldNot compile
  }
}
