package alpaca
package internal
package lexer
package regex

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

final class CharSetTest extends AnyFunSuite with Matchers:

  test("contains decides membership") {
    val s = CharSet.range('a', 'z')
    s.contains('a'.toInt) shouldBe true
    s.contains('z'.toInt) shouldBe true
    s.contains('m'.toInt) shouldBe true
    s.contains('A'.toInt) shouldBe false
    s.contains(('a' - 1).toInt) shouldBe false
  }

  test("union merges overlapping ranges") {
    val a = CharSet.range('a', 'm')
    val b = CharSet.range('h', 'z')
    val u = a.union(b)
    u.ranges shouldBe Vector(Range('a', 'z'))
  }

  test("union keeps disjoint ranges separate") {
    val a = CharSet.range('a', 'c')
    val b = CharSet.range('x', 'z')
    val u = a.union(b)
    u.ranges shouldBe Vector(Range('a', 'c'), Range('x', 'z'))
  }

  test("union merges adjacent ranges") {
    val a = CharSet.range('a', 'c')
    val b = CharSet.range('d', 'f')
    val u = a.union(b)
    u.ranges shouldBe Vector(Range('a', 'f'))
  }

  test("intersect computes overlap") {
    val a = CharSet.range('a', 'm')
    val b = CharSet.range('h', 'z')
    a.intersect(b).ranges shouldBe Vector(Range('h', 'm'))
  }

  test("intersect of disjoint is empty") {
    val a = CharSet.range('a', 'c')
    val b = CharSet.range('x', 'z')
    a.intersect(b).isEmpty shouldBe true
  }

  test("complement of all is empty, of empty is all") {
    CharSet.all.complement.isEmpty shouldBe true
    CharSet.empty.complement shouldBe CharSet.all
  }

  test("complement excludes original ranges") {
    val s = CharSet.range('a', 'z').complement
    s.contains('a'.toInt) shouldBe false
    s.contains('z'.toInt) shouldBe false
    s.contains('A'.toInt) shouldBe true
    s.contains(('a' - 1).toInt) shouldBe true
    s.contains(('z' + 1).toInt) shouldBe true
  }

  test("normalize merges overlapping and adjacent ranges") {
    val s = CharSet.normalize(Range('a', 'c'), Range('b', 'e'), Range('f', 'h'))
    s.ranges shouldBe Vector(Range('a', 'h'))
  }

  test("normalize sorts unsorted input") {
    val s = CharSet.normalize(Range('x', 'z'), Range('a', 'c'))
    s.ranges shouldBe Vector(Range('a', 'c'), Range('x', 'z'))
  }

  test("dotDefault excludes line terminators") {
    CharSet.dotDefault.contains('\n'.toInt) shouldBe false
    CharSet.dotDefault.contains('\r'.toInt) shouldBe false
    CharSet.dotDefault.contains(0x85) shouldBe false
    CharSet.dotDefault.contains(0x2028) shouldBe false
    CharSet.dotDefault.contains(0x2029) shouldBe false
    CharSet.dotDefault.contains('a'.toInt) shouldBe true
  }

  test("empty set contains nothing") {
    CharSet.empty.contains('a'.toInt) shouldBe false
    CharSet.empty.contains(0) shouldBe false
    CharSet.empty.isEmpty shouldBe true
  }

  test("all set contains every code point in range") {
    CharSet.all.contains(0) shouldBe true
    CharSet.all.contains('a'.toInt) shouldBe true
    CharSet.all.contains(CharSet.maxCodePoint) shouldBe true
  }

  test("union is commutative") {
    val a = CharSet.range('a', 'm')
    val b = CharSet.range('h', 'z')
    a.union(b) shouldBe b.union(a)
  }

  test("union with self is self") {
    val a = CharSet.range('a', 'z')
    a.union(a) shouldBe a
  }

  test("union with empty is self") {
    val a = CharSet.range('a', 'z')
    a.union(CharSet.empty) shouldBe a
    CharSet.empty.union(a) shouldBe a
  }

  test("intersect is commutative") {
    val a = CharSet.range('a', 'm')
    val b = CharSet.range('h', 'z')
    a.intersect(b) shouldBe b.intersect(a)
  }

  test("intersect with self is self") {
    val a = CharSet.range('a', 'z')
    a.intersect(a) shouldBe a
  }

  test("intersect with empty is empty") {
    val a = CharSet.range('a', 'z')
    a.intersect(CharSet.empty).isEmpty shouldBe true
    CharSet.empty.intersect(a).isEmpty shouldBe true
  }

  test("intersect with all is self") {
    val a = CharSet.range('a', 'z')
    a.intersect(CharSet.all) shouldBe a
  }

  test("double complement returns to original") {
    val a = CharSet.range('a', 'z')
    a.complement.complement shouldBe a
  }

  test("normalize merges multiple adjacent ranges") {
    val s = CharSet.normalize(
      Range('a', 'b'),
      Range('c', 'd'),
      Range('e', 'f'),
    )
    s.ranges shouldBe Vector(Range('a', 'f'))
  }

  test("normalize keeps non-overlapping ranges separate") {
    val s = CharSet.normalize(
      Range('a', 'c'),
      Range('f', 'h'),
      Range('m', 'p'),
    )
    s.ranges shouldBe Vector(
      Range('a', 'c'),
      Range('f', 'h'),
      Range('m', 'p'),
    )
  }

  test("normalize handles duplicate ranges") {
    val s = CharSet.normalize(Range('a', 'z'), Range('a', 'z'))
    s.ranges shouldBe Vector(Range('a', 'z'))
  }

  test("single from Int and Char agree") {
    CharSet.single('a') shouldBe CharSet.single('a'.toInt)
  }
