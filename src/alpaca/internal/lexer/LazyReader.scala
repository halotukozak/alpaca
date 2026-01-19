package alpaca
package internal
package lexer

import java.io.{Closeable, Reader}
import java.nio.charset.{Charset, StandardCharsets}
import java.nio.file.{Files, Path}
import scala.annotation.tailrec
import scala.collection.mutable

/**
 * A lazy character sequence that reads from a Reader on demand.
 *
 * This class is useful for tokenizing large files without loading them
 * entirely into memory. It buffers characters as they are accessed and
 * can efficiently skip over processed characters.
 *
 * @param reader the underlying Reader to read from
 * @param size the total size of the input (if known)
 */
//todo: use Ox
final class LazyReader(private val reader: Reader, private var size: Long) extends CharSequence, Closeable:
  private val buffer = mutable.ArrayDeque.empty[Char]
  private val chunk = new Array[Char](8192)

  /**
   * Gets the character at the specified position.
   *
   * This will read from the underlying Reader if necessary.
   *
   * @param pos the position (0-based)
   * @return the character at that position
   * @throws IndexOutOfBoundsException if the position is beyond the end of input
   */
  def charAt(pos: Int): Char =
    ensure(pos)
    buffer(pos)

  /**
   * Gets the length of the input.
   *
   * @return the length, capped at Int.MaxValue
   */
  def length: Int = math.min(Int.MaxValue, size).toInt

  /**
   * Creates a subsequence.
   *
   * @param start the start position (inclusive)
   * @param end the end position (exclusive)
   * @return a string containing the subsequence
   */
  def subSequence(start: Int, end: Int): CharSequence =
    ensure(end - 1)
    buffer.slice(start, end).mkString

  /**
   * Skips the first count characters.
   *
   * This is used to advance past tokens that have been processed.
   *
   * @param count the number of characters to skip
   * @return this LazyReader for chaining
   */
  def from(count: Int): LazyReader =
    buffer.remove(0, count)
    size -= count
    this

  override def toString: String = subSequence(0, length).toString

  override def close(): Unit = reader.close()

  @tailrec
  private def ensure(pos: Int): Unit =
    if pos >= buffer.length then
      val charsRead = reader.read(chunk)
      if charsRead == -1 then
        throw new IndexOutOfBoundsException(s"Position $pos is out of bounds for LazyReader of size $size")
      else
        buffer.appendAll(chunk.iterator.take(charsRead))
        ensure(pos)

/**
 * Factory methods for creating LazyReader instances.
 */
object LazyReader:

  /**
   * Creates a LazyReader from a file path.
   *
   * @param path the path to the file
   * @param charset the character encoding (defaults to UTF-8)
   * @return a new LazyReader
   */
  def from(path: Path, charset: Charset = StandardCharsets.UTF_8): LazyReader =
    val reader = Files.newBufferedReader(path, charset)
    val size = Files.size(path)
    LazyReader(reader, size)
