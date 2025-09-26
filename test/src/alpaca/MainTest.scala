package alpaca

import alpaca.lexer.{lexer, Token}
import alpaca.parser.Parser
import alpaca.parser.Rule
import org.scalatest.funsuite.AnyFunSuite

@main def main(): Unit = {
  val Lexer = lexer {
    case "\\s+" => Token.Ignored
    case "=" => Token["="]
    case "\\*" => Token["*"]
    case "[a-zA-Z_][a-zA-Z0-9_]*" => Token["ID"]
  }

  final case class Ast(name: String, children: Ast*)

  object Parser extends Parser {
    val S: Rule[Ast] =
      case R(r) => Ast("S", r)
      case (L(l), Lexer.`=`(_), R(r)) => Ast("S", l, r)

    val L: Rule[Ast] =
      case Lexer.ID(id) => Ast("L", Ast(s"id: $id"))
      case (Lexer.`*`(_), R(r)) => Ast("L", r)

    val R: Rule[Ast] =
      case L(l) => Ast("R", l)

    val root: Rule[Ast] =
      case S(s) => Ast("S'", s)
  }

  val tokens = Lexer.tokenize("*A = **B")
  val result = Parser.parse[Ast](tokens)
  println(result)
}

class MainTest extends AnyFunSuite:
  test("e2e main test") {
    main()
  }
