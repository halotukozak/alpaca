package alpaca
package internal
package lexer

import scala.annotation.implicitNotFound

/**
 * A typeclass defining error-handling logic for a specific lexer context.
 * Instances of this typeclass provide a mechanism to handle errors
 * encountered during the lexing process.
 *
 * This trait represents a function that, given a lexer context (`Ctx`),
 * produces no further computation (`Nothing`). Implementations are expected
 * to manage error handling, including logging, modifying the context state,
 * or halting processing.
 *
 * @tparam Ctx The type of the lexer context, which must extend `LexerCtx`.
 */
@implicitNotFound("Define ErrorHandling for ${Ctx}.")
trait ErrorHandling[-Ctx <: LexerCtx] extends (Ctx => Nothing)
