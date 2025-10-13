package alpaca.lexer
package context
package default

import alpaca.lexer.BetweenStages

/** A trait for contexts that track character positions.
  *
  * This trait adds character position tracking to a lexer context. The
  * position tracks the column number within the current line and is
  * reset to 1 when a newline is encountered.
  */
trait PositionTracking extends GlobalCtx {
  /** The current character position within the line (1-based). */
  var position: Int
}

object PositionTracking:
  
  /** BetweenStages instance that updates the position after each match.
    *
    * The position is reset to 1 on newlines, otherwise it is incremented
    * by the length of the matched text.
    *
    * This is automatically composed with other BetweenStages instances
    * when the context extends PositionTracking.
    */
  given BetweenStages[PositionTracking] = (name, m, ctx) =>
    if m.matched == "\n" then ctx.position = 1
    else ctx.position += m.matched.length
