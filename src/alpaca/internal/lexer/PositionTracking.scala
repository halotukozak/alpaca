package alpaca
package internal
package lexer

/**
 * A trait for contexts that track character positions.
 *
 * This trait adds character position tracking to a lexer context. The
 * position tracks the column number within the current line and is
 * reset to 1 when a newline is encountered.
 */
trait PositionTracking extends LexerCtx:
  this: Product =>

  /** The current character position within the line (1-based). */
  var position: Int

object PositionTracking:

  /**
   * BetweenStages instance that updates the position after each match.
   *
   * The position is reset to 1 on newlines, otherwise it is incremented
   * by the length of the matched text.
   *
   * This is automatically composed with other BetweenStages instances
   * when the context extends PositionTracking.
   */
  given BetweenStages[PositionTracking] = (_, matcher, ctx) =>
    matcher.matched match
      case "\n" => ctx.position = 1
      case matched => ctx.position += matched.nn.length
