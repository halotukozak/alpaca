package alpaca.lexer

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import alpaca.lexer.context.default.DefaultGlobalCtx
import scala.annotation.nowarn

@nowarn("msg=A pure expression")
final class LexerApiTest extends AnyFunSuite with Matchers {
  val Lexer = lexer {
    case literal @ ("<" | ">" | "=" | "\\+" | "-" | "\\*" | "/" | "\\(" | "\\)" | "\\[" | "\\]" | "{" | "}" | ":" |
        "'" | "," | ";") =>
      Token[literal.type]
    case "\\.\\+" => Token["dotAdd"]
    case "\\.\\-" => Token["dotSub"]
    case "\\.\\*" => Token["dotMul"]
    case "\\.\\/" => Token["dotDiv"]
    case "<=" => Token["lessEqual"]
    case comment @ "#.*" => Token.Ignored[comment.type]
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
  }

  test("Lexer recognizes basic tokens") {
    Lexer.tokens.map(_.pattern) shouldBe List(
    //format: off
      "#.*",
      "<", ">", "=", "\\+", "-", "\\*", "/", "\\(", "\\)", "\\[", "\\]", "{", "}", ":", "'", ",", ";",
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
    )
    //format: on
    type Ctx = DefaultGlobalCtx

    // we check if compiles and not crashes
    Lexer.< : Token["<", Ctx, Unit]
    Lexer.> : Token[">", Ctx, Unit]
    Lexer.`=`: Token["=", Ctx, Unit]
    Lexer.`\\+`: Token["\\+", Ctx, Unit]
    Lexer.- : Token["-", Ctx, Unit]
    Lexer.`\\*`: Token["\\*", Ctx, Unit]
    Lexer.`/`: Token["/", Ctx, Unit]
    Lexer.`\\(`: Token["\\(", Ctx, Unit]
    Lexer.`\\)`: Token["\\)", Ctx, Unit]
    Lexer.`\\[`: Token["\\[", Ctx, Unit]
    Lexer.`\\]`: Token["\\]", Ctx, Unit]
    Lexer.`{`: Token["{", Ctx, Unit]
    Lexer.`}`: Token["}", Ctx, Unit]
    Lexer.`:`: Token[":", Ctx, Unit]
    Lexer.`'`: Token["'", Ctx, Unit]
    Lexer.`,`: Token[",", Ctx, Unit]
    Lexer.`;`: Token[";", Ctx, Unit]
    Lexer.dotAdd: Token["dotAdd", Ctx, Unit]
    Lexer.dotSub: Token["dotSub", Ctx, Unit]
    Lexer.dotMul: Token["dotMul", Ctx, Unit]
    Lexer.dotDiv: Token["dotDiv", Ctx, Unit]
    Lexer.lessEqual: Token["lessEqual", Ctx, Unit]
    Lexer.greaterEqual: Token["greaterEqual", Ctx, Unit]
    Lexer.notEqual: Token["notEqual", Ctx, Unit]
    Lexer.equal: Token["equal", Ctx, Unit]
    Lexer.float: Token["float", Ctx, Double]
    Lexer.int: Token["int", Ctx, Int]
    Lexer.string: Token["string", Ctx, String]
    Lexer.`if`: Token["if", Ctx, Unit]
    Lexer.`else`: Token["else", Ctx, Unit]
    Lexer.`for`: Token["for", Ctx, Unit]
    Lexer.`while`: Token["while", Ctx, Unit]
    Lexer.break: Token["break", Ctx, Unit]
    Lexer.continue: Token["continue", Ctx, Unit]
    Lexer.`return`: Token["return", Ctx, Unit]
    Lexer.eye: Token["eye", Ctx, Unit]
    Lexer.zeros: Token["zeros", Ctx, Unit]
    Lexer.ones: Token["ones", Ctx, Unit]
    Lexer.print: Token["print", Ctx, Unit]
    Lexer.id: Token["id", Ctx, String]
  }
}
