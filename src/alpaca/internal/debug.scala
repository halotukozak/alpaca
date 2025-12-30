package alpaca
package internal

import java.io.{File, FileWriter}
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

  given Showable[DebugPosition] = Showable: pos =>
    show"at line ${pos.line} in ${pos.file}"

/**
 * Writes debug content to a file if debug settings are enabled.
 *
 * This function conditionally writes the content to a file in the
 * debug directory only when debugging is enabled in the debug settings.
 * The directory structure is created if it doesn't exist.
 *
 * @param path          the relative path within the debug directory
 * @param content       the content to write
 * @param debugSettings the debug settings determining if/where to write
 */
private[internal] def debugToFile(path: String)(content: Shown)(using debugSettings: DebugSettings): Unit =
  if debugSettings.enabled then
    val file = new File(s"${debugSettings.directory}$path")
    file.getParentFile.mkdirs()
    Using.resource(new FileWriter(file))(_.write(content))

private[internal] def debug(msg: Shown)(using debugSettings: DebugSettings, quotes: Quotes): Unit =
  if debugSettings.enabled then quotes.reflect.report.info(msg)
