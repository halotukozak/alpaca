package alpaca.lexer

import scala.util.matching.Regex.Match

//todo: find a way to make Ctx immutable with mutable-like changes https://github.com/halotukozak/alpaca/issues/45

trait EmptyCtx { // or AnyCtx ?
  var text: String

  def betweenLexems(m: Match): Unit = {
    this.text = this.text.substring(m.start, m.end)
  }
}

object EmptyCtx {
  def create(arg: String): EmptyCtx = new EmptyCtx {
    var text: String = arg
  }
}

trait PositionTracking { this: EmptyCtx =>
  var position: Int

  override def betweenLexems(m: Match): Unit = {
    this.position = this.position + m.end
  }
}

case class DefaultCtx(
  var text: String,
  var position: Int,
) extends EmptyCtx
    with PositionTracking

object DefaultCtx {
  def create(arg: String) = new DefaultCtx(arg, 0)
}

//todo: custom Ctx should be available https://github.com/halotukozak/alpaca/issues/45
transparent inline given ctx(using c: EmptyCtx): c.type = c
// transparent inline def ctx(using c: EmptyCtx): c.type = c
