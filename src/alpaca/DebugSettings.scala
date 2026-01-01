package alpaca

import scala.quoted.{Expr, FromExpr, Quotes, ToExpr}

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

object DebugSettings:

  /**
   * Default debug settings with debugging disabled.
   */
  given default: DebugSettings = DebugSettings(false, "debug/", 90, false)

  private[alpaca] def summonUnsafe(using Quotes): DebugSettings =
    Expr.summon[DebugSettings].get.valueOrAbort

  private[alpaca] given FromExpr[DebugSettings] with

    def unapply(expr: Expr[DebugSettings])(using quotes: Quotes): Option[DebugSettings] =
      import quotes.reflect.*

      val extractValue: Expr[DebugSettings] => Option[DebugSettings] =
        case '{ DebugSettings($enabled, $directory, $timeout, $verboseNames) } =>
          for
            en <- enabled.value
            dir <- directory.value
            to <- timeout.value
            vn <- verboseNames.value
          yield DebugSettings(en, dir, to, vn)
        case other => None

      extractValue(expr)
        .orElse:
          extractValue:
            expr.asTerm.symbol.tree match
              case ValDef(_, _, Some(rhs)) => rhs.asExprOf[DebugSettings]
              case DefDef(_, Nil, _, Some(rhs)) => rhs.asExprOf[DebugSettings]
              case x => report.errorAndAbort("DebugSettings must be a given val")

  private[alpaca] given ToExpr[DebugSettings] with
    def apply(x: DebugSettings)(using Quotes): Expr[DebugSettings] =
      '{ DebugSettings(${ Expr(x.enabled) }, ${ Expr(x.directory) }, ${ Expr(x.timeout) }, ${ Expr(x.verboseNames) }) }
