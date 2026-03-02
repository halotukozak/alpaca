package alpaca
package internal
package lexer

import scala.annotation.implicitNotFound

/**
 * Trait representing error handling strategies for a lexer context.
 *
 * This trait allows the specification of error handling strategies
 * for various lexer contexts by transforming a given lexer context
 * to an instance of `ErrorHandling.Strategy`. The strategies determine
 * how the lexer should proceed when it encounters an error, such as
 * invalid tokens or characters during parsing.
 *
 * Error handling is essential for customizing lexer behavior in response
 * to specific scenarios, including ignoring or stopping on errors, or
 * throwing specific exceptions.
 *
 * Users must define an implicit instance of this trait to provide the
 * error handling behavior for a custom lexer context.
 *
 * @tparam Ctx The lexer context that this error handling strategy applies to.
 *             Must be a subtype of `LexerCtx`.
 */
@implicitNotFound("Define ErrorHandling for ${Ctx}.")
trait ErrorHandling[-Ctx <: LexerCtx] extends (Ctx => ErrorHandling.Strategy)

/**
 * Provides mechanisms for defining error-handling strategies when processing input.
 * This object contains a sealed enumeration `Strategy` that specifies the different
 * ways errors can be addressed during tokenization or parsing workflows.
 */
object ErrorHandling:
  enum Strategy:
    case Throw(ex: Exception)

    /** Skips the single current character that failed to match any token and continues. */
    case IgnoreChar

    /** Skips the entire sequence that failed to match and continues from the next successful match. If the match is not found, it skips the current character. */
    case IgnoreToken

    /** Gracefully stops tokenization at the current position, returning the lexemes collected so far. */
    case Stop
