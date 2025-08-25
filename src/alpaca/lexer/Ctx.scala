package alpaca.lexer

import alpaca.core.reifyAllBetweenLexems

import scala.util.matching.Regex.Match

//todo: find a way to make Ctx immutable with mutable-like changes
trait EmptyCtx { // or AnyCtx ?
  var text: String

  // todo: find some better way of modularization
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

case class NoCtx(
  var text: String,
) extends EmptyCtx {

  override def betweenLexems(m: Match): Unit =
    reifyAllBetweenLexems(this)(m)
}

object NoCtx {
  def create(arg: String) = new NoCtx(arg)
}

case class DefaultCtx(
  var text: String,
  var position: Int,
) extends EmptyCtx
    with PositionTracking {

  override def betweenLexems(m: Match): Unit =
    reifyAllBetweenLexems(this)(m)
}

object DefaultCtx {
  def create(arg: String) = new DefaultCtx(arg, 0)
}

transparent inline given ctx(using c: EmptyCtx): c.type = c
