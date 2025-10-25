package alpaca.core

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import alpaca.core.Csv.toCsv

final class CsvTest extends AnyFunSuite with Matchers {

  test("Csv Showable should format headers and rows with commas and newlines") {
    val csv = Csv(
      headers = List("Name", "Age"),
      rows = List(
        List("Alice", 30),
        List("Bob", 41),
      ),
    )

    val shown: String = show"$csv"
    shown shouldBe "Name,Age\nAlice,30\nBob,41"
  }

  test("toCsv should convert a list of named tuples into a Csv with proper headers and values") {
    val rows = List(
      (name = "Alice", age = 30),
      (name = "Bob", age = 41),
    )

    val csv = rows.toCsv
    val shown: String = show"$csv"

    shown shouldBe "name,age\nAlice,30\nBob,41"
  }

  test("Csv Showable should handle empty rows (headers only)") {
    val csv = Csv(headers = List("OnlyHeader"), rows = Nil)
    val shown: String = show"$csv"

    shown shouldBe "OnlyHeader\n"
  }
}
