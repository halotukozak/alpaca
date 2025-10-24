package alpaca
package parser

import scala.annotation.compileTimeOnly
import alpaca.lexer.Token
import alpaca.core.ValidName
import alpaca.core.tap
import alpaca.lexer.AlgorithmError
import alpaca.core.*
import alpaca.core.Showable.mkShow

type ConflictResolution

extension (str: String)
  @compileTimeOnly(RuleOnly)
  inline infix def after(other: (String | Token[?, ?, ?])*): ConflictResolution = shoudNotBeCalled
  @compileTimeOnly(RuleOnly)
  inline infix def before(other: (String | Token[?, ?, ?])*): ConflictResolution = shoudNotBeCalled

final class ConflictResolutionTable(private val resolutions: Map[NSet[2, (Production | String)], (Production | String)]) {
  def get(first: ParseAction, second: ParseAction)(symbol: Symbol): Option[ParseAction] = {
    val extractProdOrName: ParseAction => Production | String =
      case ParseAction.Reduction(prod) => prod
      case _: ParseAction.Shift => symbol.name

    resolutions
      .get(NSet((extractProdOrName(first), extractProdOrName(second))))
      .map: res =>
        if extractProdOrName(first) == res then first
        else if extractProdOrName(second) == res then second
        else
          throw new AlgorithmError(
            s"Conflict resolution target does not match any of the actions: $first, $second -> $res",
          )
  }
}
