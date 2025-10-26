package alpaca
package parser

import alpaca.core.*
import alpaca.lexer.{AlgorithmError, Token}

import scala.annotation.compileTimeOnly

type ConflictResolution

extension (str: Production)
  @compileTimeOnly(RuleOnly)
  inline infix def after(other: (Production | Token[?, ?, ?])*): ConflictResolution = dummy
  @compileTimeOnly(RuleOnly)
  inline infix def before(other: (Production | Token[?, ?, ?])*): ConflictResolution = dummy

opaque type ConflictResolutionTable = Map[NSet[2, Production | String], Production | String]

object ConflictResolutionTable {
  def apply(resolutions: Map[NSet[2, Production | String], Production | String]): ConflictResolutionTable = resolutions

  extension (table: ConflictResolutionTable)
    def get(first: ParseAction, second: ParseAction)(symbol: Symbol): Option[ParseAction] = {
      val extractProdOrName: ParseAction => Production | String =
        case ParseAction.Reduction(prod) => prod
        case _: ParseAction.Shift => symbol.name

      table
        .get(NSet((extractProdOrName(first), extractProdOrName(second))))
        .map: res =>
          if extractProdOrName(first) == res then first
          else if extractProdOrName(second) == res then second
          else
            throw new AlgorithmError(
              s"Conflict resolution target does not match any of the actions: $first, $second -> $res",
            )
    }

  given Showable[ConflictResolutionTable] =
    _.map: (k, v) =>
      def show(x: Production | String): String = x match
        case p: Production => show"$p"
        case s: String => show"Token[$s]"

      show"${show((k - v).head)} before ${show(v)}"
    .mkShow("\n")
}
