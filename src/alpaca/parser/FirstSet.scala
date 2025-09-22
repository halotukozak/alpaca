package alpaca.parser

import alpaca.core.raiseShouldNeverBeCalled
import alpaca.parser.Symbol.*

import scala.annotation.tailrec

opaque type FirstSet = Map[NonTerminal, Set[Terminal]]

object FirstSet {
  def apply(productions: List[Production]): FirstSet = loop(productions, Map.empty.withDefaultValue(Set.empty))

  @tailrec
  private def loop(productions: List[Production], firstSet: FirstSet): FirstSet =
    val newFirstSet = productions.foldLeft(firstSet)(addImports)
    if firstSet == newFirstSet then newFirstSet else loop(productions, newFirstSet)

  @tailrec
  private def addImports(firstSet: FirstSet, production: Production): FirstSet = production match {
    case Production(lhs, Seq(head: Terminal, tail*)) =>
      firstSet.updated(lhs, firstSet(lhs) + head)

    case Production(lhs, Seq(head: NonTerminal, tail*)) =>
      val newFirstSet = firstSet.updated(lhs, firstSet(lhs) ++ (firstSet(head) - Symbol.Empty))

      if firstSet(head).contains(Symbol.Empty)
      then addImports(newFirstSet, Production(lhs, tail))
      else newFirstSet

    case Production(lhs, Seq()) =>
      firstSet.updated(lhs, firstSet(lhs) + Symbol.Empty)

    case x =>
      raiseShouldNeverBeCalled(x.toString)
  }

  extension (firstSet: FirstSet)
    def first(symbol: Symbol): Set[Terminal] = symbol match {
      case t: Terminal => Set(t)
      case nt: NonTerminal => firstSet(nt)
    }
}
