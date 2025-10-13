package alpaca.lexer
package context
package default

import alpaca.lexer.BetweenStages

/** A trait for contexts that track line numbers.
  *
  * This trait adds line number tracking to a lexer context. The line
  * number is incremented each time a newline character is matched.
  */
trait LineTracking extends GlobalCtx {
  /** The current line number (1-based). */
  var line: Int
}

object LineTracking:
  
  /** BetweenStages instance that increments the line number on newlines.
    *
    * This is automatically composed with other BetweenStages instances
    * when the context extends LineTracking.
    */
  given BetweenStages[LineTracking] = (token, m, ctx) => if m.matched == "\n" then ctx.line += 1
