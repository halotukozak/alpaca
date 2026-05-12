package alpaca
package internal
package lexer
package regex

import alpaca.internal.lexer.regex.Regex.*

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

final class RegexAlgebraTest extends AnyFunSuite with Matchers:

  private val rA = Regex.lit('a')
  private val rB = Regex.lit('b')
  private val rC = Regex.lit('c')

  // concat ----------------------------------------------------------------

  test("concat with Empty annihilates") {
    (Empty.concat(rA)) shouldBe Empty
    (rA.concat(Empty)) shouldBe Empty
  }

  test("concat with Eps is identity") {
    (Eps.concat(rA)) shouldBe rA
    (rA.concat(Eps)) shouldBe rA
  }

  test("concat is right-associated") {
    rA.concat(rB).concat(rC) match
      case Concat(a, Concat(b, c)) => (a, b, c) shouldBe (rA, rB, rC)
      case other => fail(s"not right-associated: $other")
  }

  // alt -------------------------------------------------------------------

  test("alt with Empty drops it") {
    (Empty | rA) shouldBe rA
    (rA | Empty) shouldBe rA
  }

  test("alt deduplicates equal regexes") {
    (rA | rA) shouldBe rA
  }

  test("alt flattens nested Alt") {
    val sA = rA.star
    val sB = rB.star
    val sC = rC.star
    sA | sB | sC match
      case Alt(parts) => parts shouldBe Set(sA, sB, sC)
      case other => fail(s"not flattened Alt: $other")
  }

  test("alt merges Chars by union") {
    val merged = Regex.lit('a') | Regex.lit('b')
    merged shouldBe Regex.chars(CharSet.normalize(('a'.toInt, 'a'.toInt), ('b'.toInt, 'b'.toInt)))
  }

  // inter -----------------------------------------------------------------

  test("inter with Empty gives Empty") {
    (Empty & rA) shouldBe Empty
    (rA & Empty) shouldBe Empty
  }

  test("inter deduplicates equal regexes") {
    (rA & rA) shouldBe rA
  }

  test("inter intersects Chars") {
    val ab = Regex.range('a', 'm')
    val bc = Regex.range('h', 'z')
    (ab & bc) shouldBe Regex.range('h', 'm')
  }

  test("inter of disjoint Chars is Empty") {
    (Regex.lit('a') & Regex.lit('b')) shouldBe Empty
  }

  // star ------------------------------------------------------------------

  test("star of Eps is Eps") {
    Eps.star shouldBe Eps
  }

  test("star of Empty is Eps") {
    Empty.star shouldBe Eps
  }

  test("star of Star is the same Star") {
    val s = rA.star
    s.star shouldBe s
  }

  // compl -----------------------------------------------------------------

  test("compl is involutive") {
    !(!rA) shouldBe rA
  }

  // literal ---------------------------------------------------------------

  test("literal of empty string is Eps") {
    Regex.literal("") shouldBe Eps
  }

  test("literal of single char is Chars") {
    Regex.literal("a") shouldBe Regex.lit('a')
  }

  test("literal of two chars is right-associated Concat") {
    Regex.literal("ab") match
      case Concat(a, b) => (a, b) shouldBe (Regex.lit('a'), Regex.lit('b'))
      case other => fail(s"not Concat: $other")
  }

  // repeat ----------------------------------------------------------------

  test("repeat {0} is Eps") {
    rA.repeat(0, 0) shouldBe Eps
  }

  test("repeat {1,1} is the regex itself") {
    rA.repeat(1, 1) shouldBe rA
  }

  test("repeat {n, MaxValue} ends with Star") {
    rA.repeat(2, Int.MaxValue) shouldBe (rA.concat(rA).concat(rA.star))
  }

  test("repeat rejects invalid bounds") {
    an[IllegalArgumentException] shouldBe thrownBy(rA.repeat(-1, 0))
    an[IllegalArgumentException] shouldBe thrownBy(rA.repeat(3, 2))
  }
