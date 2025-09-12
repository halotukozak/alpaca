package alpaca.lexer.context
package default

import alpaca.core.BetweenStages

trait LineTracking {
  this: GlobalCtx[?] =>

  var line: Int
}

object LineTracking:
  given BetweenStages[LineTracking & AnyGlobalCtx] =
    (m, ctx) => ??? // todo: https://github.com/halotukozak/alpaca/issues/51
