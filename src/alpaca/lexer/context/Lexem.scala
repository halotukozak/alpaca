package alpaca.lexer
package context

trait Lexem[Name <: ValidName, Value] {
  val name: Name
  val value: Value
}
