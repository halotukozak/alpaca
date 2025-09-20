package alpaca.lexer
package context
package default

import alpaca.lexer.BetweenStages

trait LineTracking extends GlobalCtx {
  var line: Int
}

object LineTracking:
  given BetweenStages[LineTracking] = (token, m, ctx) => if m.matched == "\n" then ctx.line += 1
