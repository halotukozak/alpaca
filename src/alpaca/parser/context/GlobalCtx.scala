package alpaca.parser.context

import alpaca.core.Copyable
import alpaca.lexer.BetweenStages

import scala.deriving.Mirror
import scala.util.matching.Regex.Match

type AnyGlobalCtx = GlobalCtx

trait GlobalCtx

object GlobalCtx:
  given [Ctx <: GlobalCtx & Product: Mirror.ProductOf]: Copyable[Ctx] =
    Copyable.derived
