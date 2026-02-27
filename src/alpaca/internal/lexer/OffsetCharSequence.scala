package alpaca
package internal
package lexer

/**
 * A mutable CharSequence wrapper that makes text advancement O(1).
 *
 * Instead of creating a new String via `subSequence` each time the lexer
 * advances past a token (which copies O(remaining) characters), this class
 * simply increments an internal offset. All CharSequence methods account
 * for the offset transparently.
 *
 * @param underlying the original input string
 * @param offset     the current logical start position (advanced by `from`)
 */
private[alpaca] final class OffsetCharSequence(
  private val underlying: String,
  private var offset: Int = 0,
) extends CharSequence:

  def length: Int = underlying.length - offset

  def charAt(index: Int): Char = underlying.charAt(index + offset)

  def subSequence(start: Int, end: Int): CharSequence =
    underlying.substring(start + offset, end + offset)

  /**
   * Advances the logical start position by `count` characters.
   *
   * This is an O(1) operation that mutates the offset in place,
   * avoiding the O(remaining) cost of `String.subSequence`.
   *
   * @param count number of characters to skip
   * @return this instance (for fluent use)
   */
  def from(count: Int): OffsetCharSequence =
    offset += count
    this

  override def toString: String = underlying.substring(offset)
