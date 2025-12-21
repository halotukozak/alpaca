package alpaca

import scala.quoted.FromExpr

import scala.language.experimental.modularity

/**
 * Configuration settings for debug output generation.
 *
 * This case class controls whether debug information should be generated
 * during compilation and where it should be written. Debug output includes
 * parse tables, action tables, and production rules that can help understand
 * and diagnose parser behavior.
 *
 * @param enabled runtime value of the enabled flag
 * @param directory runtime value of the directory path
 * @param timeout runtime value of the timeout
 */
final case class DebugSettings(
  tracked val enabled: Boolean,
  tracked val directory: String,
  tracked val timeout: Int,
)

object DebugSettings {
  /**
   * Default debug settings with debugging disabled.
   *
   * Debug output is disabled by default and would be written to "debug/" if enabled.
   */
  inline given default: DebugSettings = DebugSettings(false, "debug/", 90)
}
