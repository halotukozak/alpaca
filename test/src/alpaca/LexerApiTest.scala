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
    Lexer.< : Token["<", LexerCtx.Default, Unit]
    Lexer.> : Token[">", LexerCtx.Default, Unit]
    Lexer.`=`: Token["=", LexerCtx.Default, Unit]
    Lexer.`\\+`: Token["\\+", LexerCtx.Default, Unit]
    Lexer.- : Token["-", LexerCtx.Default, Unit]
    Lexer.`\\*`: Token["\\*", LexerCtx.Default, Unit]
    Lexer.`/`: Token["/", LexerCtx.Default, Unit]
    Lexer.`\\(`: Token["\\(", LexerCtx.Default, Unit]
    Lexer.`\\)`: Token["\\)", LexerCtx.Default, Unit]
    Lexer.`\\[`: Token["\\[", LexerCtx.Default, Unit]
    Lexer.`\\]`: Token["\\]", LexerCtx.Default, Unit]
    Lexer.`\\{`: Token["\\{", LexerCtx.Default, Unit]
    Lexer.`\\}`: Token["\\}", LexerCtx.Default, Unit]
    Lexer.`:`: Token[":", LexerCtx.Default, Unit]
    Lexer.`'`: Token["'", LexerCtx.Default, Unit]
    Lexer.`,`: Token[",", LexerCtx.Default, Unit]
    Lexer.`;`: Token[";", LexerCtx.Default, Unit]
    Lexer.dotAdd: Token["dotAdd", LexerCtx.Default, Unit]
    Lexer.dotSub: Token["dotSub", LexerCtx.Default, Unit]
    Lexer.dotMul: Token["dotMul", LexerCtx.Default, Unit]
    Lexer.dotDiv: Token["dotDiv", LexerCtx.Default, Unit]
    Lexer.lessEqual: Token["lessEqual", LexerCtx.Default, Unit]
    Lexer.greaterEqual: Token["greaterEqual", LexerCtx.Default, Unit]
    Lexer.notEqual: Token["notEqual", LexerCtx.Default, Unit]
    Lexer.equal: Token["equal", LexerCtx.Default, Unit]
    Lexer.float: Token["float", LexerCtx.Default, Double]
    Lexer.int: Token["int", LexerCtx.Default, Int]
    Lexer.string: Token["string", LexerCtx.Default, String]
    Lexer.`if`: Token["if", LexerCtx.Default, Unit]
    Lexer.`else`: Token["else", LexerCtx.Default, Unit]
    Lexer.`for`: Token["for", LexerCtx.Default, Unit]
    Lexer.`while`: Token["while", LexerCtx.Default, Unit]
    Lexer.break: Token["break", LexerCtx.Default, Unit]
    Lexer.continue: Token["continue", LexerCtx.Default, Unit]
    Lexer.`return`: Token["return", LexerCtx.Default, Unit]
    Lexer.eye: Token["eye", LexerCtx.Default, Unit]
    Lexer.zeros: Token["zeros", LexerCtx.Default, Unit]
    Lexer.ones: Token["ones", LexerCtx.Default, Unit]
    Lexer.print: Token["print", LexerCtx.Default, Unit]
    Lexer.id: Token["id", LexerCtx.Default, String]
  }

  test("Lexer manipulates context") {
    case class StateCtx(
      var text: CharSequence = "",
      var count: Int = 0,
    ) extends LexerCtx

    val Lexer = lexer[StateCtx]:
      case "inc" =>
        ctx.count += 1
        Token["inc"](ctx.count)
      case "check" =>
        Token["check"](ctx.count)
      case " " => Token.Ignored

    val (_, lexemes) = Lexer.tokenize("inc check inc inc check")
    lexemes.map(_.value) shouldBe List(1, 1, 2, 3, 3)
  }
}
