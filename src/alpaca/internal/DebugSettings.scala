package alpaca
package internal

import java.io.{File, FileWriter}
import scala.util.Using

given FromExpr[DebugSettings[?, ?]] with
  def unapply(x: Expr[DebugSettings[?, ?]])(using quotes: Quotes): Option[DebugSettings[?, ?]] =
    import quotes.reflect.*
    x.asTerm.tpe.widen match
      case AppliedType(
            tycon,
            List(ConstantType(BooleanConstant(enabled)), ConstantType(StringConstant(directory))),
          ) if tycon <:< TypeRepr.of[DebugSettings] =>
        Some(DebugSettings(enabled, directory))
      case _ => None

/**
 * Writes debug content to a file if debug settings are enabled.
 *
 * This function conditionally writes the content to a file in the
 * debug directory only when debugging is enabled in the debug settings.
 * The directory structure is created if it doesn't exist.
 *
 * @param path the relative path within the debug directory
 * @param content the content to write
 * @param debugSettings the debug settings determining if/where to write
 */
private[internal] def debugToFile(path: String)(content: Shown)(using debugSettings: DebugSettings[?, ?]): Unit =
  if debugSettings.enabled then
    val file = new File(s"${debugSettings.directory}$path")
    file.getParentFile.mkdirs()
    Using.resource(new FileWriter(file))(_.write(content))
