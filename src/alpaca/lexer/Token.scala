package alpaca.lexer

import alpaca.lexer.context.{AnyGlobalCtx, GlobalCtx, Lexem}

import java.util.concurrent.atomic.AtomicInteger
import scala.annotation.compileTimeOnly
import scala.annotation.unchecked.uncheckedVariance as uv
import scala.quoted.*

/** Type alias for valid token names.
  *
  * Token names must be singleton strings (string literals) to enable
  * compile-time type safety.
  */
type ValidName = String & Singleton

/** Type alias for context manipulation functions.
  *
  * These functions are used to update the lexer context as tokens are matched.
  *
  * @tparam Ctx the global context type
  */
type CtxManipulation[Ctx <: AnyGlobalCtx] = Ctx => Unit

/** Information about a token definition.
  *
  * Contains the token's name, pattern, and a unique group name for regex matching.
  *
  * @tparam Name the token name type
  * @param name the token name
  * @param regexGroupName a unique name for the regex capture group
  * @param pattern the regex pattern that matches this token
  */
final case class TokenInfo[+Name <: ValidName] private (name: Name, regexGroupName: String, pattern: String)

object TokenInfo {
  private val counter = AtomicInteger(0)

  /** Creates a new TokenInfo with an auto-generated regex group name.
    *
    * @param name the token name
    * @param pattern the regex pattern
    * @return a new TokenInfo instance
    */
  def apply[Name <: ValidName](name: Name, pattern: String): TokenInfo[Name] =
    TokenInfo(name, s"token${counter.getAndIncrement()}", pattern)

  given FromExpr[TokenInfo[?]] with
    def unapply(x: Expr[TokenInfo[?]])(using Quotes): Option[TokenInfo[?]] = x match
      case '{ TokenInfo($name: ValidName, $pattern: String) } =>
        for
          name <- name.value
          pattern <- pattern.value
        yield TokenInfo(name, pattern)
      case _ => None
}

/** Base trait for all token types.
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
  
  /** Creates an ignored token that will be matched but not included in the output.
    *
    * This is compile-time only and should only be used inside lexer definitions.
    *
    * @param ctx the lexer context
    * @return a token that will be ignored
    */
  @compileTimeOnly("Should never be called outside the lexer definition")
  def Ignored(using ctx: AnyGlobalCtx): Token[?, ctx.type, Nothing] = ???

  /** Creates a token that captures the matched string.
    *
    * This is compile-time only and should only be used inside lexer definitions.
    *
    * @tparam Name the token name
    * @param ctx the lexer context
    * @return a token definition
    */
  @compileTimeOnly("Should never be called outside the lexer definition")
  def apply[Name <: ValidName](using ctx: AnyGlobalCtx): Token[Name, ctx.type, String] = ???

  /** Creates a token with a custom value extractor.
    *
    * This is compile-time only and should only be used inside lexer definitions.
    *
    * @tparam Name the token name
    * @param value the value to extract from the match
    * @param ctx the lexer context
    * @return a token definition
    */
  @compileTimeOnly("Should never be called outside the lexer definition")
  def apply[Name <: ValidName](value: Any)(using ctx: AnyGlobalCtx): Token[Name, ctx.type, value.type] = ???
}

/** A token that produces a value when matched.
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
  
  /** Pattern matching extractor for use in parser definitions.
    *
    * This is compile-time only and should only be used inside parser definitions.
    *
    * @param lexem the lexem to match
    * @return Some(lexem) if the lexem matches this token, None otherwise
    */
  // todo: find a better way to handle Value = Unit to avoid CalcLexer.PLUS(()) or CalcLexer.PLUS(_)
  @compileTimeOnly("Should never be called outside the parser definition")
  inline def unapply(lexem: Lexem[?, ?]): Option[Lexem[Name, Value]] = ???
}

/** A token that is matched but not included in the output.
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
