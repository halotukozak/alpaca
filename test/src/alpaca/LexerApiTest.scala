package alpaca

import alpaca.internal.lexer.Token
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.annotation.nowarn

@nowarn("msg=A pure expression")
final class LexerApiTest extends AnyFunSuite with Matchers {
  val Lexer = lexer {
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
    case x @ "\"[^\"]*\"" => Token["string"](x)
    case keyword @ ("if" | "else" | "for" | "while" | "break" | "continue" | "return" | "eye" | "zeros" | "ones" |
        "print") =>
      Token[keyword.type]
    case x @ "[a-zA-Z_][a-zA-Z0-9_]*" => Token["id"](x)
    case literal @ ("<" | ">" | "=" | "\\+" | "-" | "\\*" | "/" | "\\(" | "\\)" | "\\[" | "\\]" | "\\{" | "\\}" | ":" |
        "'" | "," | ";") =>
      Token[literal.type]
  }

  test("Lexer recognizes basic tokens") {
    Lexer.tokens.map(_.info.pattern) shouldBe List(
    //format: off
      "#.*",
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
      "\"[^\"]*\"",
      "if", "else", "for", "while", "break", "continue", "return", "eye", "zeros", "ones", "print",
      "[a-zA-Z_][a-zA-Z0-9_]*",
      "<", ">", "=", "\\+", "-", "\\*", "/", "\\(", "\\)", "\\[", "\\]", "\\{", "\\}", ":", "'", ",", ";",
    )
    //format: on

    // we check if compiles and not crashes
    Lexer.< : Token[LexerCtx.Default, "<"]
    Lexer.> : Token[LexerCtx.Default, ">"]
    Lexer.`=`: Token[LexerCtx.Default, "="]
    Lexer.`\\+`: Token[LexerCtx.Default, "\\+"]
    Lexer.- : Token[LexerCtx.Default, "-"]
    Lexer.`\\*`: Token[LexerCtx.Default, "\\*"]
    Lexer.`/`: Token[LexerCtx.Default, "/"]
    Lexer.`\\(`: Token[LexerCtx.Default, "\\("]
    Lexer.`\\)`: Token[LexerCtx.Default, "\\)"]
    Lexer.`\\[`: Token[LexerCtx.Default, "\\["]
    Lexer.`\\]`: Token[LexerCtx.Default, "\\]"]
    Lexer.`\\{`: Token[LexerCtx.Default, "\\{"]
    Lexer.`\\}`: Token[LexerCtx.Default, "\\}"]
    Lexer.`:`: Token[LexerCtx.Default, ":"]
    Lexer.`'`: Token[LexerCtx.Default, "'"]
    Lexer.`,`: Token[LexerCtx.Default, ","]
    Lexer.`;`: Token[LexerCtx.Default, ";"]
    Lexer.dotAdd: Token[LexerCtx.Default, "dotAdd"]
    Lexer.dotSub: Token[LexerCtx.Default, "dotSub"]
    Lexer.dotMul: Token[LexerCtx.Default, "dotMul"]
    Lexer.dotDiv: Token[LexerCtx.Default, "dotDiv"]
    Lexer.lessEqual: Token[LexerCtx.Default, "lessEqual"]
    Lexer.greaterEqual: Token[LexerCtx.Default, "greaterEqual"]
    Lexer.notEqual: Token[LexerCtx.Default, "notEqual"]
    Lexer.equal: Token[LexerCtx.Default, "equal"]
    Lexer.float: Token[LexerCtx.Default, "float"]
    Lexer.int: Token[LexerCtx.Default, "int"]
    Lexer.string: Token[LexerCtx.Default, "string"]
    Lexer.`if`: Token[LexerCtx.Default, "if"]
    Lexer.`else`: Token[LexerCtx.Default, "else"]
    Lexer.`for`: Token[LexerCtx.Default, "for"]
    Lexer.`while`: Token[LexerCtx.Default, "while"]
    Lexer.break: Token[LexerCtx.Default, "break"]
    Lexer.continue: Token[LexerCtx.Default, "continue"]
    Lexer.`return`: Token[LexerCtx.Default, "return"]
    Lexer.eye: Token[LexerCtx.Default, "eye"]
    Lexer.zeros: Token[LexerCtx.Default, "zeros"]
    Lexer.ones: Token[LexerCtx.Default, "ones"]
    Lexer.print: Token[LexerCtx.Default, "print"]
    Lexer.id: Token[LexerCtx.Default, "id"]
  }
}
