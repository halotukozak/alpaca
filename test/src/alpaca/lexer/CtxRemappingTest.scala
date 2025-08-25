package alpaca.lexer

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import alpaca.showAst

final class CtxRemappingTest extends AnyFunSuite with Matchers {

  test("remapping maps matched text to custom values using ctx.text") {
    val L = lexer {
      case "\\s+" => Token.Ignored["temp"]
      case x @ "[0-9]+" => Token["int"](x.toInt)
      case s @ "[a-z]+" => Token["id"](s.toUpperCase)
    }

    val res = L.tokenize("12 abc 7", NoCtx.create)

    res.map(_.tpe) shouldBe List("int", "id", "int")
    res.map(_.value) shouldBe List(12, "ABC", 7)
    // res.map(_.index) shouldBe List(0, 3, 7)
  }

  test("ctx manipulation influences error position after ignored token") {
    val L2 = lexer[DefaultCtx] {
      case "A" => Token["A"]
      case "!" =>
        ctx.position += 5
        Token.Ignored["temp"]
      case x @ "\n+" =>
        ctx.position += x.count(_ == '\n')
        Token.Ignored["temp"]
    }

    val ex = intercept[RuntimeException] {
      L2.tokenize("A!?", DefaultCtx.create)
    }

    ex.getMessage should include("Unexpected character at position 7")
  }
}
