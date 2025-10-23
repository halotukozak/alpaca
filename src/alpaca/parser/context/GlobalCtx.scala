package alpaca.parser.context

import alpaca.core.Copyable
import alpaca.lexer.BetweenStages

import scala.deriving.Mirror
import scala.util.matching.Regex.Match

/** Type alias for any parser global context. */
type AnyGlobalCtx = GlobalCtx

/**
 * Base trait for parser global context.
 *
 * Unlike the lexer, the parser's global context is typically empty by default,
 * but can be extended to track custom state during parsing such as symbol tables,
 * type information, or other semantic data.
 */
trait GlobalCtx

object GlobalCtx:

  /**
   * Automatic Copyable instance for any GlobalCtx that is a Product (case class).
   *
   * @tparam Ctx the context type
   */
  given [Ctx <: GlobalCtx & Product: Mirror.ProductOf]: Copyable[Ctx] =
    Copyable.derived
