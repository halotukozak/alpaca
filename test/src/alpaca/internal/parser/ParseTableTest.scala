package alpaca
package internal.parser

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.LoneElement

import scala.compiletime.testing.typeCheckErrors

final class ParseTableTest extends AnyFunSuite with Matchers with LoneElement {
  val CalcLexer = lexer {
    case "\\+" => Token["+"]
    case value @ "[1-9][0-9]*" => Token["Num"](value.toInt)
  }

  case class CalcContext() extends ParserCtx

  test("parse table Shift-Reduce conflict") {
    typeCheckErrors("""
    object CalcParser extends Parser[CalcContext] {
      val Expr: Rule[Int] = rule(
        { case (Expr(expr1), CalcLexer.`+`(_), Expr(expr2)) => expr1 + expr2 },
        { case CalcLexer.Num(lexem) => lexem.value },
      )

      val root = rule { case Expr(expr) => expr }
    }""").loneElement.message should
      include("""
                |Shift "+ ($plus)" vs Reduce Expr -> Expr + ($plus) Expr
                |In situation like:
                |Expr + ($plus) Expr + ($plus) ...
                |Consider marking production Expr -> Expr + ($plus) Expr to be alwaysBefore or alwaysAfter "+ ($plus)"
                |""".stripMargin)
  }

  test("parse table Reduce-Reduce conflict") {
    typeCheckErrors("""
    object CalcParser extends Parser[CalcContext] {
      val Integer = rule { case CalcLexer.Num(lexem) => lexem.value }

      val Float = rule { case CalcLexer.Num(lexem) => lexem.value.toFloat }

      val Expr = rule[Any](
        { case Integer(value) => value },
        { case Float(value) => value },
      )

      val root = rule { case Expr(expr) => expr }
    }
    """).loneElement.message should
      include("""
                |Reduce Integer -> Num vs Reduce Float -> Num
                |In situation like:
                |Num ...
                |Consider marking one of the productions to be alwaysBefore or alwaysAfter the other
                |""".stripMargin)
  }
}
