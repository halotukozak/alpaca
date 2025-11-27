package alpaca
package internal

import alpaca.internal.lexer.Lexem

import scala.collection.mutable

/**
 * Represents a syntax error encountered during lexing or parsing.
 *
 * This sealed trait hierarchy defines different types of syntax errors
 * that can occur during lexical analysis or parsing phases.
 */
sealed trait SyntaxError

object SyntaxError {

  /**
   * Represents an unexpected character encountered during lexing.
   *
   * @param char the unexpected character
   * @param position the position in the input where the error occurred (1-based)
   * @param line the line number where the error occurred (1-based)
   */
  final case class UnexpectedChar(
    char: Char,
    position: Int,
    line: Int,
  ) extends SyntaxError

  /**
   * Represents an unexpected token encountered during parsing.
   *
   * @param found the token that was found
   * @param expected the tokens that were expected at this position
   */
  final case class UnexpectedToken(
    found: Lexem[?, ?],
    expected: Set[String],
  ) extends SyntaxError
}

/**
 * A trait for contexts that support error recovery during lexing.
 *
 * When a lexer context extends this trait, the lexer will collect
 * errors instead of throwing exceptions, and attempt to continue
 * lexing the remaining input.
 */
trait LexerErrorRecovery {

  /** Collection of syntax errors encountered during lexing. */
  val lexerErrors: mutable.ListBuffer[SyntaxError] = mutable.ListBuffer.empty
}

/**
 * A trait for contexts that support error recovery during parsing.
 *
 * When a parser context extends this trait, the parser will collect
 * errors instead of throwing exceptions, and attempt to continue
 * parsing the remaining input using error recovery strategies.
 */
trait ParserErrorRecovery {

  /** Collection of syntax errors encountered during parsing. */
  val parserErrors: mutable.ListBuffer[SyntaxError] = mutable.ListBuffer.empty
}
