package alpaca.parser.context

import alpaca.core.{BetweenStages, Copyable}

import scala.deriving.Mirror
import scala.util.matching.Regex.Match

type AnyGlobalCtx = GlobalCtx

object AnyGlobalCtx:
  given BetweenStages[AnyGlobalCtx] = (m: Match, ctx: AnyGlobalCtx) => {
    ??? // todo: https://github.com/halotukozak/alpaca/issues/51
  }

trait GlobalCtx

object GlobalCtx:
  given [Ctx <: GlobalCtx & Product: Mirror.ProductOf]: Copyable[Ctx] =
    Copyable.derived
