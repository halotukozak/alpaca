package alpaca.lexer.context
package default

import alpaca.core.BetweenStages

import scala.util.matching.Regex.Match

trait PositionTracking {
  this: GlobalCtx[?] =>

  var position: Int
}

object PositionTracking:
  given BetweenStages[PositionTracking & AnyGlobalCtx] =
    (m, ctx) => ??? // todo: https://github.com/halotukozak/alpaca/issues/51
