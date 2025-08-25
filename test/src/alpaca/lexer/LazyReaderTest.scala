package alpaca.lexer

import org.scalatest.funsuite.AnyFunSuite
import java.io.StringReader
import java.nio.file.Files
import java.nio.charset.StandardCharsets

class LazyReaderTest extends AnyFunSuite {
  test("charAt should return correct character at position") {
    val reader = new StringReader("hello world")
    val lazyReader = new LazyReader(reader, 11)

    assert(lazyReader.charAt(0) == 'h')
    assert(lazyReader.charAt(4) == 'o')
    assert(lazyReader.charAt(6) == 'w')
    assert(lazyReader.charAt(10) == 'd')

    lazyReader.close()
  }

  test("charAt should throw IndexOutOfBoundsException for position beyond end") {
    val reader = new StringReader("hello")
    val lazyReader = new LazyReader(reader, 5)

    val exception = intercept[IndexOutOfBoundsException] {
      lazyReader.charAt(10)
    }
    assert(exception.getMessage.contains("Position 10 is out of bounds"))

    lazyReader.close()
  }

  test("length should return correct length") {
    val reader = new StringReader("hello world")
    val lazyReader = new LazyReader(reader, 11)

    assert(lazyReader.length == 11)

    lazyReader.close()
  }

  test("length should handle very large sizes by capping at Int.MaxValue") {
    val reader = new StringReader("test")
    val lazyReader = new LazyReader(reader, Long.MaxValue)

    assert(lazyReader.length == Int.MaxValue)

    lazyReader.close()
  }

  test("subSequence should return correct substring") {
    val reader = new StringReader("hello world")
    val lazyReader = new LazyReader(reader, 11)

    assert(lazyReader.subSequence(0, 5) == "hello")
    assert(lazyReader.subSequence(6, 11) == "world")
    assert(lazyReader.subSequence(0, 11) == "hello world")
    assert(lazyReader.subSequence(1, 4) == "ell")

    lazyReader.close()
  }

  test("from should remove characters from beginning and update size") {
    val reader = new StringReader("hello world")
    val lazyReader = new LazyReader(reader, 11)

    // First load some data
    lazyReader.charAt(10) // This ensures buffer is loaded

    // Remove first 6 characters
    lazyReader.from(6)

    assert(lazyReader.length == 5)
    assert(lazyReader.charAt(0) == 'w')
    assert(lazyReader.charAt(4) == 'd')

    lazyReader.close()
  }

  test("lazy loading should work correctly") {
    val reader = new StringReader("abcdefghijklmnopqrstuvwxyz")
    val lazyReader = new LazyReader(reader, 26)

    // Access characters at different positions to test lazy loading
    assert(lazyReader.charAt(25) == 'z')
    assert(lazyReader.charAt(0) == 'a')
    assert(lazyReader.charAt(12) == 'm')

    lazyReader.close()
  }

  test("LazyReader.from should create LazyReader from file path") {
    // Create a temporary file
    val tempFile = Files.createTempFile("test", ".txt")
    try {
      Files.write(tempFile, "test content for file reading.".getBytes(StandardCharsets.UTF_8))

      val lazyReader = LazyReader.from(tempFile)

      assert(lazyReader.length == 30)
      assert(lazyReader.charAt(0) == 't')
      assert(lazyReader.charAt(4) == ' ')
      assert(lazyReader.subSequence(0, 4) == "test")
      assert(lazyReader.subSequence(5, 12) == "content")

      lazyReader.close()
    } finally {
      Files.deleteIfExists(tempFile)
    }
  }

  test("LazyReader.from should handle different charsets") {
    val tempFile = Files.createTempFile("test", ".txt")
    try {
      val content = "café"
      Files.write(tempFile, content.getBytes(StandardCharsets.UTF_8))

      val lazyReader = LazyReader.from(tempFile, StandardCharsets.UTF_8)

      assert(lazyReader.length == 5) // 'café' is 5 bytes in UTF-8 due to é
      assert(lazyReader.charAt(0) == 'c')
      assert(lazyReader.charAt(3) == 'é')

      lazyReader.close()
    } finally {
      Files.deleteIfExists(tempFile)
    }
  }

  test("empty string should work correctly") {
    val reader = new StringReader("")
    val lazyReader = new LazyReader(reader, 0)

    assert(lazyReader.length == 0)
    assert(lazyReader.subSequence(0, 0) == "")

    val exception = intercept[IndexOutOfBoundsException] {
      lazyReader.charAt(0)
    }
    assert(exception.getMessage.contains("Position 0 is out of bounds"))

    lazyReader.close()
  }

  test("large text should be handled efficiently") {
    val largeText = "a" * 20000 // Larger than chunk size (8192)
    val reader = new StringReader(largeText)
    val lazyReader = new LazyReader(reader, 20000)

    assert(lazyReader.charAt(0) == 'a')
    assert(lazyReader.charAt(8191) == 'a')
    assert(lazyReader.charAt(8192) == 'a') // This should trigger a new chunk read
    assert(lazyReader.charAt(19999) == 'a')
    assert(lazyReader.length == 20000)

    lazyReader.close()
  }

  test("from should work with partially loaded buffer") {
    val text = "0123456789abcdefghij"
    val reader = new StringReader(text)
    val lazyReader = new LazyReader(reader, 20)

    // Load first part
    assert(lazyReader.charAt(5) == '5')

    // Remove first 3 characters
    lazyReader.from(3)

    assert(lazyReader.length == 17)
    assert(lazyReader.charAt(0) == '3') // Was at position 3, now at 0
    assert(lazyReader.charAt(2) == '5') // Was at position 5, now at 2

    lazyReader.close()
  }
}
