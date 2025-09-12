package alpaca.lexer.context

import alpaca.core.{BetweenStages, Copyable}

import alpaca.lexer.context.default.DefaultLexem
import scala.deriving.Mirror
import scala.util.matching.Regex.Match

type AnyGlobalCtx = GlobalCtx[?]

object AnyGlobalCtx:
  given BetweenStages[AnyGlobalCtx] = (name: String, m: Match, ctx: AnyGlobalCtx) => {
    ctx.lastLexem = DefaultLexem(name, m.matched)
  }

trait GlobalCtx[LexemTpe <: Lexem[?, ?]] {
  def text: String = _text.toString
  var lastLexem: Lexem[?, ?] | Null
  protected var _text: CharSequence

  def text_=(t: CharSequence): Unit = _text = t
}

object GlobalCtx:
  given [LexemTpe <: Lexem[?, ?], Ctx <: GlobalCtx[LexemTpe] & Product: Mirror.ProductOf]: Copyable[Ctx] =
    Copyable.derived
