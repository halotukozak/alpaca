package alpaca
package parser

import alpaca.core.*
import alpaca.lexer.context.default.*
import alpaca.lexer.{context, ctx as lctx, *}
import alpaca.parser.context.GlobalCtx
import alpaca.parser.ctx as pctx
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.collection.mutable

val CalcLexer = lexer {
  case " \\t" => Token.Ignored["ignore"]
  case id @ "[a-zA-Z_][a-zA-Z0-9_]*" => Token["ID"](id)
  case "+" => Token["PLUS"]
  case "-" => Token["MINUS"]
  case "*" => Token["TIMES"]
  case "/" => Token["DIVIDE"]
  case "=" => Token["ASSIGN"]
  case "," => Token["COMMA"]
  case parenthesis @ ("(" | ")") => Token[parenthesis.type]
  case number @ "\\d+" => Token["NUMBER"](number.toInt)
  case "#.*" => Token.Ignored["ignoreComment"]
  case newline @ "\n+" =>
    lctx.line += newline.count(_ == '\n')
    Token.Ignored["newline"]
}

case class CalcContext(
  names: mutable.Map[String, Int] = mutable.Map.empty,
  errors: mutable.ListBuffer[String] = mutable.ListBuffer.empty,
) extends GlobalCtx derives Copyable

object CalcParser extends Parser[CalcContext] {
  def Statement = rule {
    case (CalcLexer.ID(id), CalcLexer.ASSIGN(()), Expr(expr)) =>
      pctx.names(id) = expr
    case (CalcLexer.ID(id), CalcLexer.`(`(()), arglist: Option[?], CalcLexer.`)`(())) =>
      (id, arglist)
    case (CalcLexer.ID(id), exprs: List[?]) =>
      id :: exprs
    case expr => expr
  }

  def Expr: Rule = rule {
    case (Expr(expr1), CalcLexer.PLUS(()), Expr(expr2)) => expr1 + expr2
    case (Expr(expr1), CalcLexer.MINUS(()), Expr(expr2)) => expr1 - expr2
    case (Expr(expr1), CalcLexer.TIMES(()), Expr(expr2)) => expr1 * expr2
    case (Expr(expr1), CalcLexer.DIVIDE(()), Expr(expr2)) => expr1 / expr2
    case (CalcLexer.MINUS(()), Expr(expr)) => -expr
    case (CalcLexer.`(`(()), Expr(expr), CalcLexer.`)`(())) => expr
    case Expr(CalcLexer.NUMBER(expr)) => expr
    case CalcLexer.ID(id) =>
      pctx.names.getOrElse(
        id,
        { pctx.errors.append(s"undefined variable: $id"); 0 },
      )
  }
}

//class CalcParser(Parser):
//    tokens = CalcLexer.tokens
//
//    precedence = (
//        ('left', PLUS, MINUS),
//        ('left', TIMES, DIVIDE),
//        ('right', UMINUS),
//        )

final class ParserApiTest extends AnyFunSuite with Matchers {

  test("basic recognition of various tokens and literals") {
    // todo: // https://github.com/halotukozak/alpaca/issues/51
    // val tokens = CalcLexer.tokenize("a = 3 + 4 * (5 + 6)")
    // val (ctx, result) = CalcParser.parse(tokens)
    // assert(result == null)
    // assert(result.names.get("a") == 47)

    // todo: // https://github.com/halotukozak/alpaca/issues/51
    // val tokens2 = CalcLexer.tokenize("3 + 4 * (5 + 6)")
    // val (_, result2) = CalcParser.parse(tokens2)
    // assert(result2 == 47)
  }

  test("ebnf") {

    // todo: // https://github.com/halotukozak/alpaca/issues/51
//    val tokens = CalcLexer.tokenize("a()")
//    val (_, result) = CalcParser.parse(tokens)
    // assert(result == ('a', None))

    // todo: // https://github.com/halotukozak/alpaca/issues/51
//    val tokens1 = CalcLexer.tokenize("a(2+3)")
//    val (_, result1) = CalcParser.parse(tokens1)
    // assert(result1 == ('a', Seq(5)))

    // todo: // https://github.com/halotukozak/alpaca/issues/51
//    val tokens2 = CalcLexer.tokenize("a(2+3, 4+5)")
//    val (_, result2) = CalcParser.parse(tokens2)
    // assert(result2 == ('a', Seq(5, 9)))
  }

  test("parse error") {
    // todo: // https://github.com/halotukozak/alpaca/issues/51
//    val tokens = CalcLexer.tokenize("a 123 4 + 5")
//    val (_, result) = CalcParser.parse(tokens)
    // assert(result == 9)
    // assert((result.errors).size == 1)
    //  assert (result.errors[0].`type` == "NUMBER")
    //  assert (result.errors[0].value == 123)
    //
  }
  // # TO DO:  Add tests
  // # - error productions
  // # - embedded actions
  // # - lineno tracking
  // # - various error cases caught during parser construction
  //
}
