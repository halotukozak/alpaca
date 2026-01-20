package alpaca.internal

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import ox.flow.Flow

class FlowOpsTest extends AnyFunSuite with Matchers:

  test("asFlow should convert Iterable and Iterator") {
    val list = List(1, 2, 3)
    list.asFlow.runToList() shouldBe list

    val iterator = list.iterator
    iterator.asFlow.runToList() shouldBe list
  }

  test("head and headOption") {
    val flow = Flow.fromValues(1, 2, 3)
    flow.head shouldBe 1
    flow.headOption shouldBe Some(1)

    Flow.empty[Int].headOption shouldBe None
    assertThrows[NoSuchElementException](Flow.empty[Int].head)
  }

  test("collectFirst") {
    val flow = Flow.fromValues(1, 2, 3, 4)
    flow.collectFirst { case x if x % 2 == 0 => x * 10 } shouldBe Some(20)
    flow.collectFirst { case x if x > 10 => x } shouldBe None
  }

  test("filterNot") {
    val flow = Flow.fromValues(1, 2, 3, 4)
    flow.filterNot(_ % 2 == 0).runToList() shouldBe List(1, 3)
  }

  test("partition") {
    val flow = Flow.fromValues(1, 2, 3, 4, 5)
    val (evens, odds) = flow.partition(_ % 2 == 0)
    evens.runToList() shouldBe List(2, 4)
    odds.runToList() shouldBe List(1, 3, 5)
  }

  test("foldLeft") {
    val flow = Flow.fromValues(1, 2, 3, 4)
    flow.foldLeft(0)(_ + _) shouldBe 10
    flow.foldLeft("")(_ + _.toString) shouldBe "1234"
  }

  test("mkString") {
    val flow = Flow.fromValues("a", "b", "c")
    flow.mkString shouldBe "abc"
    flow.mkString(",") shouldBe "a,b,c"
    flow.mkString("[", ", ", "]") shouldBe "[a, b, c]"

    Flow.empty[String].mkString shouldBe ""
    Flow.empty[String].mkString("[", ", ", "]") shouldBe "[]"
  }

  test("reverse") {
    val flow = Flow.fromValues(1, 2, 3)
    flow.reverse.runToList() shouldBe List(3, 2, 1)

    Flow.empty[Int].reverse.runToList() shouldBe Nil
  }

  test("find") {
    val flow = Flow.fromValues(1, 2, 3, 4)
    flow.find(_ == 3) shouldBe Some(3)
    flow.find(_ == 5) shouldBe None
  }

  test("tail") {
    val flow = Flow.fromValues(1, 2, 3)
    flow.tail.runToList() shouldBe List(2, 3)
    Flow.fromValues(1).tail.runToList() shouldBe Nil
    Flow.empty[Int].tail.runToList() shouldBe Nil
  }

  test("unzip") {
    val flow = Flow.fromValues((1, "a"), (2, "b"), (3, "c"))
    val (ints, strings) = flow.unzip
    ints.runToList() shouldBe List(1, 2, 3)
    strings.runToList() shouldBe List("a", "b", "c")
  }

  test("unzip3") {
    val flow = Flow.fromValues((1, "a", true), (2, "b", false))
    val (i, s, b) = flow.unzip3
    i.runToList() shouldBe List(1, 2)
    s.runToList() shouldBe List("a", "b")
    b.runToList() shouldBe List(true, false)
  }

  test("prepend and append") {
    val flow = Flow.fromValues(2, 3)
    flow.prepend(1).runToList() shouldBe List(1, 2, 3)
    flow.append(4).runToList() shouldBe List(2, 3, 4)
  }

  test("tapFlow") {
    var tapped = false
    val flow = Flow.fromValues(1)
    val result = flow.tapFlow { f =>
      tapped = true
      f.runToList() shouldBe List(1)
    }
    result.runToList() shouldBe List(1)
    tapped shouldBe true
  }

  test("indices") {
    val flow = Flow.fromValues("a", "b", "c")
    flow.indices.runToList() shouldBe List(0L, 1L, 2L)
  }
