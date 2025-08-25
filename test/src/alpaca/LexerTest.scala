package alpaca

import org.scalatest.funsuite.AnyFunSuite
import java.nio.file.Path

class LexerTest extends AnyFunSuite {
  
  test("tokenize simple identifier") {
    val tokens = List(Token("IDENTIFIER", "[a-zA-Z][a-zA-Z0-9]*".r))
    val lexer = Lexer(tokens)
    val result = lexer.tokenize("hello")
    
    assert(result == List(Lexem("IDENTIFIER", "hello")))
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
      Lexem("NUMBER", "42"),
      Lexem("PLUS", "+"),
      Lexem("NUMBER", "13")
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
    assert(exception.getMessage == "Unexpected character: 'a'")
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
      Lexem("LPAREN", "("),
      Lexem("IDENTIFIER", "x"),
      Lexem("PLUS", "+"),
      Lexem("NUMBER", "42"),
      Lexem("RPAREN", ")"),
      Lexem("MULTIPLY", "*"),
      Lexem("IDENTIFIER", "y"),
      Lexem("MINUS", "-"),
      Lexem("NUMBER", "1")
    ))
  }

  test("tokeniza file") {
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
    val result = lexer.tokenize(Path.of("in/test.txt"))
    
    assert(result == List(
      Lexem("LPAREN", "("),
      Lexem("IDENTIFIER", "x"),
      Lexem("PLUS", "+"),
      Lexem("NUMBER", "42"),
      Lexem("RPAREN", ")"),
      Lexem("MULTIPLY", "*"),
      Lexem("IDENTIFIER", "y"),
      Lexem("MINUS", "-"),
      Lexem("NUMBER", "1")
    ))
  }
}
