package alpaca.core

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

final class NonEmptyListTest extends AnyFunSuite with Matchers {

  test("NonEmptyList.apply creates a non-empty list and supports List operations") {
    val nel = NonEmptyList(1, 2, 3)

    nel.head shouldBe 1
    nel.tail shouldBe List(2, 3)
    nel shouldBe List(1, 2, 3)
  }

  test("NonEmptyList.unapply extracts head and tail") {
    val NonEmptyList(h, t) = NonEmptyList("a", "b", "c").runtimeChecked
    h shouldBe "a"
    t shouldBe List("b", "c")
  }

  test("NonEmptyList.unsafe converts non-empty List and throws on empty List") {
    val ok = NonEmptyList.unsafe(List(10, 20))
    ok.head shouldBe 10

    val ex = intercept[IllegalArgumentException] {
      NonEmptyList.unsafe(Nil)
    }
    ex.getMessage should include("Empty list cannot be converted to NonEmptyList")
  }
}
