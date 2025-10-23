package alpaca.lexer
package context

import alpaca.core.Copyable
import alpaca.lexer.BetweenStages
import alpaca.lexer.context.Lexem

import scala.deriving.Mirror
import scala.util.NotGiven
import scala.util.matching.Regex.Match

/** Type alias for any GlobalCtx. */
type AnyGlobalCtx = GlobalCtx

transparent inline given ctx(using c: AnyGlobalCtx): c.type = c

/**
 * Base trait for lexer global context.
 *
 * The global context maintains state during lexing, including the current
 * position in the input, the last matched token, and the remaining text to process.
 * Users can extend this trait to add custom state tracking.
 */
trait GlobalCtx {

  /** The last lexem that was created. */
  var lastLexem: Lexem[?, ?] = compiletime.uninitialized

  /** The raw string that was matched for the last token. */
  var lastRawMatched: String = compiletime.uninitialized

  /** The remaining text to be tokenized. */
  var text: CharSequence
}

object GlobalCtx:

  /**
   * Automatic Copyable instance for any GlobalCtx that is a Product (case class).
   *
   * @tparam Ctx the context type
   */
  given [Ctx <: GlobalCtx & Product: Mirror.ProductOf]: Copyable[Ctx] =
    Copyable.derived

  /**
   * Default BetweenStages implementation that updates the context after each match.
   *
   * This implementation:
   * - Updates lastRawMatched with the matched text
   * - Creates a new Lexem for defined tokens
   * - Advances the text position
   * - Applies any context modifications
   */
  given BetweenStages[AnyGlobalCtx] =
    case (DefinedToken(info, modifyCtx, remapping), m, ctx) =>
      ctx.lastRawMatched = m.matched
      ctx.lastLexem = Lexem(info.name, remapping(ctx))
      ctx.text = ctx.text.from(m.end)
      modifyCtx(ctx)

    case (IgnoredToken(_, modifyCtx), m, ctx) =>
      ctx.text = ctx.text.from(m.end)
      modifyCtx(ctx)
