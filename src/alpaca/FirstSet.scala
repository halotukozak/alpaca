package alpaca

import scala.annotation.tailrec

class FirstSet(productions: List[Production]) {
  private case class Meta(first: Set[Terminal], importsFrom: Set[NonTerminal]) {
    def add(symbol: Symbol): Meta =
      symbol match {
        case t: Terminal => Meta(first + t, importsFrom)
        case nt: NonTerminal => Meta(first, importsFrom + nt)
      }

    def add(nonTerminals: Set[Terminal]): Meta =
      Meta(first ++ nonTerminals, importsFrom)
  }

  private object Meta {
    def empty(): Meta = Meta(Set.empty, Set.empty)
  }

  private val firstSet = resolveGraph(dependenciesGraph(productions))

  private def dependenciesGraph(productions: List[Production]): Map[NonTerminal, Meta] =
    productions.foldLeft(Map.empty[NonTerminal, Meta]) {
      case (acc, Production(lhs, head :: _)) =>
        acc.updatedWith(lhs) {
          case Some(meta) => Some(meta.add(head))
          case None => Some(Meta.empty().add(head))
        }

      case (acc, production) => 
        throw AlgorithmError("Serving empty productions is not implemented yet")
    }

  @tailrec
  private def resolveGraph(graph: Map[NonTerminal, Meta]): Map[NonTerminal, Meta] = {
    val newGraph = graph.map((nt, meta) => (nt, meta.importsFrom.foldLeft(meta)((acc, nt) => acc.add(graph(nt).first))))
    if graph == newGraph then newGraph else resolveGraph(newGraph)
  }

  def first(symbol: Symbol): Set[Terminal] =
    symbol match {
      case t: Terminal => Set(t)
      case nt: NonTerminal => firstSet.getOrElse(nt, Meta.empty()).first
    }
}
