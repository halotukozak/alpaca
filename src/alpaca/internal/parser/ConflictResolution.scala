package alpaca
package internal
package parser

import alpaca.internal.lexer.Token

import scala.annotation.{compileTimeOnly, tailrec}
import scala.collection.mutable

/**
 * Type representing a key in the conflict resolution table.
 *
 * A conflict key can be either a Production or a String (token name).
 */
private[parser] type ConflictKey = Production | String

/**
 * Opaque type representing a table of conflict resolution rules.
 *
 * This maps each production/token to a set of productions/tokens that it has
 * precedence over.
 */
opaque private[parser] type ConflictResolutionTable = Map[ConflictKey, Set[ConflictKey]]

private[parser] object ConflictResolutionTable {

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
            val current = table.getOrElse(head, Set.empty)
            val neighbors = current.diff(visited)
            loop(tail ++ neighbors, visited + head)

        loop(List(extractProdOrName(first)), Set())
      }

      winsOver(first, second) orElse winsOver(second, first)
    }

    def verifyNoConflicts: Unit = {
      enum VisitState:
        case Unvisited
        case Visited
        case Processed

      val visited = mutable.Map.empty[ConflictKey, VisitState].withDefaultValue(VisitState.Unvisited)

      def visit(node: ConflictKey, path: List[ConflictKey] = Nil): Unit =
        visited(node) match
          case VisitState.Unvisited =>
            visited.update(node, VisitState.Visited)
            for neighbor <- table.getOrElse(node, Set.empty) do visit(neighbor, path :+ node)
            visited.update(node, VisitState.Processed)
          case VisitState.Visited =>
            throw InconsistentConflictResolution(node, path)
          case VisitState.Processed => // Already fully processed

      for node <- table.keys do visit(node)
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
