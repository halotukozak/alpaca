package alpaca
package internal

/**
 * An opaque type representing a source code position for debug messages.
 *
 * This type wraps a line number and is used to annotate debug output
 * with the location where a debug call was made.
 */
opaque private[internal] type DebugPosition = (Int, String)

private[internal] object DebugPosition:

  given Showable[DebugPosition] = Showable: (line, file) =>
    s"at line $line in $file"

  /**
   * Implicit instance that captures the current source line number.
   *
   * When used in a debug call, this automatically provides the line number
   * where the call was made.śśś
   */
  inline given here: DebugPosition = ${ hereImpl }

  private def hereImpl(using quotes: Quotes): Expr[DebugPosition] =
    import quotes.reflect.*
    val pos = Position.ofMacroExpansion
    Expr((pos.startLine + 1, pos.sourceFile.name))
