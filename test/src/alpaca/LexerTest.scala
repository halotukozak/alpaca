package alpaca

import org.scalatest.funsuite.AnyFunSuite

class LexerTest extends AnyFunSuite {

  test("tokenize simple identifier") {
    val tokens = List(Token("IDENTIFIER", "[a-zA-Z][a-zA-Z0-9]*".r))
    val lexer = Lexer(tokens)
    val result = lexer.tokenize("hello")

    assert(result == List(Lexem("IDENTIFIER", "hello", 0)))
  }

  test("tokenize with whitespace ignored") {
    val tokens = List(
      Token("NUMBER", "[0-9]+".r),
      Token("PLUS", "\\+".r),
      Token("WHITESPACE", "\\s+".r, ignore = true)
    )
    val lexer = Lexer(tokens)
    val result = lexer.tokenize("42 + 13")

    assert(result == List(
      Lexem("NUMBER", "42", 0),
      Lexem("PLUS", "+", 3),
      Lexem("NUMBER", "13", 5)
    ))
  }

  test("tokenize empty string") {
    val tokens = List(Token("IDENTIFIER", "[a-zA-Z]+".r))
    val lexer = Lexer(tokens)
    val result = lexer.tokenize("")

    assert(result == List.empty)
  }

  test("throw exception for unexpected character") {
    val tokens = List(Token("NUMBER", "[0-9]+".r))
    val lexer = Lexer(tokens)

    val exception = intercept[RuntimeException] {
      lexer.tokenize("123abc")
    }
    assert(exception.getMessage.contains("Unexpected character at position 3"))
  }

  test("tokenize complex expression") {
    val tokens = List(
      Token("NUMBER", "[0-9]+".r),
      Token("IDENTIFIER", "[a-zA-Z][a-zA-Z0-9]*".r),
      Token("PLUS", "\\+".r),
      Token("MINUS", "-".r),
      Token("MULTIPLY", "\\*".r),
      Token("LPAREN", "\\(".r),
      Token("RPAREN", "\\)".r),
      Token("WHITESPACE", "\\s+".r, ignore = true)
    )
    val lexer = Lexer(tokens)
    val result = lexer.tokenize("(x + 42) * y - 1")

    assert(result == List(
      Lexem("LPAREN", "(", 0),
      Lexem("IDENTIFIER", "x", 1),
      Lexem("PLUS", "+", 3),
      Lexem("NUMBER", "42", 5),
      Lexem("RPAREN", ")", 7),
      Lexem("MULTIPLY", "*", 9),
      Lexem("IDENTIFIER", "y", 11),
      Lexem("MINUS", "-", 13),
      Lexem("NUMBER", "1", 15)
    ))
  }
}
