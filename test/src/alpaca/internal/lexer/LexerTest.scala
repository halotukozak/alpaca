package alpaca
package internal.lexer

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

final class LexerTest extends AnyFunSuite with Matchers:

  /**
   * A comparable shape of a Lexeme, used only for test assertions so we
   * don't need content-based equals on Lexeme itself.
   */
  private case class LexemeShape(name: String, value: Any, fields: Map[String, Any])

  private def lex(name: String, value: Any, fields: Map[String, Any]): LexemeShape =
    LexemeShape(name, value, fields)

  extension (lexeme: Lexeme[?, ?])
    private def shape: LexemeShape =
      val fields = lexeme.fieldNames.iterator.zip(lexeme.fieldValues.iterator).toMap + ("text" -> lexeme.text)
      LexemeShape(lexeme.name, lexeme.value, fields)

  test("tokenize simple identifier") {
    val Lexer = lexer:
      case id @ "[a-zA-Z][a-zA-Z0-9]*" => Token["IDENTIFIER"](id)

    val (_, lexemes) = Lexer.tokenize("hello")
    assert(lexemes.map(_.shape) == List(lex("IDENTIFIER", "hello", Map("text" -> "hello", "position" -> 6, "line" -> 1))))
  }

  test("tokenize with whitespace ignored") {
    val Lexer = lexer:
      case number @ "[0-9]+" => Token["NUMBER"](number)
      case "\\+" => Token["PLUS"]
      case "\\s+" => Token.Ignored

    val (_, lexemes) = Lexer.tokenize("42 + 13")

    assert(
      lexemes.map(_.shape) == List(
        lex("NUMBER", "42", Map("text" -> "42", "position" -> 3, "line" -> 1)),
        lex("PLUS", (), Map("text" -> "+", "position" -> 5, "line" -> 1)),
        lex("NUMBER", "13", Map("text" -> "13", "position" -> 8, "line" -> 1)),
      ),
    )
  }

  test("tokenize empty string") {
    val Lexer = lexer:
      case id @ "[a-zA-Z][a-zA-Z0-9]*" => Token["IDENTIFIER"](id)

    val (_, lexemes) = Lexer.tokenize("")
    assert(lexemes == Nil)
  }

  test("throw exception for unexpected character") {
    val Lexer = lexer:
      case number @ "[0-9]+" => Token["NUMBER"](number.toInt)

    val exception = intercept[RuntimeException]:
      Lexer.tokenize("123abc")

    assert(exception.getMessage.contains("Unexpected character at line 1, position 4: 'a'"))
  }

  test("tokenize complex expression") {
    val Lexer = lexer:
      case number @ "[0-9]+" => Token["NUMBER"](number)
      case id @ "[a-zA-Z][a-zA-Z0-9]*" => Token["IDENTIFIER"](id)
      case "\\+" => Token["PLUS"]
      case "-" => Token["MINUS"]
      case "\\*" => Token["MULTIPLY"]
      case "\\(" => Token["LPAREN"]
      case "\\)" => Token["RPAREN"]
      case "\\s+" => Token.Ignored

    val (_, lexemes) = Lexer.tokenize("(x + 42) * y - 1")

    assert(
      lexemes.map(_.shape) == List(
        lex("LPAREN", (), Map("text" -> "(", "position" -> 2, "line" -> 1)),
        lex("IDENTIFIER", "x", Map("text" -> "x", "position" -> 3, "line" -> 1)),
        lex("PLUS", (), Map("text" -> "+", "position" -> 5, "line" -> 1)),
        lex("NUMBER", "42", Map("text" -> "42", "position" -> 8, "line" -> 1)),
        lex("RPAREN", (), Map("text" -> ")", "position" -> 9, "line" -> 1)),
        lex("MULTIPLY", (), Map("text" -> "*", "position" -> 11, "line" -> 1)),
        lex("IDENTIFIER", "y", Map("text" -> "y", "position" -> 13, "line" -> 1)),
        lex("MINUS", (), Map("text" -> "-", "position" -> 15, "line" -> 1)),
        lex("NUMBER", "1", Map("text" -> "1", "position" -> 17, "line" -> 1)),
      ),
    )
  }

  test("throws on overlapping regex patterns") {
    """val Lexer = lexer:
      case "[a-zA-Z_][a-zA-Z0-9_]*" => Token["IDENTIFIER"]
      case "[a-zA-Z]+" => Token["ALPHABETIC"]
    """ shouldNot compile
  }

  test("throws on overlapping alternatives in same case - longer first") {
    """val Lexer = lexer:
      case ">=" | ">" => Token["GREATER"]
    """ shouldNot compile
  }

  test("throws on overlapping alternatives in same case - shorter first") {
    """val Lexer = lexer:
      case ">" | ">=" => Token["GREATER"]
    """ shouldNot compile
  }

  test("track line and position across newlines") {
    val Lexer = lexer:
      case id @ "[a-zA-Z]+" => Token["IDENTIFIER"](id)
      case "\\s+" => Token.Ignored

    val (ctx, lexemes) = Lexer.tokenize("abc\ndef")
    assert(
      lexemes.map(_.shape) == List(
        lex("IDENTIFIER", "abc", Map("text" -> "abc", "position" -> 4, "line" -> 1)),
        lex("IDENTIFIER", "def", Map("text" -> "def", "position" -> 4, "line" -> 2)),
      ),
    )
    ctx.line shouldBe 2
    ctx.position shouldBe 4
  }

  test("tokenize file") {
    val Lexer = lexer:
      case number @ "[0-9]+" => Token["NUMBER"](number)
      case id @ "[a-zA-Z][a-zA-Z0-9]*" => Token["IDENTIFIER"](id)
      case "\\+" => Token["PLUS"]
      case "-" => Token["MINUS"]
      case "\\*" => Token["MULTIPLY"]
      case "\\(" => Token["LPAREN"]
      case "\\)" => Token["RPAREN"]
      case "\\s+" => Token.Ignored

    withLazyReader("(x + 42) * y - 1"): reader =>
      val (_, lexemes) = Lexer.tokenize(reader)

      assert(
        lexemes.map(_.shape) == List(
          lex("LPAREN", (), Map("text" -> "(", "position" -> 2, "line" -> 1)),
          lex("IDENTIFIER", "x", Map("text" -> "x", "position" -> 3, "line" -> 1)),
          lex("PLUS", (), Map("text" -> "+", "position" -> 5, "line" -> 1)),
          lex("NUMBER", "42", Map("text" -> "42", "position" -> 8, "line" -> 1)),
          lex("RPAREN", (), Map("text" -> ")", "position" -> 9, "line" -> 1)),
          lex("MULTIPLY", (), Map("text" -> "*", "position" -> 11, "line" -> 1)),
          lex("IDENTIFIER", "y", Map("text" -> "y", "position" -> 13, "line" -> 1)),
          lex("MINUS", (), Map("text" -> "-", "position" -> 15, "line" -> 1)),
          lex("NUMBER", "1", Map("text" -> "1", "position" -> 17, "line" -> 1)),
        ),
      )
  }
