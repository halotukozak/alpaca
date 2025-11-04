package alpaca
package parser

import alpaca.core.*
import alpaca.lexer.{AlgorithmError, Token}
import scala.collection.mutable
import alpaca.core.Showable.mkShow
import scala.annotation.compileTimeOnly
import scala.annotation.tailrec

/**
 * Type representing conflict resolution rules for the parser.
 *
 * Conflict resolutions are used to resolve shift/reduce and reduce/reduce conflicts
 * in the parsing table by specifying precedence relationships between productions
 * and tokens.
 */
type ConflictResolution

/**
 * Type representing a key in the conflict resolution table.
 *
 * A conflict key can be either a Production or a String (token name).
 */
type ConflictKey = Production | String

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

/**
 * Opaque type representing a table of conflict resolution rules.
 *
 * This maps each production/token to a set of productions/tokens that it has
 * precedence over.
 */
opaque type ConflictResolutionTable = Map[ConflictKey, Set[ConflictKey]]

object ConflictResolutionTable {

  /**
   * Creates a ConflictResolutionTable from a map of resolutions.
   *
   * @param resolutions the resolution map
   * @return a new ConflictResolutionTable
   */
  def apply(resolutions: Map[ConflictKey, Set[ConflictKey]]): ConflictResolutionTable = resolutions

  extension (table: ConflictResolutionTable)
    /**
     * Resolves a conflict between two parse actions.
     *
     * Uses the precedence rules in the table to determine which action
     * should be preferred. Returns None if no resolution rule applies.
     *
     * @param first the first parse action
     * @param second the second parse action
     * @param symbol the symbol causing the conflict
     * @return Some(action) if one action has precedence, None otherwise
     */
    def get(first: ParseAction, second: ParseAction)(symbol: Symbol): Option[ParseAction] = {
      def extractProdOrName(action: ParseAction): ConflictKey = action.runtimeChecked match
        case red: ParseAction.Reduction => red.production
        case _: ParseAction.Shift => symbol.name

      def winsOver(first: ParseAction, second: ParseAction): Option[ParseAction] = {
        val to = extractProdOrName(second)

        @tailrec
        def loop(queue: List[ConflictKey], visited: Set[ConflictKey]): Option[ParseAction] = queue match
          case Nil => None
          case `to` :: _ => Some(first)
          case head :: tail =>
            val current = table.get(head).getOrElse(Set.empty)
            val neighbors = current.filterNot(visited.contains)
            loop(tail ++ neighbors, visited + head)

        loop(List(extractProdOrName(first)), Set())
      }

      winsOver(first, second) orElse winsOver(second, first)
    }

  /**
   * Showable instance for displaying conflict resolution tables.
   */
  given Showable[ConflictResolutionTable] =
    _.map: (k, v) =>
      def show(x: ConflictKey): String = x match
        case p: Production => show"$p"
        case s: String => show"Token[$s]"

      show"${show(k)} before ${v.map(show).mkShow(", ")}"
    .mkShow("\n")
}
