package halotukozak
package alpaca.internal

import java.nio.file.{Files, Path}

/**
 * Shared helpers for writing compile-time grammar exports (lexer tokens,
 * parser productions) to disk as JSON, gated by [[GrammarExportSettings]].
 *
 * Kept dependency-free (no JSON library) since this runs inside the compiler
 * process for every consumer of the `lexer`/parser macros, not just for
 * consumers of an optional downstream tool.
 */
private[internal] object JsonExport:

// $COVERAGE-OFF$
  /** Writes `content` to `<dir>/<fileName>`, skipping the write if the file already has this exact content. */
  def maybeWrite(dir: String, fileName: String, content: String): Unit =
    val path = Path.of(dir).resolve(fileName)
    try
      if !Files.exists(path) || Files.readString(path) != content then
        if path.getParent != null then Files.createDirectories(path.getParent)
        Files.writeString(path, content)
    catch case e: Exception => System.err.println(s"[alpaca] failed to write grammar export to $path: $e")

  def quote(s: String): String =
    val sb = StringBuilder("\"")
    s.foreach:
      case '"' => sb.append("\\\"")
      case '\\' => sb.append("\\\\")
      case '\n' => sb.append("\\n")
      case '\r' => sb.append("\\r")
      case '\t' => sb.append("\\t")
      case c if c < ' ' => sb.append(f"\\u${c.toInt}%04x")
      case c => sb.append(c)
    sb.append('"').toString
// $COVERAGE-ON$
