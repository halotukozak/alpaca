package alpaca.parser

import alpaca.lexer.*
import alpaca.parser.context.GlobalCtx
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import alpaca.lexer.context.Lexem
import org.scalatest.Assertions.assertDoesNotCompile
import scala.compiletime.testing.typeCheckErrors
import org.scalatest.LoneElement

final class ParseTableTest extends AnyFunSuite with Matchers with LoneElement {
  val CalcLexer = lexer {
    case "\\+" => Token["+"]
    case value @ "[1-9][0-9]*" => Token["Num"](value.toInt)
  }

  case class CalcContext() extends GlobalCtx

  test("parse table Shift-Reduce conflict") {
    object CalcParser extends Parser[CalcContext] {
      val Expr: Rule[Int] =
        case (Expr(expr1), CalcLexer.`+`(_), Expr(expr2)) => expr1 + expr2
        case CalcLexer.Num(lexem) => lexem.value

      val root: Rule[Int] =
        case Expr(expr) => expr
    }

    typeCheckErrors("CalcParser.parse[Int](Nil)").loneElement.message should include("""
      |Shift "+" vs Reduce Expr -> Expr+Expr
      |In situation like:
      |Expr + Expr + ...
      |Consider marking production Expr -> Expr+Expr to be alwaysBefore or alwaysAfter "+"
      |""".stripMargin)
  }

  test("parse table Reduce-Reduce conflict") {

    object CalcParser extends Parser[CalcContext] {
      val Integer: Rule[Int] =
        case CalcLexer.Num(lexem) => lexem.value

      val Float: Rule[Float] =
        case CalcLexer.Num(lexem) => lexem.value.toFloat

      val Expr: Rule[Any] =
        case Integer(value) => value
        case Float(value) => value

      val root: Rule[Any] =
        case Expr(expr) => expr
    }

    typeCheckErrors("CalcParser.parse[Any](Nil)").loneElement.message should include("""
      |Reduce Float -> Num vs Reduce Integer -> Num
      |In situation like:
      |Num ...
      |Consider marking one of the productions to be alwaysBefore or alwaysAfter the other
      |""".stripMargin)
  }
}
