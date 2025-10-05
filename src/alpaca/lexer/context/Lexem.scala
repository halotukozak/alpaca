package alpaca
package lexer
package context

final case class Lexem[+Name <: ValidName, +Value](name: Name, value: Value)
//todo: (attributes: Map[String, Any] = Map.empty) extends Selectable
object Lexem {
  val EOF: Lexem["$", String] = Lexem("$", "")
}
