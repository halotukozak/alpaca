package alpaca

import scala.collection.mutable
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.nio.charset.Charset
import java.io.Reader
import scala.annotation.tailrec

final class LazyReader(private val reader: Reader, private var size: Long) extends CharSequence {
  private val buffer = mutable.ArrayDeque.empty[Char]
  private val chunk = new Array[Char](8192)

  def charAt(pos: Int): Char = {
    ensure(pos)
    buffer(pos)
  }

  def length: Int = math.min(Int.MaxValue, size).toInt

  def subSequence(start: Int, end: Int): CharSequence = {
    ensure(end-1)
    buffer.slice(start, end).mkString
  }

  def from(count: Int): LazyReader = {
    buffer.remove(0, count)
    size -= count
    this
  }

  def close(): Unit = reader.close()

  @tailrec
  private def ensure(pos: Int): Unit = {
    if pos >= buffer.length then
      val charsRead = reader.read(chunk)
      if charsRead == -1 then
        throw new IndexOutOfBoundsException(s"Position $pos is out of bounds for LazyReader of size $size")
      else
        buffer.appendAll(chunk.iterator.take(charsRead))
        ensure(pos)
  }
}

object LazyReader {
  def from(path: Path, charset: Charset = StandardCharsets.UTF_8): LazyReader = {
    val reader = Files.newBufferedReader(path, charset)
    val size = Files.size(path)
    LazyReader(reader, size)
  }
}
