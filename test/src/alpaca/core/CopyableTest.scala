package alpaca.core

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.compiletime.testing.typeChecks

final class CopyableTest extends AnyFunSuite with Matchers {

  inline def copy[T]: Copyable[T] = compiletime.summonInline[Copyable[T]]

  case class Person(name: String, age: Int) derives Copyable

  case class Address(city: String, zip: Int) derives Copyable

  case class User(person: Person, address: Address, tags: List[String], opt: Option[Int], tuple: (Int, String))
    derives Copyable

  case class Box[T](value: T) derives Copyable

  case class Defaults(a: Int = 1, b: String = "x") derives Copyable

  case class Empty() derives Copyable

  test("derived produces an identity-like function for a simple case class") {
    val p = Person("Ann", 30)
    copy(p) shouldEqual p
  }

  test("derived works with nested case classes, options, lists and tuples") {
    val u = User(
      person = Person("Bob", 25),
      address = Address("NYC", 10001),
      tags = List("a", "b"),
      opt = Some(7),
      tuple = (42, "answer"),
    )
    copy(u) shouldEqual u
  }

  test("derived works for a generic case class instantiation") {
    val bx = Box(Person("Cara", 22))
    copy(bx) shouldEqual bx
  }

  test("derived handles default parameters and zero-arity case class") {
    val d = Defaults()
    val e = Empty()

    copy(d) shouldEqual d
    copy(e) shouldEqual e
  }

  test("cannot derive Copyable for non-case classes or non-product types (compile-time)") {
    // String is not a case class => should not typecheck
    typeChecks("import alpaca.core.Copyable; val c = Copyable.derived[String]") shouldBe false

    // Define a plain (non-case) class and try to derive should fail
    """
      |import alpaca.core.Copyable
      |class Regular(val x: Int)
      |val c = Copyable.derived[Regular]
      |""".stripMargin shouldNot compile
  }
}
