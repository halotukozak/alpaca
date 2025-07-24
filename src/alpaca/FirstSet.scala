package alpaca

import scala.annotation.tailrec

final class FirstSet(productions: List[Production]) {
  private val firstSet = resolveGraph(dependenciesGraph(productions))

  private def dependenciesGraph(productions: List[Production]): Map[NonTerminal, FirstSet.Meta] =
    productions.foldLeft(Map.empty[NonTerminal, FirstSet.Meta].withDefaultValue(FirstSet.Meta.empty)) {
      case (acc, Production(lhs, head +: _)) =>
        acc.updated(lhs, acc(lhs).including(head))

      case (acc, production) =>
        throw AlgorithmError("Serving empty productions is not implemented yet")
    }

  @tailrec
  private def resolveGraph(graph: Map[NonTerminal, FirstSet.Meta]): Map[NonTerminal, FirstSet.Meta] = {
    val newGraph =
      graph.map((nt, meta) => (nt, meta.importsFrom.foldLeft(meta)((acc, nt) => acc.including(graph(nt).first))))
    if graph == newGraph then newGraph else resolveGraph(newGraph)
  }

  def first(symbol: Symbol): Set[Terminal] =
    symbol match {
      case t: Terminal => Set(t)
      case nt: NonTerminal => firstSet.getOrElse(nt, FirstSet.Meta.empty).first
    }
}

object FirstSet {
  private case class Meta(first: Set[Terminal], importsFrom: Set[NonTerminal]) {
    def including(symbol: Symbol): Meta = symbol match
      case t: Terminal => Meta(first + t, importsFrom)
      case nt: NonTerminal => Meta(first, importsFrom + nt)

    def including(terminals: Set[Terminal]): Meta =
      Meta(first ++ terminals, importsFrom)
  }

  private object Meta {
    val empty = Meta(Set.empty, Set.empty)
  }
}
