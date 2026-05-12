package alpaca
package internal
package lexer
package regex

import alpaca.internal.lexer.regex.Regex.*

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

final class SubsetTest extends AnyFunSuite with Matchers:

  private def s(pattern: String): Subset = Subset.parse(pattern) match
    case Right(sub) => sub
    case Left(err) => fail(s"expected successful parse of /$pattern/, got $err")
  private def s(r: Regex): Subset = Subset.of(r)

  test("identity: r ⊆ r") {
    s("a").subset(s("a")) shouldBe true
    s("[a-z]").subset(s("[a-z]")) shouldBe true
    s("if").subset(s("if")) shouldBe true
  }

  test("empty language is subset of anything") {
    s(Empty).subset(s("a")) shouldBe true
    s(Empty).subset(s(Empty)) shouldBe true
  }

  test("nothing nonempty is subset of empty") {
    s("a").subset(s(Empty)) shouldBe false
  }

  test("Eps ⊆ a*") {
    s(Eps).subset(s("a*")) shouldBe true
  }

  test("strict subset: a ⊆ [a-z]") {
    s("a").subset(s("[a-z]")) shouldBe true
    s("[a-z]").subset(s("a")) shouldBe false
  }

  test("prefix subset via .* extension") {
    s("if.*").subset(s("i.*")) shouldBe true
    s("i.*").subset(s("if.*")) shouldBe false
  }

  test("character class subset") {
    s("[a-c]").subset(s("[a-z]")) shouldBe true
    s("[a-z]").subset(s("[a-c]")) shouldBe false
  }

  test("alternation subset") {
    s("a").subset(s("a|b")) shouldBe true
    s("a|b").subset(s("a|b|c")) shouldBe true
    s("a|b|c").subset(s("a|b")) shouldBe false
  }

  test("Kleene star relationships") {
    s("a").subset(s("a*")) shouldBe true
    s("a*").subset(s(".*")) shouldBe true
  }

  test("isEmpty detects empty languages") {
    s(Empty).isEmpty shouldBe true
    s(Eps).isEmpty shouldBe false
    s(Regex(CharSet.empty)).isEmpty shouldBe true
    s("a").isEmpty shouldBe false
    s(s("a").underlying & s("b").underlying).isEmpty shouldBe true
    s(s("[a-z]").underlying & s("[A-Z]").underlying).isEmpty shouldBe true
    s(s("[a-m]").underlying & s("[h-z]").underlying).isEmpty shouldBe false
  }

  test("complement reverses subset") {
    val a = s("[a-z]")
    val notA = s(!a.underlying)
    a.subset(notA) shouldBe false
    s(a.underlying & notA.underlying).isEmpty shouldBe true
  }

  // nullable --------------------------------------------------------------

  test("nullable: Eps and Star are nullable") {
    s(Eps).nullable shouldBe true
    s("a*").nullable shouldBe true
  }

  test("nullable: non-empty literals are not nullable") {
    s("a").nullable shouldBe false
    s("abc").nullable shouldBe false
  }

  test("nullable: Empty is not nullable") {
    s(Empty).nullable shouldBe false
  }

  test("nullable: a? is nullable") {
    s("a?").nullable shouldBe true
  }

  test("nullable: alternation if any branch is") {
    s("a|b*").nullable shouldBe true
    s("a|b").nullable shouldBe false
  }

  test("nullable: concat requires both nullable") {
    s("a*b*").nullable shouldBe true
    s("a*b").nullable shouldBe false
  }

  // derive ----------------------------------------------------------------

  test("derive of literal 'a' wrt 'a' is Eps") {
    s("a").derive('a'.toInt).underlying shouldBe Eps
  }

  test("derive of literal 'a' wrt 'b' is Empty") {
    s("a").derive('b'.toInt).underlying shouldBe Empty
  }

  test("derive of 'ab' wrt 'a' yields 'b'") {
    s("ab").derive('a'.toInt).underlying shouldBe Regex.lit('b')
  }

  test("derive of 'a*' wrt 'a' is 'a*'") {
    s("a*").derive('a'.toInt).underlying shouldBe Regex.lit('a').star
  }

  test("derive of char class") {
    s("[a-z]").derive('m'.toInt).underlying shouldBe Eps
    s("[a-z]").derive('A'.toInt).underlying shouldBe Empty
  }

  // withAnySuffix ---------------------------------------------------------

  test("withAnySuffix accepts prefix matches") {
    val ext = s("if").withAnySuffix
    ext.derive('i'.toInt).derive('f'.toInt).nullable shouldBe true
    ext.derive('i'.toInt).derive('f'.toInt).derive('x'.toInt).nullable shouldBe true
  }

  test("withAnySuffix used in shadow check") {
    s("if").withAnySuffix.subset(s("i").withAnySuffix) shouldBe true
    s("i").withAnySuffix.subset(s("if").withAnySuffix) shouldBe false
  }

  // parse / of constructors -----------------------------------------------

  test("Subset.parse and Subset.of compose") {
    s("abc").underlying shouldBe Regex.literal("abc")
    Subset.of(Eps).underlying shouldBe Eps
  }
