package alpaca

import alpaca.internal.dummy
import scala.quoted.*
import alpaca.internal.raiseShouldNeverBeCalled

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
case class DebugSettings[Enabled <: Boolean, Directory <: String, Timeout <: Int](
  enabled: Enabled,
  directory: Directory,
  timeout: Timeout,
)

object DebugSettings {
  type Any = DebugSettings[?, ?, ?]

  given FromExpr[DebugSettings[?, ?, ?]]:
    def unapply(x: Expr[DebugSettings[?, ?, ?]])(using Quotes): Option[DebugSettings[?, ?, ?]] =
      import quotes.reflect.*
      x.asTerm.tpe.widen match
        case AppliedType(
              _,
              List(
                ConstantType(BooleanConstant(enabled)),
                ConstantType(StringConstant(directory)),
                ConstantType(IntConstant(timeout)),
              ),
            ) =>
          Some(DebugSettings(enabled, directory, timeout))
        case _ => None

  inline def apply[Enabled <: Boolean, Directory <: String, Timeout <: Int]()
    : DebugSettings[Enabled, Directory, Timeout] =
    DebugSettings(compiletime.constValue[Enabled], compiletime.constValue[Directory], compiletime.constValue[Timeout])

  /**
   * Default debug settings with debugging disabled.
   *
   * Debug output is disabled by default and would be written to "debug/" if enabled.
   */
  given default: DebugSettings[false, "debug/", 90] = DebugSettings()
}
