package alpaca.lexer.context
package default

import alpaca.core.Empty

final case class EmptyGlobalCtx[LexemTpe <: Lexem[?]](
  var lastLexem: LexemTpe | Null = null,
  protected var _text: CharSequence = "",
) extends GlobalCtx[LexemTpe]

object EmptyGlobalCtx:
  given [LexemTpe <: Lexem[?]]: Empty[EmptyGlobalCtx[LexemTpe]] = Empty.derived

final case class DefaultGlobalCtx[LexemTpe <: Lexem[?]](
  var lastLexem: LexemTpe | Null = null,
  protected var _text: CharSequence = "",
  var position: Int = 0,
) extends GlobalCtx[LexemTpe]
    with PositionTracking

object DefaultGlobalCtx:
  given [LexemTpe <: Lexem[?]]: Empty[DefaultGlobalCtx[LexemTpe]] = Empty.derived
