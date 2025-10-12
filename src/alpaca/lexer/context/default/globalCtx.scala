package alpaca.lexer.context
package default

/** An empty lexer context with no extra state tracking.
  *
  * This is the simplest context that only tracks the remaining text.
  * Use this when you don't need line or position tracking.
  *
  * @param text the remaining text to tokenize
  */
final case class EmptyGlobalCtx(
  var text: CharSequence = "",
) extends GlobalCtx

/** The default lexer context with position and line tracking.
  *
  * This context tracks:
  * - The remaining text to tokenize
  * - The current character position (1-based)
  * - The current line number (1-based)
  *
  * This is the most commonly used context and provides useful information
  * for error reporting.
  *
  * @param text the remaining text to tokenize
  * @param position the current character position (1-based)
  * @param line the current line number (1-based)
  */
final case class DefaultGlobalCtx(
  var text: CharSequence = "",
  var position: Int = 1,
  var line: Int = 1,
) extends GlobalCtx
    with PositionTracking
    with LineTracking
