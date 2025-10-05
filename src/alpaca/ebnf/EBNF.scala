package alpaca.ebnf

import alpaca.core.raiseShouldNeverBeCalled
import alpaca.parser.{Production as BNF, Symbol}
import alpaca.parser.Symbol.*
import alpaca.core.Showable
import alpaca.core.show
import alpaca.core.Showable.mkShow

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
  given Showable[EBNF] =
    case Definition(lhs, rhs) => show"$lhs = $rhs"
    case Concatenation(items) => items.mkShow(", ")
    case Alternation(options) => options.mkShow(" | ")
    case Optional(node) => show"[$node]"
    case Identifier(symbol) => symbol.show
    case ZeroOrMore(node) => show"{$node}"
    case OneOrMore(node) => show"$node+"

  given Conversion[Symbol, Identifier] = Identifier(_)
