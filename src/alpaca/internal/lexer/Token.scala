package alpaca
package internal
package lexer

import java.util.concurrent.atomic.AtomicInteger
import scala.annotation.unchecked.uncheckedVariance as uv
import scala.annotation.compileTimeOnly
import scala.annotation.publicInBinary

import scala.language.experimental.modularity
import javax.naming.Name

import annotation.unchecked.uncheckedVariance as uv

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
private[lexer] final case class TokenInfo(
  tracked val name: ValidName,
  regexGroupName: String,
  pattern: String,
)

//todo: private[lexer]
object TokenInfo {
  private val counter = AtomicInteger(0)

  type AUX[Name <: ValidName] = TokenInfo { val name: Name }

  /**
   * Creates a TokenInfo expression from a name and regex pattern.
   *
   * This validates the name and constructs an expression that will
   * create a TokenInfo at runtime.
   *
   * @param name the token name
   * @param regex the regex pattern
   * @param quotes the Quotes instance
   * @return a TokenInfo expression
   */
  def unsafe(name: String, regex: String)(using quotes: Quotes): Expr[TokenInfo] =
    import quotes.reflect.*
    ValidName.check(name)
    ConstantType(StringConstant(name)).asType match
      case '[type nameTpe <: ValidName; nameTpe] =>
        '{ TokenInfo(${ Expr(name).asExprOf[nameTpe] }, ${ Expr(nextName()) }, ${ Expr(regex) }) }

  /**
   * Generates a unique name for a regex capture group.
   *
   * @return a unique token group name
   */
  private def nextName(): String = s"token${counter.getAndIncrement()}"

  /**
   * Given instance to extract TokenInfo from compile-time expressions.
   */
  given [Name <: ValidName]: FromExpr[TokenInfo.AUX[Name]] with
    def unapply(x: Expr[TokenInfo.AUX[Name]])(using Quotes): Option[TokenInfo.AUX[Name]] = x match
      case '{ TokenInfo($name, $regexGroupName: String, $pattern: String) } =>
        for
          name <- name.value
          regexGroupName <- regexGroupName.value
          pattern <- pattern.value
        yield TokenInfo(name.asInstanceOf[Name], regexGroupName, pattern)
      case _ => None

  given [Name <: ValidName: Type]: ToExpr[TokenInfo.AUX[Name]] with
    def apply(x: TokenInfo.AUX[Name])(using Quotes): Expr[TokenInfo.AUX[Name]] =
      '{ TokenInfo(${ Expr[Name](x.name) }, ${ Expr(x.regexGroupName) }, ${ Expr(x.pattern) }) }.asExprOf[TokenInfo.AUX[Name]]

  given [Name <: ValidName] => TokenInfo.AUX[Name] has Default = () => TokenInfo("".asInstanceOf[Name], "", "")
}

/**
 * Base trait for all token types.
 *
 * A token represents a lexical unit matched by the lexer. It contains information
 * about the token's name, pattern, and how to manipulate the lexer context when matched.
 *
 * @tparam Ctx the global context type
 * @tparam Name the token name
 */
sealed trait Token[+Ctx <: LexerCtx, +Name <: ValidName]:
  type Value

  /** Token information including name and pattern. */
  val info: TokenInfo.AUX[Name]

  /** Function to update the context when this token is matched. */
  val ctxManipulation: CtxManipulation[Ctx @uv]

/**
 * A token that produces a value when matched.
 *
 * This is the main token type used in the lexer. It can extract a value
 * from the matched text using a remapping function.
 *
 * @tparam Ctx the global context type
 * @tparam Name the token name
 * @tparam Value the token value type
 * @param info token information
 * @param ctxManipulation function to update context
 * @param remapping function to extract value from context
 */
//todo: may be invariant?
final case class DefinedToken[+Ctx <: LexerCtx, +Name <: ValidName, Value](
  info: TokenInfo.AUX[Name],
  ctxManipulation: CtxManipulation[Ctx @uv],
  remapping: (Ctx@uv) => Value,
) extends Token[Ctx, Name]:
  type LexemeTpe = Lexeme { val name: info.name.type; val value: Value }

  @compileTimeOnly(RuleOnly)
  inline def unapply(x: Any): Option[LexemeTpe] = dummy
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
 * @tparam Ctx the global context type
 * @param info token information
 * @param ctxManipulation function to update context
 */
final case class IgnoredToken[+Ctx <: LexerCtx, +Name <: ValidName](
  info: TokenInfo.AUX[Name],
  ctxManipulation: CtxManipulation[Ctx @uv],
) extends Token[Ctx, Name]:
  type Value = Nothing