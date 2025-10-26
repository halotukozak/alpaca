package alpaca
package parser

import alpaca.core.*
import alpaca.lexer.{AlgorithmError, Token}
import scala.collection.mutable
import alpaca.core.Showable.mkShow
import scala.annotation.compileTimeOnly
import alpaca.lexer.Token

type ConflictResolution

extension (first: Production | Token[?, ?, ?])
  @compileTimeOnly(RuleOnly)
  inline infix def after(second: (Production | Token[?, ?, ?])*): ConflictResolution = dummy
  @compileTimeOnly(RuleOnly)
  inline infix def before(second: (Production | Token[?, ?, ?])*): ConflictResolution = dummy

opaque type ConflictResolutionTable = Map[Production | String, Set[Production | String]]

object ConflictResolutionTable {
  def apply(resolutions: Map[Production | String, Set[Production | String]]): ConflictResolutionTable = resolutions

  extension (table: ConflictResolutionTable)
    def get(first: ParseAction, second: ParseAction)(symbol: Symbol): Option[ParseAction] = {
      def extractProdOrName(action: ParseAction): Production | String = action.runtimeChecked match
        case red: ParseAction.Reduction => red.production
        case _: ParseAction.Shift => symbol.name

      if relationExists(extractProdOrName(first), extractProdOrName(second)) then Some(first)
      else if relationExists(extractProdOrName(second), extractProdOrName(first)) then Some(second)
      else None
    }

    def relationExists(from: Production | String, to: Production | String): Boolean = {
      val visited = mutable.Set[Production | String](from)
      val queue = mutable.Queue(from)

      while queue.nonEmpty do
        val current = queue.dequeue()
        if current == to then return true

        table.get(current) match
          case Some(neighbors) =>
            neighbors.foreach: neighbor =>
              if !visited.contains(neighbor) then
                visited.add(neighbor)
                queue.enqueue(neighbor)
          case None => ()

      false
    }

  given Showable[ConflictResolutionTable] =
    _.map: (k, v) =>
      def show(x: Production | String): String = x match
        case p: Production => show"$p"
        case s: String => show"Token[$s]"

      show"${show(k)} before ${v.map(show).mkShow(", ")}"
    .mkShow("\n")
}
