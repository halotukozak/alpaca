package alpaca

import Production as P

import alpaca.internal.Copyable
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import alpaca.internal.lexer.Lexeme
import alpaca.internal.lexer.LexerRefinement

import scala.deriving.Mirror

import scala.collection.mutable

final class ParserApiTest extends AnyFunSuite with Matchers:
  type R = Unit | Int | List[Int] | (String, Option[List[Int]])

  val CalcLexer = lexer {
    case " " => Token.Ignored
    case "\\t" => Token.Ignored
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
    errors: mutable.ListBuffer[(tpe: String, value: Any, line: Int)] = mutable.ListBuffer.empty,
  ) extends ParserCtx derives Copyable

  object CalcParser extends Parser[CalcContext]:
    val Expr: Rule[Int] = rule(
      { case (Expr(expr1), CalcLexer.PLUS(_), Expr(expr2)) => expr1 + expr2 }: @name("plus"),
      { case (Expr(expr1), CalcLexer.MINUS(_), Expr(expr2)) => expr1 - expr2 }: @name("minus"),
      { case (Expr(expr1), CalcLexer.TIMES(_), Expr(expr2)) => expr1 * expr2 },
      { case (Expr(expr1), CalcLexer.DIVIDE(_), Expr(expr2)) => expr1 / expr2 },
      { case (CalcLexer.MINUS(_), Expr(expr)) => -expr },
      { case (CalcLexer.`\\(`(_), Expr(expr), CalcLexer.`\\)`(_)) => expr },
      { case CalcLexer.NUMBER(expr) => expr.value },
      { case CalcLexer.ID(id) =>
        ctx.names.getOrElse(
          id.value, {
            ctx.errors.append(("undefined", id, id.line));
            0
          },
        )
      },
    )
    val ArgList: Rule[List[Int]] = rule(
      { case (Expr(expr), CalcLexer.COMMA(_), ArgList(exprs)) => expr :: exprs },
      { case Expr(expr) => expr :: Nil },
    )
    val Statement: Rule[R] = rule(
      { case (CalcLexer.ID(id), CalcLexer.ASSIGN(_), Expr(expr)) => ctx.names(id.value) = expr },
      { case (CalcLexer.ID(id), CalcLexer.`\\(`(_), ArgList.Option(argList), CalcLexer.`\\)`(_)) =>
        (id.value, argList)
      },
      { case Expr(expr) => expr },
    )
    val root = rule { case Statement(stmt) => stmt }

    override val resolutions = Set(
      P(CalcLexer.MINUS, Expr).before(CalcLexer.DIVIDE, CalcLexer.TIMES, CalcLexer.PLUS, CalcLexer.MINUS),
      P(Expr, CalcLexer.DIVIDE, Expr).before(CalcLexer.DIVIDE, CalcLexer.TIMES, CalcLexer.PLUS, CalcLexer.MINUS),
      P(Expr, CalcLexer.TIMES, Expr).before(CalcLexer.DIVIDE, CalcLexer.TIMES, CalcLexer.PLUS, CalcLexer.MINUS),
      P.ofName("plus").before(CalcLexer.PLUS, CalcLexer.MINUS),
      P.ofName("plus").after(CalcLexer.TIMES, CalcLexer.DIVIDE),
      P.ofName("minus").before(CalcLexer.PLUS, CalcLexer.MINUS),
      P.ofName("minus").after(CalcLexer.TIMES, CalcLexer.DIVIDE),
    )

  test("basic recognition of various tokens and literals") {
    CalcParser.parse(CalcLexer.tokenize("a = 3 + 4 * (5 + 6)").lexemes) should matchPattern:
      case (ctx: CalcContext, _) if ctx.names("a") == 47 =>

    CalcParser.parse(CalcLexer.tokenize("3 + 4 * (5 + 6)").lexemes) should matchPattern:
      case (_, 47) =>
  }

  test("ebnf") {
    CalcParser.parse(CalcLexer.tokenize("a()").lexemes) should matchPattern:
      case (_, ("a", None)) =>

    CalcParser.parse(CalcLexer.tokenize("a(2+3)").lexemes) should matchPattern:
      case (_, ("a", Some(Seq(5)))) =>

    CalcParser.parse(CalcLexer.tokenize("a(2+3,4+5)").lexemes) should matchPattern:
      case (_, ("a", Some(Seq(5, 9)))) =>
  }

  test("api") {
    type R = (Int, Option[Int], List[Int])
    object ApiParser extends Parser[CalcContext]:
      val Num = rule { case CalcLexer.NUMBER(n) => n.value }

      val root = rule { case (Num(n), CalcLexer.COMMA(_), Num.Option(numOpt), CalcLexer.COMMA(_), Num.List(numList)) =>
        (n, numOpt, numList)
      }

    ApiParser.parse(CalcLexer.tokenize("1,,").lexemes) should matchPattern:
      case (_, (1, None, Nil)) =>

    ApiParser.parse(CalcLexer.tokenize("1,2,").lexemes) should matchPattern:
      case (_, (1, Some(2), Nil)) =>

    ApiParser.parse(CalcLexer.tokenize("1,2,1 2 3").lexemes) should matchPattern:
      case (_, (1, Some(2), List(1, 2, 3))) =>

    ApiParser.parse(CalcLexer.tokenize("1,,3").lexemes) should matchPattern:
      case (_, (1, None, List(3))) =>
  }

  test("parse error") {
    val lexems = CalcLexer.tokenize("a 123 4 + 5").lexemes

    // todo https://github.com/halotukozak/alpaca/pull/65
    // todo https://github.com/halotukozak/alpaca/pull/51
    // CalcParser.parse[R](lexems) should matchPattern:
    //   case (ctx: CalcContext, Some(9)) if ctx.errors.toList == Seq(("NUMBER", 123)) =>
  }
