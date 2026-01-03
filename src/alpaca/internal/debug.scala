package alpaca
package internal

import java.io.{File, FileWriter}
import java.nio.file.Path
import scala.util.Using

/**
 * An opaque type representing a source code position for debug messages.
 *
 * This type wraps a line number and a file name and is used to annotate debug output
 * with the location where a debug call was made.
 */
opaque private[internal] type DebugPosition = (line: Int, file: String)

private[internal] object DebugPosition:

  /**
   * Implicit instance that captures the current source line number and file name.
   *
   * When used in a debug call, this automatically provides the line number and
   * file name where the call was made.
   */
  inline given here: DebugPosition = ${ hereImpl }

  private def hereImpl(using quotes: Quotes): Expr[DebugPosition] =
    import quotes.reflect.*
    val pos = Position.ofMacroExpansion
    Expr((pos.startLine + 1, pos.sourceFile.name))

  given Showable[DebugPosition] = Showable: (line, file) =>
    show"line $line in $file"
