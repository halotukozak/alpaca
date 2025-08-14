package alpaca

import alpaca.temp.{Token, Tokenize, lexer}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

val Lexer: Tokenize = lexer {
  case literal @ ("<" | ">" | "=" | "+" | "-" | "*" | "/" | "(" | ")" | "[" | "]" | "{" | "}" | ":" | "'" | "," |
      ";") =>
    Token[literal.type]
  case "\\.\\+" => Token["dotAdd"]
  case "\\.\\-" => Token["dotSub"]
  case "\\.\\*" => Token["dotMul"]
  case "\\.\\/" => Token["dotDiv"]
  case "<=" => Token["lessEqual"]
  case comment @ "#.*" => Token.Ignored
  case ">=" => Token["greaterEqual"]
  case "!=" => Token["notEqual"]
  case "==" => Token["equal"]
  case x @ "(d+(\\.\\d*)|\\.\\d+)([eE][+-]?\\d+)?" => Token["float"](x.toDouble)
  case x @ "[0-9]+" => Token["int"](x.toInt)
  case x @ "[^\"]*" => Token["string"](x)
  case keyword @ ("if" | "else" | "for" | "while" | "break" | "continue" | "return" | "eye" | "zeros" | "ones" |
      "print") =>
    Token[keyword.type]
  case x @ "[a-zA-Z_][a-zA-Z0-9_]*" => Token["id"](x)
}

class LexerApiTest extends AnyFunSuite with Matchers {
  test("Lexer recognizes basic tokens") {
    Lexer.tokens.toList.map(_.pattern) shouldBe List(
      "#.*",
      "<|>|=|+|-|*|/|(|)|[|]|{|}|:|'|,|;",
      raw"\.\+",
      raw"\.\-",
      raw"\.\*",
      raw"\.\/",
      "<=",
      ">=",
      "!=",
      "==",
      raw"(d+(\.\d*)|\.\d+)([eE][+-]?\d+)?",
      "[0-9]+",
      "[^\"]*",
      "if|else|for|while|break|continue|return|eye|zeros|ones|print",
      "[a-zA-Z_][a-zA-Z0-9_]*",
    )
  }
}
