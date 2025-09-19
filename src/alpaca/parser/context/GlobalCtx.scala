package alpaca.parser.context

import alpaca.core.{BetweenStages, Copyable, CtxMarker}
import alpaca.lexer.Token

import scala.deriving.Mirror
import scala.util.matching.Regex.Match

type AnyGlobalCtx = GlobalCtx

object AnyGlobalCtx:
  given BetweenStages[AnyGlobalCtx] = (token: Token[?, ?, ?], m: Match, ctx: AnyGlobalCtx) => {
    ??? // todo: https://github.com/halotukozak/alpaca/issues/51}
  }

trait GlobalCtx extends CtxMarker

object GlobalCtx:
  given [Ctx <: GlobalCtx & Product: Mirror.ProductOf]: Copyable[Ctx] =
    Copyable.derived
