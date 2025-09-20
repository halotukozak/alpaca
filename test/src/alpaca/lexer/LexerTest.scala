package alpaca.lexer

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import java.nio.file.Path
import java.nio.file.Files
import alpaca.lexer.context.Lexem

final class LexerTest extends AnyFunSuite with Matchers {

  test("tokenize simple identifier") {
    val Lexer = lexer { case id @ "[a-zA-Z][a-zA-Z0-9]*" => Token["IDENTIFIER"](id) }
    val result = Lexer.tokenize("hello")
    assert(result == List(Lexem("IDENTIFIER", "hello")))
  }

  test("tokenize with whitespace ignored") {
    val Lexer = lexer {
      case number @ "[0-9]+" => Token["NUMBER"](number)
      case "\\+" => Token["PLUS"]
      case "\\s+" => Token.Ignored["WHITESPACE"]
    }
    val result = Lexer.tokenize("42 + 13")

    assert(
      result == List(
        Lexem("NUMBER", "42"),
        Lexem("PLUS", ()),
        Lexem("NUMBER", "13"),
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
      case "\\s+" => Token.Ignored["WHITESPACE"]
    }
    val result = Lexer.tokenize("(x + 42) * y - 1")

    assert(
      result == List(
        Lexem("LPAREN", ()),
        Lexem("IDENTIFIER", "x"),
        Lexem("PLUS", ()),
        Lexem("NUMBER", "42"),
        Lexem("RPAREN", ()),
        Lexem("MULTIPLY", ()),
        Lexem("IDENTIFIER", "y"),
        Lexem("MINUS", ()),
        Lexem("NUMBER", "1"),
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
      case "\\s+" => Token.Ignored["WHITESPACE"]
    }

    val tempFile = Files.createTempFile("lexer_test", ".txt")
    Files.writeString(tempFile, "(x + 42) * y - 1")
    val reader = LazyReader.from(tempFile.toAbsolutePath)
    val result = Lexer.tokenize(reader)

    assert(
      result == List(
        Lexem("LPAREN", ()),
        Lexem("IDENTIFIER", "x"),
        Lexem("PLUS", ()),
        Lexem("NUMBER", "42"),
        Lexem("RPAREN", ()),
        Lexem("MULTIPLY", ()),
        Lexem("IDENTIFIER", "y"),
        Lexem("MINUS", ()),
        Lexem("NUMBER", "1"),
      ),
    )

    Files.deleteIfExists(tempFile)
  }
}
