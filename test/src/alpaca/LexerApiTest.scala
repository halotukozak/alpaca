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
    Lexer.< : Token[LexerCtx.Default] { val info: { val name: "<" } }
    Lexer.> : Token[LexerCtx.Default] { val info: { val name: ">" } }
    Lexer.`=`: Token[LexerCtx.Default] { val info: { val name: "=" } }
    Lexer.`\\+`: Token[LexerCtx.Default] { val info: { val name: "\\+" } }
    Lexer.- : Token[LexerCtx.Default] { val info: { val name: "-" } }
    Lexer.`\\*`: Token[LexerCtx.Default] { val info: { val name: "\\*" } }
    Lexer.`/`: Token[LexerCtx.Default] { val info: { val name: "/" } }
    Lexer.`\\(`: Token[LexerCtx.Default] { val info: { val name: "\\(" } }
    Lexer.`\\)`: Token[LexerCtx.Default] { val info: { val name: "\\)" } }
    Lexer.`\\[`: Token[LexerCtx.Default] { val info: { val name: "\\[" } }
    Lexer.`\\]`: Token[LexerCtx.Default] { val info: { val name: "\\]" } }
    Lexer.`\\{`: Token[LexerCtx.Default] { val info: { val name: "\\{" } }
    Lexer.`\\}`: Token[LexerCtx.Default] { val info: { val name: "\\}" } }
    Lexer.`:`: Token[LexerCtx.Default] { val info: { val name: ":" } }
    Lexer.`'`: Token[LexerCtx.Default] { val info: { val name: "'" } }
    Lexer.`,`: Token[LexerCtx.Default] { val info: { val name: "," } }
    Lexer.`;`: Token[LexerCtx.Default] { val info: { val name: ";" } }
    Lexer.dotAdd: Token[LexerCtx.Default] { val info: { val name: "dotAdd" } }
    Lexer.dotSub: Token[LexerCtx.Default] { val info: { val name: "dotSub" } }
    Lexer.dotMul: Token[LexerCtx.Default] { val info: { val name: "dotMul" } }
    Lexer.dotDiv: Token[LexerCtx.Default] { val info: { val name: "dotDiv" } }
    Lexer.lessEqual: Token[LexerCtx.Default] { val info: { val name: "lessEqual" } }
    Lexer.greaterEqual: Token[LexerCtx.Default] { val info: { val name: "greaterEqual" } }
    Lexer.equal: Token[LexerCtx.Default] { val info: { val name: "equal" } }
    Lexer.notEqual: Token[LexerCtx.Default] { val info: { val name: "notEqual" } }
    Lexer.float: Token[LexerCtx.Default] { val info: { val name: "float" } }
    Lexer.int: Token[LexerCtx.Default] { val info: { val name: "int" } }
    Lexer.string: Token[LexerCtx.Default] { val info: { val name: "string" } }
    Lexer.`if`: Token[LexerCtx.Default] { val info: { val name: "if" } }
    Lexer.`else`: Token[LexerCtx.Default] { val info: { val name: "else" } }
    Lexer.`for`: Token[LexerCtx.Default] { val info: { val name: "for" } }
    Lexer.`while`: Token[LexerCtx.Default] { val info: { val name: "while" } }
    Lexer.break: Token[LexerCtx.Default] { val info: { val name: "break" } }
    Lexer.continue: Token[LexerCtx.Default] { val info: { val name: "continue" } }
    Lexer.`return`: Token[LexerCtx.Default] { val info: { val name: "return" } }
    Lexer.eye: Token[LexerCtx.Default] { val info: { val name: "eye" } }
    Lexer.zeros: Token[LexerCtx.Default] { val info: { val name: "zeros" } }
    Lexer.ones: Token[LexerCtx.Default] { val info: { val name: "ones" } }
    Lexer.print: Token[LexerCtx.Default] { val info: { val name: "print" } }
    Lexer.id: Token[LexerCtx.Default] { val info: { val name: "id" } }
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
