package alpaca.lexer

import scala.util.matching.Regex.Match

//todo: find a way to make Ctx immutable with mutable-like changes https://github.com/halotukozak/alpaca/issues/45

trait EmptyCtx { // or AnyCtx ?
  var text: String

  def betweenLexems(m: Match): Unit = {
    this.text = this.text.substring(m.start, m.end)
  }
}

trait PositionTracking { this: EmptyCtx =>
  var position: Int

  override def betweenLexems(m: Match): Unit = {
    this.position = this.position + m.end
  }
}

case class DefaultCtx(
  val text: String,
  var position: Int,
) extends EmptyCtx
    with PositionTracking

//todo: custom Ctx should be available https://github.com/halotukozak/alpaca/issues/45
inline given ctx[Ctx <: EmptyCtx]: Ctx = compiletime.summonInline
