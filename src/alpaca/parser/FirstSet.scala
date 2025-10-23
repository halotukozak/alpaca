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
  private def addImports(firstSet: FirstSet, production: Production): FirstSet = production match {
    case Production.NonEmpty(lhs, NonEmptyList(head: Terminal, tail)) =>
      firstSet.updated(lhs, firstSet(lhs) + head)

    case Production.NonEmpty(lhs, NonEmptyList(head: NonTerminal, tail)) =>
      val newFirstSet = firstSet.updated(lhs, firstSet(lhs) ++ (firstSet(head) - Symbol.Empty))

      val production = tail match
        case head :: next => Production.NonEmpty(lhs, NonEmptyList(head, next*))
        case Nil => Production.Empty(lhs)

      if firstSet(head).contains(Symbol.Empty)
      then addImports(newFirstSet, production)
      else newFirstSet

    case Production.Empty(lhs) =>
      firstSet.updated(lhs, firstSet(lhs) + Symbol.Empty)

    case x =>
      raiseShouldNeverBeCalled(x.toString)
  }

  extension (firstSet: FirstSet)
    def first(symbol: Symbol): Set[Terminal] = symbol match
      case t: Terminal => Set(t)
      case nt: NonTerminal => firstSet(nt)
}
