package alpaca
package lexer
package context

/** A lexem represents a token that has been matched and extracted from the input.
  *
  * A lexem contains the token's name and the value that was extracted from
  * the matched text. This is the output of the tokenization process.
  *
  * @tparam Name the token name type
  * @tparam Value the value type
  * @param name the token name
  * @param value the extracted value
  */
final case class Lexem[+Name <: ValidName, +Value](name: Name, value: Value)
//todo: (attributes: Map[String, Any] = Map.empty) extends Selectable

/** Companion object for Lexem. */
object Lexem {
  
  /** A special end-of-file lexem used to signal the end of input.
    *
    * This is used internally by the parser to detect when all input has been consumed.
    */
  val EOF: Lexem["$", String] = Lexem("$", "")
}
