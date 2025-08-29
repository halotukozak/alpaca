package alpaca.lexer.context
package default

final case class EmptyGlobalCtx[LexemTpe <: Lexem[?]](
  var lastLexem: LexemTpe | Null = null,
  protected var _text: CharSequence = "",
) extends GlobalCtx[LexemTpe]
// derives Copyable todo: https://github.com/halotukozak/alpaca/issues/54
// derives Empty todo: https://github.com/halotukozak/alpaca/issues/53l
final case class DefaultGlobalCtx[LexemTpe <: Lexem[?]](
  var lastLexem: LexemTpe | Null = null,
  protected var _text: CharSequence = "",
  var position: Int = 0,
) extends GlobalCtx[LexemTpe]
    with PositionTracking
// derives Copyable todo: https://github.com/halotukozak/alpaca/issues/54
// derives Empty todo: https://github.com/halotukozak/alpaca/issues/53l
