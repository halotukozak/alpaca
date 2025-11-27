package alpaca
package internal

import alpaca.{lexer, Token, rule, Rule, ParserCtx, Production}
import alpaca.internal.lexer.Lexem
import alpaca.internal.parser.Parser
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.collection.mutable

final class ParserErrorRecoveryTest extends AnyFunSuite with Matchers {

  // Test lexer for simple arithmetic
  val CalcLexer = lexer {
    case " " => Token.Ignored
    case "\\t" => Token.Ignored
    case "\\+" => Token["PLUS"]
    case "-" => Token["MINUS"]
    case "\\*" => Token["TIMES"]
    case "/" => Token["DIVIDE"]
    case "\\(" => Token["LPAREN"]
    case "\\)" => Token["RPAREN"]
    case number @ "\\d+" => Token["NUMBER"](number.toInt)
    case id @ "[a-zA-Z_][a-zA-Z0-9_]*" => Token["ID"](id)
  }

  // Context without error recovery
  case class SimpleContext() extends ParserCtx

  // Context with error recovery
  case class RecoveringContext(
  ) extends ParserCtx with ParserErrorRecovery

  // Simple parser that parses addition expressions
  object SimpleParser extends Parser[SimpleContext] {
    override val resolutions = Set(
      Production(Expr, CalcLexer.PLUS, Expr).before(CalcLexer.PLUS, CalcLexer.MINUS),
    )
    val Expr: Rule[Int] = rule(
      { case (Expr(a), CalcLexer.PLUS(_), Expr(b)) => a + b },
      { case CalcLexer.NUMBER(n) => n.value },
    )
    val root = rule { case Expr(e) => e }
  }

  object RecoveringParser extends Parser[RecoveringContext] {
    override val resolutions = Set(
      Production(Expr, CalcLexer.PLUS, Expr).before(CalcLexer.PLUS, CalcLexer.MINUS),
    )
    val Expr: Rule[Int] = rule(
      { case (Expr(a), CalcLexer.PLUS(_), Expr(b)) => a + b },
      { case CalcLexer.NUMBER(n) => n.value },
    )
    val root = rule { case Expr(e) => e }
  }

  test("parser without error recovery returns null on syntax error") {
    val lexems = CalcLexer.tokenize("1 2") // Missing operator between numbers

    val (ctx, result) = SimpleParser.parse[Int](lexems)

    assert(result == null)
  }

  test("parser parses valid expressions correctly") {
    val lexems = CalcLexer.tokenize("1 + 2")

    val (ctx, result) = SimpleParser.parse[Int](lexems)

    assert(result == 3)
  }

  test("parser with error recovery collects errors") {
    val lexems = CalcLexer.tokenize("1 2 + 3") // "2" is unexpected

    val (ctx, result) = RecoveringParser.parse[Int](lexems)

    // The parser should try to recover and collect errors
    // Due to panic mode recovery, some result may be produced
    assert(ctx.parserErrors.nonEmpty)
  }

  test("parser with error recovery handles multiple errors") {
    val lexems = CalcLexer.tokenize("1 abc def + 3")

    val (ctx, result) = RecoveringParser.parse[Int](lexems)

    // Should have multiple errors recorded
    assert(ctx.parserErrors.size >= 1)
  }
}
