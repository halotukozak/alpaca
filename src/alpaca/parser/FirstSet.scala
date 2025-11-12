package alpaca.parser

import alpaca.core.NonEmptyList
import alpaca.parser.Symbol.*
import scala.quoted.*
import scala.annotation.tailrec

/**
 * An opaque type representing FIRST sets for grammar symbols.
 *
 * The FIRST set of a symbol is the set of terminals that can appear
 * as the first symbol in a derivation from that symbol. This is used
 * in LR parser construction to determine lookaheads.
 */
opaque private[parser] type FirstSet = Map[NonTerminal, Set[Terminal]]

private[parser] object FirstSet {

  /**
   * Computes the FIRST sets for all non-terminals in a grammar.
   *
   * This uses a fixed-point algorithm to iteratively compute the
   * FIRST sets until they stabilize.
   *
   * @param productions the grammar productions
   * @return the computed FIRST sets
   */
  def apply(productions: List[Production]): FirstSet =
    loop(productions, Map.empty.withDefaultValue(Set.empty))

  @tailrec
  private def loop(productions: List[Production], firstSet: FirstSet): FirstSet =
    val newFirstSet = productions.foldLeft(firstSet)(addImports)
    if firstSet == newFirstSet then newFirstSet else loop(productions, newFirstSet)

  @tailrec
  private def addImports(firstSet: FirstSet, production: Production): FirstSet = production.runtimeChecked match
    case Production.NonEmpty(lhs, NonEmptyList(head: Terminal, tail), name) =>
      firstSet.updated(lhs, firstSet(lhs) + head)

    case Production.NonEmpty(lhs, NonEmptyList(head: NonTerminal { type IsEmpty = false }, tail), name) =>
      val newFirstSet = firstSet.updated(lhs, firstSet(lhs) ++ (firstSet(head) - Symbol.Empty))

      val production = tail match
        case head :: next => Production.NonEmpty(lhs, NonEmptyList(head, next*))
        case Nil => Production.Empty(lhs)

      if firstSet(head).contains(Symbol.Empty)
      then addImports(newFirstSet, production)
      else newFirstSet

    case Production.Empty(lhs, name) =>
      firstSet.updated(lhs, firstSet(lhs) + Symbol.Empty)

  /** Extension methods for FirstSet. */
  extension (firstSet: FirstSet)

    /**
     * Gets the FIRST set for a symbol.
     *
     * For terminals, this returns a singleton set containing the terminal itself.
     * For non-terminals, this returns the computed FIRST set.
     *
     * @param symbol the symbol to get the FIRST set for
     * @return the set of terminals that can appear first
     */
    def first(symbol: Symbol): Set[Terminal] = symbol match
      case t: Terminal => Set(t)
      case nt: NonTerminal => firstSet(nt)
}
