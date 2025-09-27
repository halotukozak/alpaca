package alpaca.lexer
package context

trait Lexem[Name <: ValidName] {
  val name: Name
  val value: Any
}
