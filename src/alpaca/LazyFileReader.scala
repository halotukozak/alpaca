package alpaca

import scala.collection.mutable.ArrayDeque
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.io.{Reader, StringReader}
import scala.annotation.tailrec

final class LazyFileReader(private val reader: Reader, private var size: Int) extends CharSequence {
  private val buffer = ArrayDeque[Char]()
  private val chunk = new Array[Char](8192)

  def charAt(pos: Int): Char = {
    ensure(pos)
    buffer(pos)
  }

  def length: Int = size

  def subSequence(start: Int, end: Int): CharSequence = buffer.slice(start, end).mkString

  def from(count: Int): LazyFileReader = {
    buffer.remove(0, count)
    size -= count
    this
  }

  @tailrec
  private def ensure(pos: Int): Unit = {
    if pos >= buffer.length then
      val charsRead = reader.read(chunk)
      if charsRead == -1 then
        throw new IndexOutOfBoundsException(s"Position $pos is out of bounds for LazyFileReader of size $size")
      else
        buffer.appendAll(chunk.iterator.take(charsRead))
        ensure(pos)
  }
}

object LazyFileReader {
  def fromFile(path: Path): LazyFileReader = {
    val reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)
    val size = Files.size(path).toInt
    LazyFileReader(reader, size)
  }

  def fromString(data: String): LazyFileReader = {
    val reader = new StringReader(data)
    val size = data.length()
    LazyFileReader(reader, size)
  }
}
