package alpaca.lexer

//todo: find a way to make Ctx immutable with mutable-like changes https://github.com/halotukozak/alpaca/issues/45
class Ctx(
  val _text: CharSequence,
  var position: Int,
) {
  private[lexer] def copy() = new Ctx(_text, position)

  def text: String = _text.toString

  override def toString: String = s"Ctx(text = $text, position = $position)"
}

//todo: custom Ctx should be available https://github.com/halotukozak/alpaca/issues/45

inline given ctx: Ctx = compiletime.summonInline
