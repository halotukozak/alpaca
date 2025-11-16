package alpaca
package internal

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

final class ShowableTest extends AnyFunSuite with Matchers {

  test("Showable should convert String to Shown") {
    val str = "Hello, World!"
    val shown: String = show"$str"

    assert(shown == "Hello, World!")
  }

  test("Showable should convert Int to Shown") {
    val number: Int = 42
    val shown: String = show"$number"

    assert(shown == "42")
  }

  test("Showable should convert custom case class to Shown") {
    case class Person(name: String, age: Int)
    given Showable[Person] = person => s"${person.name} is ${person.age} years old."

    val person = Person("Alice", 30)
    val shown: String = show"$person"

    assert(shown == "Alice is 30 years old.")
  }

  test("Showable should convert derived types") {
    case class Address(city: String, zip: Int) derives Showable

    val address = Address("Wonderland", 12345)
    val shown: String = show"$address"

    assert(shown == "Address(city: Wonderland, zip: 12345)")
  }

  test("Showable should not convert unsupported types") {
    """|
       |import Showable._
       |class UnsupportedType(val value: String)
       |
       |val unsupported: UnsupportedType = new UnsupportedType("Unsupported")
       |val shown: String = show"$unsupported"
       |""".stripMargin shouldNot compile
  }
}
