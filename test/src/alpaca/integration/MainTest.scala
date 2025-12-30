package alpaca
package integration

import org.scalatest.funsuite.AnyFunSuite

@main def main(): Unit =
  val str = "(?<PLUS>\\+)|(?<MINUS>-)|(?<NUMBER>\\d+)"
  val x = 1

class MainTest extends AnyFunSuite:
  test("e2e main test") {
    main()
  }
