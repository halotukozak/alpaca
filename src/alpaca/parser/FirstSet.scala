package alpaca.parser

import alpaca.lexer.AlgorithmError
import alpaca.parser.Symbol.*

import scala.annotation.tailrec

opaque type FirstSet = Map[NonTerminal, Set[Terminal]]

object FirstSet {
  def apply(productions: List[Production]): FirstSet = loop(productions, Map.empty.withDefaultValue(Set.empty))

  @tailrec
  private def loop(productions: List[Production], firstSet: FirstSet): FirstSet =
    val newFirstSet = productions.foldLeft(firstSet)((acc, production) => addImports(acc, production))
    if firstSet == newFirstSet then newFirstSet else loop(productions, newFirstSet)

  @tailrec
  private def addImports(firstSet: FirstSet, production: Production): FirstSet = production match {
    case Production(lhs, (head: Terminal) +: tail) =>
      firstSet.union(lhs, head)

    case Production(lhs, (head: NonTerminal) +: tail) =>
      val newFirstSet = firstSet.union(lhs, firstSet(head))

      if firstSet(head).contains(Symbol.Empty)
      then addImports(newFirstSet, Production(lhs, tail))
      else newFirstSet

    case Production(lhs, _) =>
      firstSet.union(lhs, Symbol.Empty)
  }

  extension (firstSet: FirstSet) {
    def first(symbol: Symbol): Set[Terminal] = symbol match {
      case t: Terminal => Set(t)
      case nt: NonTerminal => firstSet.getOrElse(nt, Set.empty)
    }

    def union(nt: NonTerminal, value: Terminal): FirstSet = {
      val currentSet = firstSet.getOrElse(nt, Set.empty)
      val newSet = currentSet + value
      firstSet.updated(nt, newSet)
    }

    def union(nt: NonTerminal, values: Set[Terminal]): FirstSet = {
      val currentSet = firstSet.getOrElse(nt, Set.empty)
      val newSet = currentSet ++ (values - Symbol.Empty)
      firstSet.updated(nt, newSet)
    }
  }
}
