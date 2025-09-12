package alpaca.lexer.context
package default

import alpaca.core.BetweenStages

trait LineTracking {
  this: GlobalCtx[?] =>

  var line: Int
}

object LineTracking:
  given BetweenStages[LineTracking & AnyGlobalCtx] =  (name, m, ctx) => {
    if m.matched == "\n" then
      ctx.line += 1
  }
