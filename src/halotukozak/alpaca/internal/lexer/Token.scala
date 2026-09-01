package halotukozak
package alpaca
package internal
package lexer

import halotukozak.alpaca.internal.{Default, RuleOnly, Showable, ValidName}
import halotukozak.alpaca.{LexerCtx, SepValue}

import java.util.concurrent.atomic.AtomicInteger
import scala.annotation.unchecked.uncheckedVariance as uv
import scala.annotation.{compileTimeOnly, publicInBinary, unused}
import scala.quoted.{Quotes, ToExprFactory}

/**
 * Type alias for context manipulation functions.
 *
 * These functions are used to update the lexer context as tokens are matched.
 * The updated context is returned rather than mutated in place, so that user
 * contexts can be immutable `case class`es: the `lexer` macro rewrites every
 * `ctx.field = ...` / `ctx.field += ...` inside a rule into a `copy` and threads
 * the result through here. Contexts that still declare `var` fields keep working
 * unchanged (the assignment mutates in place and the same instance is returned).
 *
 * @tparam Ctx the global context type
 */
private[lexer] type CtxManipulation[Ctx <: LexerCtx] = Ctx => Ctx

/**
 * Information about a token definition.
 *
 * Contains the token's name, pattern, and a unique group name for regex matching.
 *
 * @param name the token name
 * @param regexGroupName a unique name for the regex capture group
 * @param pattern the regex pattern that matches this token
 */
//todo: should it contain info about ignored? for perf? https://github.com/halotukozak/alpaca/issues/231
private[lexer] final case class TokenInfo(name: String, regexGroupName: String, pattern: String) derives ToExprFactory

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
// $COVERAGE-OFF$
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

  given Showable[TokenInfo] = Showable.fromToString
// $COVERAGE-ON$
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
  @publicInBinary
  private[alpaca] val info: TokenInfo

  /** Function to update the context when this token is matched. */
  private[lexer] val ctxManipulation: CtxManipulation[Ctx @uv]

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
// Ctx must stay covariant (see #234): the `lexer` macro constructs each token as
// `Token[Name, ctx.type, Value]` and then widens them all into a single
// `List[Token[?, Ctx, ?]]` (see Lexer.scala). That widening only type-checks if Ctx is
// covariant. The `@uv` annotations below are safe despite Ctx also appearing in argument
// position (`ctxManipulation`, `remapping`): `ctx.type` is a compile-time-only device to
// tag which lexer a token belongs to — at runtime there is exactly one Ctx instance per
// lexer, so the variance escape hatch is never actually exercised unsoundly.
private[alpaca] final case class DefinedToken[Name <: ValidName, +Ctx <: LexerCtx, +Value](
  @publicInBinary private[alpaca] info: TokenInfo,
  private[lexer] ctxManipulation: CtxManipulation[Ctx @uv],
  private[lexer] remapping: (Ctx @uv) => Value,
) extends Token[Name, Ctx, Value]:
  type LexemeTpe <: Lexeme[Name, Value @uv] // & LexemeRefinement

  @compileTimeOnly(RuleOnly)
  inline def unapply(@unused x: Any): Option[LexemeTpe] = null.asInstanceOf[Option[LexemeTpe]]
  @compileTimeOnly(RuleOnly)
  inline def List: PartialFunction[Any, List[LexemeTpe]] = null.asInstanceOf[PartialFunction[Any, List[LexemeTpe]]]
  @compileTimeOnly(RuleOnly)
  inline def Option: PartialFunction[Any, Option[LexemeTpe]] = null.asInstanceOf[PartialFunction[Any, Option[LexemeTpe]]]
  @compileTimeOnly(RuleOnly)
  inline def SeparatedBy[Separator]: PartialFunction[Any, List[LexemeTpe | SepValue[Separator]]] =
    null.asInstanceOf[PartialFunction[Any, List[LexemeTpe | SepValue[Separator]]]]

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
private[alpaca] final case class IgnoredToken[Name <: ValidName, +Ctx <: LexerCtx](
  @publicInBinary private[alpaca] info: TokenInfo,
  private[lexer] ctxManipulation: CtxManipulation[Ctx @uv],
) extends Token[Name, Ctx, Nothing]

private[alpaca] def RecoveredToken[Ctx <: LexerCtx](matched: String): IgnoredToken[matched.type, Ctx] =
  IgnoredToken(TokenInfo(matched, s"<unrecognized \"$matched\">", matched), identity)
