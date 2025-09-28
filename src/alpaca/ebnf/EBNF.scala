package alpaca.ebnf

import alpaca.core.raiseShouldNeverBeCalled
import alpaca.parser.{Symbol, Production as BNF}
import alpaca.parser.Symbol.*

import scala.util.Random

enum EBNF:
  case Definition(lhs: NonTerminal, rhs: EBNF)
  case Concatenation(items: List[EBNF])
  case Alternation(options: Set[EBNF])
  case Optional(node: EBNF)
  case Identifier(symbol: Symbol)
  case ZeroOrMore(node: EBNF)
  case OneOrMore(node: EBNF)

object EBNF:

  /**
   * Convert this EBNF AST to a list of BNF productions.
   *
   * Conversions follow the rules:
   * - { E }  -> X = ε | X E
   * - [ E ]  -> X = ε | E
   * - ( E )  -> X = E            (handled by introducing a fresh non-terminal when an expression must be a single symbol)
   * - E | E' -> several productions with the same LHS
   */
  extension (definition: Definition) def toBNF: List[BNF] = expand(definition.rhs, definition.lhs)

  private def freshNonTerminal(): NonTerminal =
    NonTerminal(s"Fresh${Random.alphanumeric.take(8).mkString}")

  // Expand an expression into productions for a specific non-terminal target
  private def expand(expr: EBNF, target: NonTerminal): List[BNF] = expr match
    case Identifier(symbol) =>
      List(BNF(target, List(symbol)))

    case Concatenation(items) =>
      val (rhs, support) = items.foldRight((symbols = List.empty[Symbol], support = List.empty[BNF])):
        case (Identifier(symbol), (symbols, support)) => (symbol :: symbols, support)
        case (other, (symbols, support)) =>
          val nt = EBNF.freshNonTerminal()
          val prods = expand(other, nt)
          (nt :: symbols, prods ::: support)

      BNF(target, rhs) :: support

    case Alternation(options) =>
      options.flatMap(opt => expand(opt, target)).toList
    case Optional(Identifier(rhs)) =>
      List(BNF(target, Nil), BNF(target, rhs :: Nil))
    case ZeroOrMore(Identifier(rhs)) =>
      List(BNF(target, Nil), BNF(target, target :: rhs :: Nil))
    case OneOrMore(Identifier(rhs)) =>
      List(BNF(target, rhs :: Nil), BNF(target, target :: rhs :: Nil))
    case x =>
      raiseShouldNeverBeCalled(x.toString)

