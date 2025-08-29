package alpaca.lexer.context.default

import alpaca.core.Copyable
import alpaca.lexer.ValidName
import alpaca.lexer.context.Lexem

final case class DefaultLexem[Name <: ValidName](
  name: Name,
  value: Any,
) extends Lexem[Name]
