package alpaca
package internal.parser

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

final class Issue148ReproTest extends AnyFunSuite with Matchers:

  val MyLexer = lexer:
    case "T" => Token["T"]

  case class MyCtx() extends ParserCtx

  test("Parser should support multiline actions") {
    object MyParser extends Parser[MyCtx]:
      override val root: Rule[Any] = rule:
        case MyLexer.T(_) =>
          val x = 1
          val y = 2
          x + y

    val (_, lexemes) = MyLexer.tokenize("T")
    val (_, result) = MyParser.parse(lexemes)
    result shouldBe 3
  }
