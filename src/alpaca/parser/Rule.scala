package alpaca
package parser

import alpaca.core.dummy
import alpaca.lexer.context.Lexem

import scala.annotation.compileTimeOnly

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
 * @tparam Result the type of value produced when this rule is matched
 */
trait Rule[Result] {
  /**
   * Pattern matching extractor for single occurrences of this rule.
   *
   * This is compile-time only and should only be used in parser rule definitions.
   *
   * @param x the value to match
   * @return Some(result) if the match succeeds
   */
  @compileTimeOnly(RuleOnly)
  inline def unapply(x: Any): Option[Result] = dummy

  /**
   * Pattern matching extractor for lists of this rule.
   *
   * This is compile-time only and should only be used in parser rule definitions.
   *
   * @return a partial function that extracts a list of results
   */
  @compileTimeOnly(RuleOnly)
  inline def List: PartialFunction[Any, List[Result]] = dummy

  /**
   * Pattern matching extractor for optional occurrences of this rule.
   *
   * This is compile-time only and should only be used in parser rule definitions.
   *
   * @return a partial function that extracts an optional result
   */
  @compileTimeOnly(RuleOnly)
  inline def Option: PartialFunction[Any, Option[Result]] = dummy
}

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
inline def rule[R](productions: PartialFunction[Tuple | Lexem[?, ?], R]*): Rule[R] = dummy
