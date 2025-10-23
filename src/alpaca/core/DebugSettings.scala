package alpaca
package core

import scala.quoted.{Expr, FromExpr, Quotes}

/**
 * Configuration settings for debug output generation.
 *
 * This case class controls whether debug information should be generated
 * during compilation and where it should be written. Debug output includes
 * parse tables, action tables, and production rules that can help understand
 * and diagnose parser behavior.
 *
 * @tparam Enabled whether debug output is enabled (compile-time boolean)
 * @tparam Directory the directory path for debug output (compile-time string)
 * @param enabled runtime value of the enabled flag
 * @param directory runtime value of the directory path
 */
final case class DebugSettings[Enabled <: Boolean & Singleton, Directory <: String & Singleton] private (
  enabled: Enabled,
  directory: Directory,
)

private[alpaca] object DebugSettings {
  /**
   * Default debug settings with debugging disabled.
   *
   * Debug output is disabled by default and would be written to "debug/" if enabled.
   */
  given default: DebugSettings[false, "debug/"] = DebugSettings(false, "debug/")

  /**
   * Creates DebugSettings from compile-time type parameters.
   *
   * This factory method extracts the compile-time values of the type parameters
   * and creates a DebugSettings instance with those values.
   *
   * @tparam Enabled the enabled flag as a singleton boolean type
   * @tparam Directory the directory path as a singleton string type
   * @return a new DebugSettings instance
   */
  inline def apply[Enabled <: Boolean & Singleton, Directory <: String & Singleton]()
    : DebugSettings[Enabled, Directory] =
    DebugSettings(compiletime.constValue[Enabled], compiletime.constValue[Directory])

  given FromExpr[DebugSettings[?, ?]] with
    def unapply(x: Expr[DebugSettings[?, ?]])(using quotes: Quotes): Option[DebugSettings[?, ?]] = {
      import quotes.reflect.*

      x.asTerm.tpe.widen match
        case AppliedType(
              tycon,
              List(ConstantType(BooleanConstant(enabled)), ConstantType(StringConstant(directory))),
            ) if tycon <:< TypeRepr.of[DebugSettings] =>
          Some(DebugSettings(enabled, directory))
        case _ => None
    }

}
