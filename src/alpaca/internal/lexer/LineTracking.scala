package alpaca
package internal
package lexer

/**
 * A trait for contexts that track line numbers.
 *
 * This trait adds line number tracking to a lexer context. The line
 * number is incremented each time a newline character is matched.
 */
trait LineTracking extends LexerCtx:
  /** The current line number (1-based). */
  var line: Int

object LineTracking:

  /**
   * OnTokenMatch instance that increments the line number on newlines.
   *
   * This is automatically composed with other OnTokenMatch instances
   * when the context extends LineTracking.
   */
  given OnTokenMatch[LineTracking] =
    case (_, "\n", ctx) => ctx.line += 1
    case _ => ()
