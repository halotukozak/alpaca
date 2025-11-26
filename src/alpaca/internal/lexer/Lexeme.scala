package alpaca
package internal
package lexer

import scala.annotation.unchecked.uncheckedVariance as uv
import NamedTuple.AnyNamedTuple
import NamedTuple.NamedTuple
import collection.immutable.SeqMap

/**
 * A lexeme represents a token that has been matched and extracted from the input.
 *
 * A lexeme contains the token's name and the value that was extracted from
 * the matched text. This is the output of the tokenization process.
 *
 * @tparam Name the token name type
 * @tparam Value the value type
 * @param name the token name
 * @param value the extracted value
 */
private[alpaca] final case class Lexeme[+Name <: ValidName, +Value](
  name: Name,
  value: Value,
  private[alpaca] val fields: Map[String, Any],
) extends Selectable {
  type Fields <: AnyNamedTuple
}

private[alpaca] object Lexeme:

  /**
   * A special end-of-file lexeme used to signal the end of input.
   *
   * This is used internally by the parser to detect when all input has been consumed.
   */
  val EOF: Lexeme["$", String] = Lexeme("$", "", Map.empty)
