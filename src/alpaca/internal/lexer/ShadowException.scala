package alpaca
package internal
package lexer

import scala.annotation.constructorOnly

/**
 * Exception thrown when a token pattern is shadowed by another pattern.
 *
 * This exception is thrown during lexer compilation when one regex pattern
 * will never match because an earlier pattern always matches first.
 *
 * @param first the pattern that is shadowed
 * @param second the pattern that shadows it
 */
final class ShadowException(first: String, second: String)(using @constructorOnly log: Log)
  extends AlpacaException(show"Pattern $first is shadowed by $second")
