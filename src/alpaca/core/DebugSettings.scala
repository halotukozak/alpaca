package alpaca
package core

import scala.quoted.{Expr, FromExpr, Quotes}

final case class DebugSettings[Enabled <: Boolean & Singleton, Directory <: String & Singleton] private (
  enabled: Enabled,
  directory: Directory,
)

private[alpaca] object DebugSettings {
  given default: DebugSettings[false, "debug/"] = DebugSettings(false, "debug/")

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
