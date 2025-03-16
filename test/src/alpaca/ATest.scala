package alpaca

import org.scalatest.funsuite.AnyFunSuiteLike

class ATest extends AnyFunSuiteLike:

  test("hello") {
    val a = new A
    a.hello()
  }
