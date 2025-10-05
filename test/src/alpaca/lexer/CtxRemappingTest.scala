package alpaca
package lexer

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import alpaca.lexer.context.ctx

final class CtxRemappingTest extends AnyFunSuite with Matchers {
  test("remapping maps matched text to custom values using ctx.text") {
    val L = lexer {
      case "\\s+" => Token.Ignored
      case x @ "[0-9]+" => Token["int"](x.toInt)
      case s @ "[a-z]+" => Token["id"](s.toUpperCase)
    }

    val res = L.tokenize("12 abc 7")
    res.map(_.name) shouldBe List("int", "id", "int")
    res.map(_.value) shouldBe List(12, "ABC", 7)
  }

  test("ctx manipulation influences error position after ignored token") {
    val L2 = lexer {
      case "A" => Token["A"]
      case "!" =>
        ctx.position += 5
        Token.Ignored
      case x @ "\n+" =>
        ctx.position += x.count(_ == '\n')
        Token.Ignored
    }

    // todo: https://github.com/halotukozak/alpaca/issues/51
    // val ex = intercept[RuntimeException] {
    //   L2.tokenize("A!?")
    // }

    // ex.getMessage should include("Unexpected character at position 7")
  }
}
