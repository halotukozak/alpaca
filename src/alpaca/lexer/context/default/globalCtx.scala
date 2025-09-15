package alpaca.lexer.context
package default

final case class EmptyGlobalCtx[LexemTpe <: Lexem[?, ?]](
  var lastLexem: Lexem[?, ?] | Null = null,
  var _text: CharSequence = "",
) extends GlobalCtx[LexemTpe]

final case class DefaultGlobalCtx[LexemTpe <: Lexem[?, ?]](
  var lastLexem: Lexem[?, ?] | Null = null,
  var _text: CharSequence = "",
  var position: Int = 0,
  var line: Int = 0,
) extends GlobalCtx[LexemTpe]
    with PositionTracking
    with LineTracking
