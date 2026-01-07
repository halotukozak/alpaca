package alpaca
package internal
package lexer

import java.util.concurrent.atomic.AtomicInteger
import scala.annotation.unchecked.uncheckedVariance as uv
import scala.annotation.{compileTimeOnly, unused}

/**
 * Type alias for context manipulation functions.
 *
 * These functions are used to update the lexer context as tokens are matched.
 *
 * @tparam Ctx the global context type
 */
private[lexer] type CtxManipulation[Ctx <: LexerCtx] = Ctx => Unit

/**
 * Information about a token definition.
 *
 * Contains the token's name, pattern, and a unique group name for regex matching.
 *
 * @param name the token name
 * @param regexGroupName a unique name for the regex capture group
 * @param pattern the regex pattern that matches this token
 */
private[lexer] final case class TokenInfo private (name: String, regexGroupName: String, pattern: String)

private[lexer] object TokenInfo:
  private val counter = AtomicInteger(0)

  /**
   * Creates a TokenInfo expression from a name and regex pattern.
   *
   * This validates the name and constructs an expression that will
   * create a TokenInfo at runtime.
   *
   * @param name the token name
   * @param pattern the regex pattern
   * @param quotes the Quotes instance
   * @return a TokenInfo expression
   */
  def apply(name: String, pattern: String)(using quotes: Quotes): (Type[? <: ValidName], TokenInfo) =
    import quotes.reflect.*
    ValidName.check(name)
    (
      ConstantType(StringConstant(name)).asType.asInstanceOf[Type[? <: ValidName]],
      TokenInfo(name, nextRegexGroupName(), pattern),
    )

  /**
   * Generates a unique name for a regex capture group.
   *
   * @return a unique token group name
   */
  private def nextRegexGroupName(): String = s"token${counter.getAndIncrement()}"

  given Default[TokenInfo] = () => TokenInfo("", "", "")

  given ToExpr[TokenInfo]:
    def apply(x: TokenInfo)(using Quotes): Expr[TokenInfo] =
      '{ TokenInfo(${ Expr(x.name) }, ${ Expr(x.regexGroupName) }, ${ Expr(x.pattern) }) }

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
sealed trait Token[+Name <: ValidName, +Ctx <: LexerCtx, +Value]:

  /** Token information including name and pattern. */
  val info: TokenInfo

  /** Function to update the context when this token is matched. */
  val ctxManipulation: CtxManipulation[Ctx @uv]

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
final case class DefinedToken[Name <: ValidName, +Ctx <: LexerCtx, +Value](
  info: TokenInfo,
  ctxManipulation: CtxManipulation[Ctx @uv],
  remapping: (Ctx @uv) => Value,
) extends Token[Name, Ctx, Value]:
  type LexemeTpe <: Lexeme[Name, Value @uv] // & LexemeRefinement

  @compileTimeOnly(RuleOnly)
  inline def unapply(@unused x: Any): Option[LexemeTpe] = dummy
  @compileTimeOnly(RuleOnly)
  inline def List: PartialFunction[Any, List[LexemeTpe]] = dummy
  @compileTimeOnly(RuleOnly)
  inline def Option: PartialFunction[Any, Option[LexemeTpe]] = dummy

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
final case class IgnoredToken[Name <: ValidName, +Ctx <: LexerCtx](
  info: TokenInfo,
  ctxManipulation: CtxManipulation[Ctx @uv],
) extends Token[Name, Ctx, Nothing]
