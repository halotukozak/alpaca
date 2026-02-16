package alpaca
package internal.parser

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

final class Issue198FixTest extends AnyFunSuite with Matchers:

  val MyLexer = lexer:
    case "if" => Token["IF"]
    case "else" => Token["ELSE"]
    case value @ "[1-9][0-9]*" => Token["Num"](value.toInt)

  case class MyCtx() extends ParserCtx

  test("hyphenated production name should compile") {
    object MyParser extends Parser[MyCtx]:
      val root = rule:
        case Expr(e) => e

      val Expr: Rule[Int] = rule(
        "if-else" { case (MyLexer.IF(_), MyLexer.Num(n), MyLexer.ELSE(_)) => n.value },
        { case MyLexer.Num(n) => n.value }
      )

      override val resolutions = Set(
        production.`if$minuselse`.after(MyLexer.Num)
      )
    
    assert(MyParser.resolutions.nonEmpty)
  }
