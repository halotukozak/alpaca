package alpaca.lexer
package context
package default

import alpaca.core.{BetweenStages, CtxMarker}

trait LineTracking extends CtxMarker {
  this: GlobalCtx[?] =>

  var line: Int
}

object LineTracking:
  given BetweenStages[LineTracking] = (token, m, ctx) => if m.matched == "\n" then ctx.line += 1
