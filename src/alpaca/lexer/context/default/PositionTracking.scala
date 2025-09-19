package alpaca.lexer
package context
package default

import alpaca.core.{BetweenStages, CtxMarker}

import scala.util.matching.Regex.Match

trait PositionTracking extends CtxMarker {
  this: GlobalCtx[?] =>
  var position: Int
}

object PositionTracking:
  given BetweenStages[PositionTracking] = (token, m, ctx) => {
    if m.matched == "\n" then
      ctx.position = 1
    else
      ctx.position += m.matched.length
  }
