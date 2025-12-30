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
opaque private[parser] type ConflictKey = Production | String

object ConflictKey:
  inline def apply(key: Production | String): ConflictKey = key

  given Showable[ConflictKey] =
    case Production.NonEmpty(lhs, rhs, null) => show"Reduction(${rhs.mkShow(" ")} -> $lhs)"
    case Production.Empty(lhs, null) => show"Reduction(${Symbol.Empty} -> $lhs)"
    case p: Production =>
      p.name match
        case null => "Reduction(<unknown>)"
        case name: String => show"Reduction($name)"
    case s: String => show"Shift($s)"

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

    def verifyNoConflicts(): Unit = {
      enum VisitState:
        case Unvisited, Visited, Processed

      enum Action:
        case Enter(node: ConflictKey, path: List[ConflictKey] = Nil)
        case Leave(node: ConflictKey)

      val visited = mutable.Map.empty[ConflictKey, VisitState].withDefaultValue(VisitState.Unvisited)

      @tailrec
      def loop(stack: List[Action]): Unit = stack match
        case Nil => // Done

        case Action.Leave(node) :: rest =>
          visited(node) = VisitState.Processed
          loop(rest)

        case Action.Enter(node, path) :: rest =>
          visited(node) match
            case VisitState.Processed => loop(rest)
            case VisitState.Visited => throw InconsistentConflictResolution(node, path.reverse)
            case VisitState.Unvisited =>
              visited(node) = VisitState.Visited
              val neighbors = table.getOrElse(node, Set.empty).map(Action.Enter(_, node :: path)).toList
              loop(neighbors ::: List(Action.Leave(node)) ::: rest)

      for node <- table.keys do loop(Action.Enter(node) :: Nil)
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
