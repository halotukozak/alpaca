package alpaca.lexer

trait Lexem[Name <: ValidName] {
  val name: Name
  val value: Any
}

final case class DefaultLexem[Name <: ValidName](
  name: Name,
  value: Any,
) extends Lexem[Name]
