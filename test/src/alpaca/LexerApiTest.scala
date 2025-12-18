package alpaca

import alpaca.internal.lexer.Token
import alpaca.internal.lexer.NamedToken
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
    Lexer.< : Token[LexerCtx.Default] & NamedToken["<"]
    Lexer.> : Token[LexerCtx.Default] & NamedToken[">"]
    Lexer.`=`: Token[LexerCtx.Default] & NamedToken["="]
    Lexer.`\\+`: Token[LexerCtx.Default] & NamedToken["\\+"]
    Lexer.- : Token[LexerCtx.Default] & NamedToken["-"]
    Lexer.`\\*`: Token[LexerCtx.Default] & NamedToken["\\*"]
    Lexer.`/`: Token[LexerCtx.Default] & NamedToken["/"]
    Lexer.`\\(`: Token[LexerCtx.Default] & NamedToken["\\("]
    Lexer.`\\)`: Token[LexerCtx.Default] & NamedToken["\\)"]
    Lexer.`\\[`: Token[LexerCtx.Default] & NamedToken["\\["]
    Lexer.`\\]`: Token[LexerCtx.Default] & NamedToken["\\]"]
    Lexer.`\\{`: Token[LexerCtx.Default] & NamedToken["\\{"]
    Lexer.`\\}`: Token[LexerCtx.Default] & NamedToken["\\}"]
    Lexer.`:`: Token[LexerCtx.Default] & NamedToken[":"]
    Lexer.`'`: Token[LexerCtx.Default] & NamedToken["'"]
    Lexer.`,`: Token[LexerCtx.Default] & NamedToken[","]
    Lexer.`;`: Token[LexerCtx.Default] & NamedToken[";"]
    Lexer.dotAdd: Token[LexerCtx.Default] & NamedToken["dotAdd"]
    Lexer.dotSub: Token[LexerCtx.Default] & NamedToken["dotSub"]
    Lexer.dotMul: Token[LexerCtx.Default] & NamedToken["dotMul"]
    Lexer.dotDiv: Token[LexerCtx.Default] & NamedToken["dotDiv"]
    Lexer.lessEqual: Token[LexerCtx.Default] & NamedToken["lessEqual"]
    Lexer.greaterEqual: Token[LexerCtx.Default] & NamedToken["greaterEqual"]
    Lexer.notEqual: Token[LexerCtx.Default] & NamedToken["notEqual"]
    Lexer.equal: Token[LexerCtx.Default] & NamedToken["equal"]
    Lexer.float: Token[LexerCtx.Default] & NamedToken["float"]
    Lexer.int: Token[LexerCtx.Default] & NamedToken["int"]
    Lexer.string: Token[LexerCtx.Default] & NamedToken["string"]
    Lexer.`if`: Token[LexerCtx.Default] & NamedToken["if"]
    Lexer.`else`: Token[LexerCtx.Default] & NamedToken["else"]
    Lexer.`for`: Token[LexerCtx.Default] & NamedToken["for"]
    Lexer.`while`: Token[LexerCtx.Default] & NamedToken["while"]
    Lexer.break: Token[LexerCtx.Default] & NamedToken["break"]
    Lexer.continue: Token[LexerCtx.Default] & NamedToken["continue"]
    Lexer.`return`: Token[LexerCtx.Default] & NamedToken["return"]
    Lexer.eye: Token[LexerCtx.Default] & NamedToken["eye"]
    Lexer.zeros: Token[LexerCtx.Default] & NamedToken["zeros"]
    Lexer.ones: Token[LexerCtx.Default] & NamedToken["ones"]
    Lexer.print: Token[LexerCtx.Default] & NamedToken["print"]
    Lexer.id: Token[LexerCtx.Default] & NamedToken["id"]
  }
}
