package alpaca.lexer

import alpaca.lexer.context.default.DefaultLexem
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import java.nio.file.Path
import java.nio.file.Files

final class LexerTest extends AnyFunSuite with Matchers {

  test("tokenize simple identifier") {
    val Lexer = lexer { case "[a-zA-Z][a-zA-Z0-9]*" => Token["IDENTIFIER"] }
    val result = Lexer.tokenize("hello")
    assert(result == List(DefaultLexem("IDENTIFIER", "hello")))
  }

  test("tokenize with whitespace ignored") {
    val Lexer = lexer {
      case "[0-9]+" => Token["NUMBER"]
      case "\\+" => Token["PLUS"]
      case "\\s+" => Token.Ignored["WHITESPACE"]
    }
    val result = Lexer.tokenize("42 + 13")

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
    val result = Lexer.tokenize("")
    assert(result == List.empty)
  }

  test("throw exception for unexpected character") {
    val Lexer = lexer { case "[0-9]+" => Token["NUMBER"] }

    val exception = intercept[RuntimeException] {
      Lexer.tokenize("123abc")
    }
    assert(exception.getMessage.contains("Unexpected character: 'a'"))
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
    val result = Lexer.tokenize("(x + 42) * y - 1")

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

  test("throws on overlapping regex patterns") {
    """val Lexer = lexer {
      case "[a-zA-Z_][a-zA-Z0-9_]*" => Token["IDENTIFIER"]
      case "[a-zA-Z]+" => Token["ALPHABETIC"]
    }""" shouldNot compile
  }

  test("tokenize file") {
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

    val tempFile = Files.createTempFile("lexer_test", ".txt")
    Files.writeString(tempFile, "(x + 42) * y - 1")
    val reader = LazyReader.from(tempFile.toAbsolutePath)
    val result = Lexer.tokenize(reader)

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

    Files.deleteIfExists(tempFile)
  }
}
