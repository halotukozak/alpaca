package alpaca.lexer

import alpaca.core.{dummy, ValidName}
import alpaca.lexer.context.{AnyGlobalCtx, Lexem}

import java.util.concurrent.atomic.AtomicInteger
import scala.annotation.compileTimeOnly
import scala.annotation.unchecked.uncheckedVariance as uv
import scala.quoted.*

/**
 * Type alias for context manipulation functions.
 *
 * These functions are used to update the lexer context as tokens are matched.
 *
 * @tparam Ctx the global context type
 */
private[lexer] type CtxManipulation[Ctx <: AnyGlobalCtx] = Ctx => Unit

/**
 * Information about a token definition.
 *
 * Contains the token's name, pattern, and a unique group name for regex matching.
 *
 * @tparam Name the token name type
 * @param name the token name
 * @param regexGroupName a unique name for the regex capture group
 * @param pattern the regex pattern that matches this token
 */
private[lexer] final case class TokenInfo[+Name <: ValidName](
  name: Name,
  regexGroupName: String,
  pattern: String,
)

/**
 * Companion object providing utilities for TokenInfo.
 */
//todo: why it cannot be private[lexer]
object TokenInfo {
  private val counter = AtomicInteger(0)

  /**
   * Generates a unique name for a regex capture group.
   *
   * @return a unique token group name
   */
  private[lexer] def nextName(): String = s"token${counter.getAndIncrement()}"

  /**
   * Given instance to extract TokenInfo from compile-time expressions.
   */
  given FromExpr[TokenInfo[?]] with
    def unapply(x: Expr[TokenInfo[?]])(using Quotes): Option[TokenInfo[?]] = x match
      case '{ type name <: ValidName; TokenInfo($name: name, $regexGroupName: String, $pattern: String) } =>
        for
          name <- name.value
          regexGroupName <- regexGroupName.value
          pattern <- pattern.value
        yield TokenInfo(name, regexGroupName, pattern)
      case _ => None
}

/**
 * Base trait for all token types.
 *
 * A token represents a lexical unit matched by the lexer. It contains information
 * about the token's name, pattern, and how to manipulate the lexer context when matched.
 *
 * @tparam Name the token name type
 * @tparam Ctx the global context type
 * @tparam Value the value type extracted from the matched text
 */
sealed trait Token[Name <: ValidName, +Ctx <: AnyGlobalCtx, Value] {

  /** Token information including name and pattern. */
  val info: TokenInfo[Name]

  /** Function to update the context when this token is matched. */
  val ctxManipulation: CtxManipulation[Ctx @uv]
}

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
  def Ignored(using ctx: AnyGlobalCtx): Token[?, ctx.type, Nothing] = dummy

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
  def apply[Name <: ValidName](using ctx: AnyGlobalCtx): Token[Name, ctx.type, String] = dummy

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
  def apply[Name <: ValidName](value: Any)(using ctx: AnyGlobalCtx): Token[Name, ctx.type, value.type] = dummy
}

/**
 * A token that produces a value when matched.
 *
 * This is the main token type used in the lexer. It can extract a value
 * from the matched text using a remapping function.
 *
 * @tparam Name the token name type
 * @tparam Ctx the global context type
 * @tparam Value the value type to extract
 * @param info token information
 * @param ctxManipulation function to update context
 * @param remapping function to extract value from context
 */
//todo: may be invariant?
final case class DefinedToken[Name <: ValidName, +Ctx <: AnyGlobalCtx, Value](
  info: TokenInfo[Name],
  ctxManipulation: CtxManipulation[Ctx @uv],
  remapping: (Ctx @uv) => Value,
) extends Token[Name, Ctx, Value] {
  type LexemTpe = Lexem[Name, Value]
}

/**
 * A token that is matched but not included in the output.
 *
 * Ignored tokens are useful for whitespace, comments, and other lexical
 * elements that should be recognized but not passed to the parser.
 *
 * @tparam Name the token name type
 * @tparam Ctx the global context type
 * @param info token information
 * @param ctxManipulation function to update context
 */
final case class IgnoredToken[Name <: ValidName, +Ctx <: AnyGlobalCtx](
  info: TokenInfo[Name],
  ctxManipulation: CtxManipulation[Ctx @uv],
) extends Token[Name, Ctx, Nothing]
