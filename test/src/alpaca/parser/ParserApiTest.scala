package alpaca
package parser

import alpaca.core.*
import alpaca.lexer.*
import alpaca.lexer.context.Lexem
import alpaca.lexer.context.default.*
import alpaca.parser.context.GlobalCtx
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import alpaca.parser.Rule.*
import alpaca.parser.Rule.given

import scala.collection.mutable
import alpaca.lexer.context.ctx

final class ParserApiTest extends AnyFunSuite with Matchers {
  val CalcLexer = lexer {
    case " " => Token.Ignored
    case " \\t" => Token.Ignored
    case id @ "[a-zA-Z_][a-zA-Z0-9_]*" => Token["ID"](id)
    case "\\+" => Token["PLUS"]
    case "-" => Token["MINUS"]
    case "\\*" => Token["TIMES"]
    case "/" => Token["DIVIDE"]
    case "=" => Token["ASSIGN"]
    case "," => Token["COMMA"]
    case parenthesis @ ("\\(" | "\\)") => Token[parenthesis.type]
    case number @ "\\d+" => Token["NUMBER"](number.toInt)
    case "#.*" => Token.Ignored
    case newline @ "\n+" =>
      ctx.line += newline.count(_ == '\n')
      Token.Ignored
  }

  case class CalcContext(
    names: mutable.Map[String, Int] = mutable.Map.empty,
    errors: mutable.ListBuffer[(tpe: String, value: Any)] = mutable.ListBuffer.empty,
  ) extends GlobalCtx derives Copyable

  // class CalcParser(Parser):
  //    tokens = CalcLexer.tokens
  //
  //    precedence = (
  //        ('left', PLUS, MINUS),
  //        ('left', TIMES, DIVIDE),
  //        ('right', UMINUS),
  //        )

  object CalcParser extends Parser[CalcContext] {
    val Expr: Rule[Int] =
      case (Expr(expr1), CalcLexer.PLUS(_), Expr(expr2)) => expr1 + expr2
      case (Expr(expr1), CalcLexer.MINUS(_), Expr(expr2)) => expr1 - expr2
      case (Expr(expr1), CalcLexer.TIMES(_), Expr(expr2)) => expr1 * expr2
      case (Expr(expr1), CalcLexer.DIVIDE(_), Expr(expr2)) => expr1 / expr2
      case (CalcLexer.MINUS(_), Expr(expr)) => -expr
      case (CalcLexer.`\\(`(_), Expr(expr), CalcLexer.`\\)`(_)) => expr
      case CalcLexer.NUMBER(expr) => expr.value
      case CalcLexer.ID(id) =>
        ctx.names.getOrElse(
          id.value, {
            ctx.errors.append(("undefined", id)); 0
          },
        )

    val ArgList: Rule[List[Int]] =
      case (Expr(expr), CalcLexer.COMMA(_), ArgList(exprs)) => expr :: exprs
      case Expr(expr) => expr :: Nil

    val Statement: Rule[Unit | Int | List[Int] | (String, Option[List[Int]])] =
      case (CalcLexer.ID(id), CalcLexer.ASSIGN(_), Expr(expr)) =>
        ctx.names(id.value) = expr
      case (CalcLexer.ID(id), CalcLexer.`\\(`(_), ArgList.Option(argList), CalcLexer.`\\)`(_)) =>
        (id.value, argList)
      case Expr(expr) => expr

    val root: Rule[Unit | Int | List[Int] | (String, Option[List[Int]])] =
      case Statement(stmt) => stmt
  }

  test("basic recognition of various tokens and literals") {
    val lexems = CalcLexer.tokenize("a = 3 + 4 * (5 + 6)")

    // todo https://github.com/halotukozak/alpaca/pull/65
    // todo https://github.com/halotukozak/alpaca/pull/51
    // CalcParser.parse[R](lexems) should matchPattern:
    //   case (ctx: CalcContext, None) if ctx.names("a") == 47 =>

    val lexems2 = CalcLexer.tokenize("3 + 4 * (5 + 6)")

    // todo https://github.com/halotukozak/alpaca/pull/65
    // todo https://github.com/halotukozak/alpaca/pull/51
    // CalcParser.parse[R](lexems2) should matchPattern:
    //   case (_, Some(47)) =>
  }

  test("ebnf") {
    val lexems = CalcLexer.tokenize("a()")
    // todo https://github.com/halotukozak/alpaca/pull/65
    // todo https://github.com/halotukozak/alpaca/pull/51
    // CalcParser.parse[R](lexems) should matchPattern:
    //   case (_, Some(('a', None))) =>

    val lexems1 = CalcLexer.tokenize("a(2+3)")

    // todo https://github.com/halotukozak/alpaca/pull/65
    // todo https://github.com/halotukozak/alpaca/pull/51
    // CalcParser.parse[R](lexems1) should matchPattern:
    //   case (_, Some(('a', Some(Seq(5))))) =>

    val lexems2 = CalcLexer.tokenize("a(2+3,4+5)")

    // todo https://github.com/halotukozak/alpaca/pull/65
    // todo https://github.com/halotukozak/alpaca/pull/51
    // CalcParser.parse[R](lexems2) should matchPattern:
    //   case (_, Some(('a', Some(Seq(5, 9))))) =>
  }

  test("parse error") {
    val lexems = CalcLexer.tokenize("a 123 4 + 5")

    // todo https://github.com/halotukozak/alpaca/pull/65
    // todo https://github.com/halotukozak/alpaca/pull/51
    // CalcParser.parse[R](lexems) should matchPattern:
    //   case (ctx: CalcContext, Some(9)) if ctx.errors.toList == Seq(("NUMBER", 123)) =>
  }
}
