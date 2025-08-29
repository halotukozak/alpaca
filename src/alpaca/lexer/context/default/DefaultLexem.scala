package alpaca.lexer
package context
package default

final case class DefaultLexem[Name <: ValidName](
  name: Name,
  value: Any,
) extends Lexem[Name]
