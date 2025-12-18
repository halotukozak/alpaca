package alpaca

import alpaca.internal.*
import alpaca.internal.lexer.*

import java.util.jar.Attributes.Name
import scala.annotation.compileTimeOnly
import scala.NamedTuple.AnyNamedTuple
import scala.collection.mutable

/**
 * Creates a lexer from a DSL-based definition.
 *
 * This is the main entry point for defining a lexer. It uses a macro to
 * compile the lexer definition into efficient tokenization code.
 *
 * Example:
 * {{{
 * val myLexer = lexer {
 *   case "\\d+" => Token["number"]
 *   case "[a-zA-Z]+" => Token["identifier"]
 *   case "\\s+" => Token.Ignored
 * }
 * }}}
 *
 * @tparam Ctx the global context type, defaults to DefaultGlobalCtx
 * @param rules the lexer rules as a partial function
 * @param copy implicit Copyable instance for the context
 * @param betweenStages implicit BetweenStages for context updates
 * @return a Tokenization instance that can tokenize input strings
 */
transparent inline def lexer[Ctx <: LexerCtx](
  using Ctx withDefault LexerCtx.Default,
)(
  inline rules: Ctx ?=> LexerDefinition[Ctx],
)(using
  copy: Ctx is Copyable,
  empty: Ctx has Empty,
  betweenStages: Ctx has BetweenStages,
)(using inline
  debugSettings: DebugSettings,
): Tokenization[Ctx] =
  ${ lexerImpl[Ctx]('{ rules }, '{ copy }, '{ empty }, '{ betweenStages })(using '{ debugSettings }) }

/** Factory methods for creating token definitions in the lexer DSL. */
object Token {

  /**
   * Creates an ignored token that will be matched but not included in the output.
   *
   * This is compile-time only and should only be used inside lexer definitions.
   *
   * @param ctx the lexer context
   * @return a token that will be ignored
   */
  @compileTimeOnly("Should never be called outside the lexer definition")
  def Ignored(using ctx: LexerCtx): Token[ctx.type] { type Value = Nothing } = dummy

  /**
   * Creates a token that captures the matched string.
   *
   * This is compile-time only and should only be used inside lexer definitions.
   *
   * @tparam Name the token name
   * @param ctx the lexer context
   * @return a token definition
   */
  @compileTimeOnly("Should never be called outside the lexer definition")
  def apply[Name <: ValidName](using ctx: LexerCtx): Token[ctx.type] { type Value = String } & NamedToken[Name] = dummy

  /**
   * Creates a token with a custom value extractor.
   *
   * This is compile-time only and should only be used inside lexer definitions.
   *
   * @tparam Name the token name
   * @param value the value to extract from the match
   * @param ctx the lexer context
   * @return a token definition
   */
  @compileTimeOnly("Should never be called outside the lexer definition")
  def apply[Name <: ValidName](value: Any)(using ctx: LexerCtx)
    : Token[ctx.type] { type Value = value.type } & NamedToken[Name] =
    dummy
}

transparent inline given ctx(using c: LexerCtx): c.type = c

/**
 * Base trait for lexer global context.
 *
 * The global context maintains state during lexing, including the current
 * position in the input, the last matched token, and the remaining text to process.
 * Users can extend this trait to add custom state tracking.
 */
trait LexerCtx {

  /** The last lexeme that was created. */
  var lastLexeme: Lexeme | Null = compiletime.uninitialized

  /** The raw string that was matched for the last token. */
  var lastRawMatched: String = compiletime.uninitialized

  /** The remaining text to be tokenized. */
  var text: CharSequence
}

object LexerCtx:

  /**
   * Automatic Copyable instance for any GlobalCtx that is a Product (case class).
   *
   * @tparam Ctx the context type
   */
  given [Ctx <: LexerCtx & Product: Mirror.ProductOf] => Ctx is Copyable =
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
  given LexerCtx has BetweenStages =
    case (DefinedToken(info, modifyCtx, remapping), m, ctx) =>
      ctx.lastRawMatched = m.matched.nn
      val ctxAsProduct = ctx.asInstanceOf[Product]
      val fields = ctxAsProduct.productElementNames.zip(ctxAsProduct.productIterator).toMap +
        ("text" -> ctx.lastRawMatched)
      ctx.lastLexeme = Lexeme(info.name, remapping(ctx), fields)
      ctx.text = ctx.text.from(m.end)
      modifyCtx(ctx)

    case (IgnoredToken(_, modifyCtx), m, ctx) =>
      ctx.lastRawMatched = m.matched.nn
      ctx.text = ctx.text.from(m.end)
      modifyCtx(ctx)

  /**
   * An empty lexer context with no extra state tracking.
   *
   * This is the simplest context that only tracks the remaining text.
   * Use this when you don't need line or position tracking.
   *
   * @param text the remaining text to tokenize
   */
  final case class Empty(
    var text: CharSequence = "",
  ) extends LexerCtx

  /**
   * The default lexer context with position and line tracking.
   *
   * This context tracks:
   * - The remaining text to tokenize
   * - The current character position (1-based)
   * - The current line number (1-based)
   *
   * This is the most commonly used context and provides useful information
   * for error reporting.
   *
   * @param text the remaining text to tokenize
   * @param position the current character position (1-based)
   * @param line the current line number (1-based)
   */
  final case class Default(
    var text: CharSequence = "",
    var position: Int = 1,
    var line: Int = 1,
  ) extends LexerCtx
      with PositionTracking
      with LineTracking
