package alpaca
package internal.lexer

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.io.StringReader
import scala.util.Using

final class LazyReaderTest extends AnyFunSuite:
  test("charAt should return correct character at position") {
    val reader = new StringReader("hello world")
    Using(new LazyReader(reader, 11)) { lazyReader =>
      assert(lazyReader.charAt(0) == 'h')
      assert(lazyReader.charAt(4) == 'o')
      assert(lazyReader.charAt(6) == 'w')
      assert(lazyReader.charAt(10) == 'd')
    }
  }

  test("charAt should throw IndexOutOfBoundsException for position beyond end") {
    val reader = new StringReader("hello")
    Using(new LazyReader(reader, 5)) { lazyReader =>
      val exception = intercept[IndexOutOfBoundsException] {
        lazyReader.charAt(10)
      }
      assert(exception.getMessage.contains("Position 10 is out of bounds"))
    }
  }

  test("length should return correct value") {
    val reader = new StringReader("hello world")
    Using(new LazyReader(reader, 11)) { lazyReader =>
      assert(lazyReader.length == 11)
    }
  }

  test("length should handle very large sizes by capping at Int.MaxValue") {
    val reader = new StringReader("test")
    Using(new LazyReader(reader, Long.MaxValue)) { lazyReader =>
      assert(lazyReader.length == Int.MaxValue)
    }
  }

  test("subSequence should return correct substring") {
    val reader = new StringReader("hello world")
    Using(new LazyReader(reader, 11)) { lazyReader =>
      assert(lazyReader.subSequence(0, 5) == "hello")
      assert(lazyReader.subSequence(6, 11) == "world")
      assert(lazyReader.subSequence(0, 11) == "hello world")
      assert(lazyReader.subSequence(1, 4) == "ell")
    }
  }

  test("from should remove characters from beginning and update length") {
    val reader = new StringReader("hello world")
    Using(new LazyReader(reader, 11)) { lazyReader =>
      lazyReader.from(6)

      assert(lazyReader.length == 5)
      assert(lazyReader.charAt(0) == 'w')
      assert(lazyReader.charAt(4) == 'd')
    }
  }

  test("LazyReader.from should create LazyReader from file path") {
    withLazyReader("test content for file reading.") { lazyReader =>
      assert(lazyReader.length == 30)
      assert(lazyReader.charAt(0) == 't')
      assert(lazyReader.charAt(4) == ' ')
      assert(lazyReader.subSequence(0, 4) == "test")
      assert(lazyReader.subSequence(5, 12) == "content")
    }
  }

  test("empty string should work correctly") {
    val reader = new StringReader("")
    Using(new LazyReader(reader, 0)) { lazyReader =>
      assert(lazyReader.length == 0)
      assert(lazyReader.subSequence(0, 0) == "")

      val exception = intercept[IndexOutOfBoundsException] {
        lazyReader.charAt(0)
      }
      assert(exception.getMessage.contains("Position 0 is out of bounds"))
    }
  }
