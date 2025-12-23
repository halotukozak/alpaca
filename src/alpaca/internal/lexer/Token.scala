package alpaca
package internal
package lexer

import java.util.concurrent.atomic.AtomicInteger
import scala.annotation.unchecked.uncheckedVariance as uv
import scala.annotation.compileTimeOnly
import scala.annotation.publicInBinary
import scala.util.matching.Regex

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
 * @tparam Name the token name type
 * @param name the token name
 * @param regexGroupName a unique name for the regex capture group
 * @param patterns the regex patterns or literals that matches this token
 */
private[lexer] final case class TokenInfo[+Name <: ValidName](
  name: Name,
  regexGroupName: String,
  patterns: Seq[String | Char],
):
  lazy val toEscapedRegex: String = patterns
    .map:
      case s: String => s
      case c: Char => Regex.quote(c.toString)
    .mkString("|")

  lazy val toRegex: String = patterns.mkString("|")

//todo: private[lexer]
object TokenInfo {
  private val counter = AtomicInteger(0)

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
   *
   * @note Do not change patterns type to Seq, it will make String => Seq[Char] conversion available
   */
  def unsafe(name: String, patterns: Vector[String | Char])(using quotes: Quotes): Expr[TokenInfo[?]] =
    import quotes.reflect.*
    ValidName.check(name)
    ConstantType(StringConstant(name)).asType match
      case '[type name <: ValidName; name] =>
        '{
          TokenInfo[name](
            ${ Expr(name).asExprOf[name] },
            ${ Expr(nextName()) },
            ${ Expr[Seq[String | Char]](patterns) },
          )
        }

  /**
   * Generates a unique name for a regex capture group.
   *
   * @return a unique token group name
   */
  private def nextName(): String = s"token${counter.getAndIncrement()}"

  /**
   * Given instance to extract TokenInfo from compile-time expressions.
   */
  given [name <: ValidName]: FromExpr[TokenInfo[name]] with
    def unapply(x: Expr[TokenInfo[name]])(using Quotes): Option[TokenInfo[name]] = x match
      case '{ TokenInfo($name, $regexGroupName, $patterns) } =>
        for
          name <- name.value
          regexGroupName <- regexGroupName.value
          patterns <- patterns.value
        yield TokenInfo(name.asInstanceOf[name], regexGroupName, patterns)
      case _ => None

  given [name <: ValidName: {Type}]: ToExpr[TokenInfo[name]] with
    def apply(x: TokenInfo[name])(using Quotes): Expr[TokenInfo[name]] =
      '{ TokenInfo[name](${ Expr[name](x.name) }, ${ Expr(x.regexGroupName) }, ${ Expr(x.patterns) }) }

  given ToExpr[String | Char] with
    def apply(x: String | Char)(using Quotes): Expr[String | Char] = x match
      case s: String => Expr(s)
      case c: Char => Expr(c)

  given FromExpr[String | Char] with
    def unapply(x: Expr[String | Char])(using Quotes): Option[String | Char] = x match
      case '{ $s: String } => s.value
      case '{ $c: Char } => c.value
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
sealed trait Token[+Name <: ValidName, +Ctx <: LexerCtx, +Value]:

  /** Token information including name and pattern. */
  val info: TokenInfo[Name]

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
  info: TokenInfo[Name],
  ctxManipulation: CtxManipulation[Ctx @uv],
  remapping: (Ctx @uv) => Value,
) extends Token[Name, Ctx, Value]:
  type LexemeTpe = Lexeme[Name, Value @uv]

  @compileTimeOnly(RuleOnly)
  inline def unapply(x: Any): Option[LexemeTpe] = dummy
  @compileTimeOnly(RuleOnly)
  inline def List: PartialFunction[Any, Option[List[LexemeTpe]]] = dummy
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
  info: TokenInfo[Name],
  ctxManipulation: CtxManipulation[Ctx @uv],
) extends Token[Name, Ctx, Nothing]
