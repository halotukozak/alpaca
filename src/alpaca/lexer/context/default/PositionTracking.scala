package alpaca.lexer
package context
package default

import alpaca.lexer.BetweenStages

import scala.util.matching.Regex.Match

trait PositionTracking extends GlobalCtx {
  var position: Int
}

object PositionTracking:
  given BetweenStages[PositionTracking] = (name, m, ctx) =>
    if m.matched == "\n" then ctx.position = 1
    else ctx.position += m.matched.length
