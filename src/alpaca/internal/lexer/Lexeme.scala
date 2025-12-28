package alpaca
package internal
package lexer

import scala.NamedTuple.AnyNamedTuple

import scala.language.experimental.modularity

/**
 * A lexeme represents a token that has been matched and extracted from the input.
 *
 * A lexeme contains the token's name and the value that was extracted from
 * the matched text. This is the output of the tokenization process.
 *
 * @param name the token name
 * @param value the extracted value
 */
private[alpaca] final case class Lexeme(
  tracked val name: String,
  tracked val value: Any,
  private[alpaca] val fields: Map[String, Any],
) extends Selectable {
  type Fields <: AnyNamedTuple
  def selectDynamic(name: String): Any = fields(name)
}

private[alpaca] object Lexeme:

  /**
   * A special end-of-file lexeme used to signal the end of input.
   *
   * This is used internally by the parser to detect when all input has been consumed.
   */
  val EOF: Lexeme = Lexeme("$", "", Map.empty)
