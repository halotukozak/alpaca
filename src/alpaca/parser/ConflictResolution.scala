package alpaca
package parser

import alpaca.core.*
import alpaca.lexer.{AlgorithmError, Token}
import scala.collection.mutable
import alpaca.core.Showable.mkShow
import scala.annotation.compileTimeOnly
import scala.annotation.tailrec

type ConflictResolution

type ConflictKey = Production | String

extension (first: Production | Token[?, ?, ?])
  @compileTimeOnly(RuleOnly)
  inline infix def after(second: (Production | Token[?, ?, ?])*): ConflictResolution = dummy
  @compileTimeOnly(RuleOnly)
  inline infix def before(second: (Production | Token[?, ?, ?])*): ConflictResolution = dummy

opaque type ConflictResolutionTable = Map[ConflictKey, Set[ConflictKey]]

object ConflictResolutionTable {
  def apply(resolutions: Map[ConflictKey, Set[ConflictKey]]): ConflictResolutionTable = resolutions

  extension (table: ConflictResolutionTable)
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

  given Showable[ConflictResolutionTable] =
    _.map: (k, v) =>
      def show(x: ConflictKey): String = x match
        case p: Production => show"$p"
        case s: String => show"Token[$s]"

      show"${show(k)} before ${v.map(show).mkShow(", ")}"
    .mkShow("\n")
}
