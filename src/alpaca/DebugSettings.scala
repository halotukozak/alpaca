package alpaca

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
 */
final case class DebugSettings(
  enabled: Boolean & Singleton,
  directory: String & Singleton,
  timeout: Int & Singleton,
  verboseNames: Boolean & Singleton,
)

object DebugSettings {

  /**
   * Default debug settings with debugging disabled.
   */
  given default: DebugSettings = DebugSettings(false, "debug/", 90, false)
}
