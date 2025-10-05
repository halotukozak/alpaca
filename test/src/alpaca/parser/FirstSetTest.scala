package alpaca.parser

import org.scalatest.funsuite.AnyFunSuite
import alpaca.parser.Symbol.{NonTerminal, Terminal}

class FirstSetTest extends AnyFunSuite {
  test("FirstSet should correctly identify first sets for simple grammar") {
    val productions: List[Production] = List(
      Production(NonTerminal("S"), NonTerminal("L"), Terminal("="), NonTerminal("R")),
      Production(NonTerminal("S"), NonTerminal("R")),
      Production(NonTerminal("L"), Terminal("1"), NonTerminal("R")),
      Production(NonTerminal("L"), Terminal("2")),
      Production(NonTerminal("R"), Terminal("3"), NonTerminal("L")),
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
      Production(NonTerminal("E"), NonTerminal("T"), NonTerminal("E'")),
      Production(NonTerminal("E'"), Terminal("+"), NonTerminal("T"), NonTerminal("E'")),
      Production(NonTerminal("E'"), Symbol.Empty),
      Production(NonTerminal("T"), NonTerminal("F"), NonTerminal("T'")),
      Production(NonTerminal("T'"), Terminal("*"), NonTerminal("F"), NonTerminal("T'")),
      Production(NonTerminal("T'"), Symbol.Empty),
      Production(NonTerminal("F"), Terminal("("), NonTerminal("E"), Terminal(")")),
      Production(NonTerminal("F"), Terminal("id")),
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
