package alpaca.lexer
package context

import alpaca.core.{BetweenStages, Copyable, CtxMarker}
import alpaca.lexer.context.default.DefaultLexem

import scala.deriving.Mirror
import scala.util.matching.Regex.Match

type AnyGlobalCtx = GlobalCtx[?]

trait GlobalCtx[LexemTpe <: Lexem[?, ?]] extends CtxMarker {
  def text: String = _text.toString
  var lastLexem: Lexem[?, ?] | Null
  var _text: CharSequence
}

object GlobalCtx:
  given [LexemTpe <: Lexem[?, ?], Ctx <: GlobalCtx[LexemTpe] & Product: Mirror.ProductOf]: Copyable[Ctx] =
    Copyable.derived

  given BetweenStages[AnyGlobalCtx] = (token, m, ctx) => {
    ctx.lastLexem = DefaultLexem(token.name, m.matched)
    ctx._text = ctx._text.from(m.end)
  }
