package alpaca.lexer.context

import scala.annotation.unchecked.uncheckedVariance
import scala.util.matching.Regex.Match

type AnyGlobalCtx = GlobalCtx[?]

object AnyGlobalCtx:
  given BetweenStages[AnyGlobalCtx] = (m: Match, ctx: AnyGlobalCtx) => {
    ??? // todo: https://github.com/halotukozak/alpaca/issues/51
  }

trait GlobalCtx[LexemTpe <: Lexem[?]] {
  val text: String = _text.toString
  var lastLexem: LexemTpe | Null
  protected var _text: CharSequence

  def text_=(t: CharSequence): Unit = _text = t
}
