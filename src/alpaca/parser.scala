package alpaca

import alpaca.internal.*
import alpaca.internal.lexer.{Lexeme, Token}
import alpaca.internal.parser.*

import scala.annotation.{compileTimeOnly, StaticAnnotation}
import scala.deriving.Mirror

type Parser[Ctx <: ParserCtx] = alpaca.internal.parser.Parser[Ctx]

/**
 * Creates a grammar rule from one or more productions.
 *
 * This is the main way to define grammar rules in the parser DSL. Each production
 * is a partial function that matches a pattern of symbols (terminals and non-terminals)
 * and produces a result value.
 *
 * This is compile-time only and should only be used inside parser class definitions.
 *
 * Example:
 * {{{
 * val expr: Rule[Int] = rule(
 *   { case (a @ number(), "+", b @ number()) => a.toInt + b.toInt },
 *   { case (n @ number()) => n.toInt }
 * )
 * }}}
 *
 * @tparam R the result type produced by this rule
 * @param productions one or more productions that define this rule
 * @return a Rule instance
 */
@compileTimeOnly(ParserOnly)
inline def rule[R](productions: PartialFunction[Tuple | Lexeme[?, ?], R]*): Rule[R] = dummy

/**
 * Represents a grammar rule in the parser.
 *
 * A rule defines how a non-terminal symbol can be parsed by specifying
 * one or more productions. Each production maps a pattern of symbols
 * to a result value.
 *
 * Rules are created using the `rule` function and can be used in pattern
 * matching within parser productions.
 *
 * @tparam R the type of value produced when this rule is matched
 */
trait Rule[R]:

  /**
   * Pattern matching extractor for single occurrences of this rule.
   *
   * This is compile-time only and should only be used in parser rule definitions.
   *
   * @param x the value to match
   * @return Some(result) if the match succeeds
   */
  @compileTimeOnly(RuleOnly)
  inline def unapply(x: Any): Option[R] = dummy

  /**
   * Pattern matching extractor for lists of this rule.
   *
   * This is compile-time only and should only be used in parser rule definitions.
   *
   * @return a partial function that extracts a list of results
   */
  @compileTimeOnly(RuleOnly)
  inline def List: PartialFunction[Any, List[R]] = dummy

  /**
   * Pattern matching extractor for optional occurrences of this rule.
   *
   * This is compile-time only and should only be used in parser rule definitions.
   *
   * @return a partial function that extracts an optional result
   */
  @compileTimeOnly(RuleOnly)
  inline def Option: PartialFunction[Any, Option[R]] = dummy

/**
 * Base trait for parser global context.
 *
 * Unlike the lexer, the parser's global context is typically empty by default,
 * but can be extended to track custom state during parsing such as symbol tables,
 * type information, or other semantic data.
 */
trait ParserCtx

final class name(name: ValidName) extends StaticAnnotation

/**
 * Type representing conflict resolution rules for the parser.
 *
 * Conflict resolutions are used to resolve shift/reduce and reduce/reduce conflicts
 * in the parsing table by specifying precedence relationships between productions
 * and tokens.
 */
type ConflictResolution
extension (first: Production | Token[?, ?, ?])
  /**
   * Specifies that this production/token should have higher precedence than others.
   *
   * This is compile-time only and should only be used inside parser rule definitions.
   *
   * Example:
   * {{{
   * Production(expr, "*", expr) after Production(expr, "+", expr)
   * }}}
   *
   * @param second the productions/tokens that should have lower precedence
   * @return a conflict resolution rule
   */
  @compileTimeOnly(RuleOnly)
  inline infix def after(second: (Production | Token[?, ?, ?])*): ConflictResolution = dummy

  /**
   * Specifies that this production/token should have lower precedence than others.
   *
   * This is compile-time only and should only be used inside parser rule definitions.
   *
   * Example:
   * {{{
   * Production(expr, "+", expr) before Production(expr, "*", expr)
   * }}}
   *
   * @param second the productions/tokens that should have higher precedence
   * @return a conflict resolution rule
   */
  @compileTimeOnly(RuleOnly)
  inline infix def before(second: (Production | Token[?, ?, ?])*): ConflictResolution = dummy

object Production:

  /**
   * Creates a production reference from symbols.
   *
   * This is compile-time only and used in conflict resolution definitions
   * to refer to productions by their right-hand side.
   *
   * @param symbols the symbols on the right-hand side of the production
   * @return a production reference
   */
  @compileTimeOnly(ConflictResolutionOnly)
  inline def apply(inline symbols: (Rule[?] | Token[?, ?, ?])*): Production = dummy

  /**
   * Creates a production reference from a name.
   *
   * This is compile-time only and used in conflict resolution definitions
   * to refer to named productions.
   *
   * @param name the name of the production
   * @return a production reference
   */
  @compileTimeOnly(ConflictResolutionOnly)
  inline def ofName(name: ValidName): Production = dummy

object ParserCtx:

  /**
   * Automatic Copyable instance for any GlobalCtx that is a Product (case class).
   *
   * @tparam Ctx the context type
   */
  given [Ctx <: ParserCtx & Product: Mirror.ProductOf]: Copyable[Ctx] =
    Copyable.derived

  /**
   * An empty parser context with no state.
   *
   * This is the default context used by parsers when no custom context
   * is needed. Most simple parsers can use this.
   */
  final case class Empty(
  ) extends ParserCtx

extension [Ctx <: ParserCtx](parser: Parser[Ctx])

  /**
   * Parses a list of lexems using the defined grammar.
   *
   * This is a convenience method that infers the result type from the root rule.
   *
   * @param lexems the list of lexems to parse
   * @param debugSettings parser settings (optional)
   * @return a tuple of (context, result), where result may be null on parse failure
   */
  inline def parse(lexems: List[Lexeme[?, ?]])(using inline debugSettings: DebugSettings): (
    ctx: Ctx,
    result: (parser.root.type match
      case Rule[t] => t
    ) | Null,
  ) = parser.unsafeParse(lexems)
