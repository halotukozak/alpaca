package alpaca.lexer

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

final class LexerApiTest extends AnyFunSuite with Matchers {
  val Lexer = lexer {
    case literal @ ("<" | ">" | "=" | "\\+" | "-" | "\\*" | "/" | "\\(" | "\\)" | "\\[" | "\\]" | "{" | "}" | ":" | "'" | "," | ";") => Token[literal.type]
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
    // todo: should be in order of definition // https://github.com/halotukozak/alpaca/issues/51
//    Lexer.tokens.map(_.pattern) shouldBe List(
    //format: off
    // https://github.com/halotukozak/alpaca/issues/51
    // Lexer.tokens.map(_.pattern) should contain theSameElementsAs List(
    //   "#.*",
    //   "<", ">", "=", "+", "-", "*", "/", "(", ")", "[", "]", "{", "}", ":", "'", ",", ";",
    //   raw"\.\+",
    //   raw"\.\-",
    //   raw"\.\*",
    //   raw"\.\/",
    //   "<=",
    //   ">=",
    //   "!=",
    //   "==",
    //   raw"(d+(\.\d*)|\.\d+)([eE][+-]?\d+)?",
    //   "[0-9]+",
    //   "[^\"]*",
    //   "if", "else", "for", "while", "break", "continue", "return", "eye", "zeros", "ones", "print",
    //   "[a-zA-Z_][a-zA-Z0-9_]*",
    // )
    //format: on

    // we check if compiles and not crashes
    Lexer.<
    Lexer.>
    Lexer.`=`
    Lexer.`\\+`
    Lexer.-
    Lexer.`\\*`
    Lexer./
    Lexer.`\\(`
    Lexer.`\\)`
    Lexer.`\\[`
    Lexer.`\\]`
    Lexer.`{`
    Lexer.`}`
    Lexer.`:`
    Lexer.`'`
    Lexer.`,`
    Lexer.`;`
    Lexer.dotAdd
    Lexer.dotSub
    Lexer.dotMul
    Lexer.dotDiv
    Lexer.lessEqual
    Lexer.greaterEqual
    Lexer.notEqual
    Lexer.equal
    Lexer.float
    Lexer.int
    Lexer.string
    Lexer.`if`
    Lexer.`else`
    Lexer.`for`
    Lexer.`while`
    Lexer.break
    Lexer.continue
    Lexer.`return`
    Lexer.eye
    Lexer.zeros
    Lexer.ones
    Lexer.print
    Lexer.id
  }
}
