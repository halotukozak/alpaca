package halotukozak
package alpaca.internal

import java.nio.file.{Files, Path}

import halotukozak.mcodec.{Json, MCodec}

import scala.util.control.NonFatal

/**
 * Writes a compile-time grammar export (lexer tokens, parser productions/table) to disk as JSON,
 * gated by [[GrammarExportSettings]].
 *
 * Kept dependency-light (mcodec, no ad-hoc JSON string building) since this runs inside the
 * compiler process for every consumer of the `lexer`/parser macros, not just for consumers of an
 * optional downstream tool.
 */
private[internal] object JsonExport:

// $COVERAGE-OFF$
  /** Writes `value` to `<name>.<suffix>.json` in the configured export directory, if any; a no-op otherwise. */
  def maybeWrite[T: MCodec](name: String, suffix: String, value: => T)(using settings: GrammarExportSettings): Unit =
    settings.exportDirectory.foreach: dir =>
      val path = Path.of(dir).resolve(s"$name.$suffix.json")
      val content = Json.write(value)
      try
        if !Files.exists(path) || Files.readString(path) != content then
          if path.getParent != null then Files.createDirectories(path.getParent)
          Files.writeString(path, content)
      catch
        case NonFatal(e) => System.err.println(s"[alpaca] failed to write grammar export to $path: $e")
// $COVERAGE-ON$
