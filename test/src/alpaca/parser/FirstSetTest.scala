package alpaca.parser

import alpaca.parser.Symbol.{NonTerminal, Terminal}
import org.scalatest.funsuite.AnyFunSuite

import alpaca.core.NonEmptyList as NEL

class FirstSetTest extends AnyFunSuite {
  test("FirstSet should correctly identify first sets for simple grammar") {
    val productions: List[Production] = List(
      Production(NonTerminal("S"), NEL(NonTerminal("L"), Terminal("="), NonTerminal("R"))),
      Production(NonTerminal("S"), NEL(NonTerminal("R"))),
      Production(NonTerminal("L"), NEL(Terminal("1"), NonTerminal("R"))),
      Production(NonTerminal("L"), NEL(Terminal("2"))),
      Production(NonTerminal("R"), NEL(Terminal("3"), NonTerminal("L"))),
    )

    val expected = Map(
      NonTerminal("S") -> Set(Terminal("1"), Terminal("2"), Terminal("3")),
      NonTerminal("L") -> Set(Terminal("1"), Terminal("2")),
      NonTerminal("R") -> Set(Terminal("3")),
    )

    assert(FirstSet(productions) == expected)
  }

  test("FirstSet should handle epsilon productions") {
    val productions: List[Production] = List(
      Production(NonTerminal("E"), NEL(NonTerminal("T"), NonTerminal("E'"))),
      Production(NonTerminal("E'"), NEL(Terminal("+"), NonTerminal("T"), NonTerminal("E'"))),
      Production(NonTerminal("E'"), NEL(Symbol.Empty)),
      Production(NonTerminal("T"), NEL(NonTerminal("F"), NonTerminal("T'"))),
      Production(NonTerminal("T'"), NEL(Terminal("*"), NonTerminal("F"), NonTerminal("T'"))),
      Production(NonTerminal("T'"), NEL(Symbol.Empty)),
      Production(NonTerminal("F"), NEL(Terminal("("), NonTerminal("E"), Terminal(")"))),
      Production(NonTerminal("F"), NEL(Terminal("id"))),
    )

    val expected = Map(
      NonTerminal("E") -> Set(Terminal("("), Terminal("id")),
      NonTerminal("E'") -> Set(Terminal("+"), Symbol.Empty),
      NonTerminal("T") -> Set(Terminal("("), Terminal("id")),
      NonTerminal("T'") -> Set(Terminal("*"), Symbol.Empty),
      NonTerminal("F") -> Set(Terminal("("), Terminal("id")),
    )

    assert(FirstSet(productions) == expected)
  }
}
