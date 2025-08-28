package alpaca.lexer

import alpaca.core.reifyAllBetweenLexems
import alpaca.showAst

import scala.util.matching.Regex.Match

trait AnyLexemCtx {
  var text: String
}

type AnyGlobalCtx = GlobalCtx[?]

//todo: find a way to make Ctx immutable with mutable-like changes
trait GlobalCtx[LCtx <: AnyLexemCtx] {
  type LexemCtx = LCtx

  var lastLexemCtx: LexemCtx
  var text: String

  // todo: find some better way of modularization
  def betweenLexems(m: Match): Unit = {
    this.text = this.text.substring(m.start, m.end)
  }

  // todo: better names
  def betweenStages(m: Match): Unit = {
    this.text = this.text.substring(m.start)
  }
}

trait PositionTracking {
  this: EmptyGlobalCtx[?] =>

  var position: Int

  def betweenLexems(m: Match): Unit = {
    this.position = this.position + m.end
  }

  // todo: better names
  def betweenStages(m: Match): Unit = ???
}

case class EmptyGlobalCtx[LexemCtx <: AnyGlobalCtx](
  var lastLexemCtx: LexemCtx,
  var text: String,
) extends GlobalCtx[LexemCtx]

case class EmptyLexemCtx(
  var text: String,
)

case class DefaultGlobalCtx[LexemCtx <: AnyGlobalCtx](
  var lastLexemCtx: LexemCtx,
  var text: String,
  var position: Int,
) extends GlobalCtx[LexemCtx]
    with PositionTracking {
  override def betweenLexems(m: Match): Unit = {
    super[GlobalCtx].betweenLexems(m)
    super[PositionTracking].betweenLexems(m)
  }

  // todo: better names
  override def betweenStages(m: Match): Unit = {
    super[GlobalCtx].betweenStages(m)
    super[PositionTracking].betweenStages(m)
  }
}

transparent inline given ctx(using c: AnyGlobalCtx): c.type = c
