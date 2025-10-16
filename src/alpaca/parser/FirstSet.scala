package alpaca.parser

import alpaca.core.{raiseShouldNeverBeCalled, NonEmptyList}
import alpaca.parser.Symbol.*

import scala.annotation.tailrec

opaque private[parser] type FirstSet = Map[NonTerminal, Set[Terminal]]

private[parser] object FirstSet {
  def apply(productions: List[Production]): FirstSet = loop(productions, Map.empty.withDefaultValue(Set.empty))

  @tailrec
  private def loop(productions: List[Production], firstSet: FirstSet): FirstSet =
    val newFirstSet = productions.foldLeft(firstSet)(addImports)
    if firstSet == newFirstSet then newFirstSet else loop(productions, newFirstSet)

  @tailrec
  private def addImports(firstSet: FirstSet, production: Production): FirstSet = production match
    case Production(lhs, NonEmptyList(head: Terminal, tail)) =>
      firstSet.updated(lhs, firstSet(lhs) + head)

    case Production(lhs, NonEmptyList(head: NonTerminal, tail)) =>
      val newFirstSet = firstSet.updated(lhs, firstSet(lhs) ++ (firstSet(head) - Symbol.Empty))

      if firstSet(head).contains(Symbol.Empty)
      then
        tail match
          case Nil => firstSet.updated(lhs, firstSet(lhs) + Symbol.Empty)
          case head :: tail =>
            addImports(newFirstSet, Production(lhs, NonEmptyList(head, tail*)))
      else newFirstSet

    case x =>
      raiseShouldNeverBeCalled(x.toString)

  extension (firstSet: FirstSet)
    def first(symbol: Symbol): Set[Terminal] = symbol match
      case t: Terminal => Set(t)
      case nt: NonTerminal => firstSet(nt)
}
