package alpaca.internal

/**
 * An opaque type representing a source code position for debug messages.
 *
 * This type wraps a line number and is used to annotate debug output
 * with the location where a debug call was made.
 */
opaque private[internal] type DebugPosition = (Int, String)

private[internal] object DebugPosition {
  given DebugPosition is Showable = (line, file) => s"at line $line ($file)"

  /**
   * Implicit instance that captures the current source line number.
   *
   * When used in a debug call, this automatically provides the line number
   * where the call was made.śśś
   */
  inline given here: DebugPosition = ${ hereImpl }

  private def hereImpl(using quotes: Quotes): Expr[DebugPosition] = {
    import quotes.reflect.*
    val pos = Position.ofMacroExpansion
    Expr((pos.startLine + 1, pos.sourceFile.name))
  }

  given ToExpr[DebugPosition]:
    def apply(x: DebugPosition)(using quotes: Quotes): Expr[DebugPosition] =
      ToExpr.Tuple2ToExpr[Int, String].apply(x)

  given FromExpr[DebugPosition]:
    def unapply(x: Expr[DebugPosition])(using Quotes): Option[DebugPosition] =
      FromExpr.Tuple2FromExpr[Int, String].unapply(x)
}
