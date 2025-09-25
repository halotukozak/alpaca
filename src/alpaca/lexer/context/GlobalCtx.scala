package alpaca.lexer
package context

import alpaca.core.Copyable
import alpaca.lexer.BetweenStages
import alpaca.lexer.context.Lexem

import scala.deriving.Mirror
import scala.util.NotGiven
import scala.util.matching.Regex.Match

type AnyGlobalCtx = GlobalCtx

trait GlobalCtx {
  var lastLexem: Lexem[?, ?] = compiletime.uninitialized
  var lastRawMatched: String = compiletime.uninitialized
  var text: CharSequence
}

object GlobalCtx:
  given [Ctx <: GlobalCtx & Product: Mirror.ProductOf]: Copyable[Ctx] =
    Copyable.derived

  given BetweenStages[AnyGlobalCtx] =
    case (DefinedToken(name, _, modifyCtx, remapping), m, ctx) =>
      ctx.lastRawMatched = m.matched
      ctx.lastLexem = Lexem(name, remapping(ctx))
      ctx.text = ctx.text.from(m.end)
      modifyCtx(ctx)

    case (IgnoredToken(name, _, modifyCtx), m, ctx) =>
      ctx.text = ctx.text.from(m.end)
      modifyCtx(ctx)
