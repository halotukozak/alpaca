package alpaca

import alpaca.lexer.{Token, lexer}
import alpaca.parser.{Parser, Rule, rule}
import org.scalatest.funsuite.AnyFunSuite
@main def main(): Unit = {
  val Lexer = lexer {
    case "\\s+" => Token.Ignored
    case "=" => Token["="]
    case "\\*" => Token["*"]
    case "[a-zA-Z_][a-zA-Z0-9_]*" => Token["ID"]
  }

  object Parser extends Parser {
    lazy val `S'`: Rule = rule { case S(_) => ??? }

    lazy val S: Rule = rule {
      case R(_) => ???
      case (L(_), Lexer.`=`(_), R(_)) => ???
    }

    lazy val L: Rule = rule {
      case Lexer.ID(_) => ???
      case (Lexer.`*`(_), R(_)) => ???
    }

    lazy val R: Rule = rule { case L(_) => ??? }
  }

  val tokens = Lexer.tokenize("*A = **B")
  val result = Parser.parse[Any](tokens)
  println(result)
}

class MainTest extends AnyFunSuite:
  test("e2e main test") {
    main()
  }
