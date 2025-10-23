package alpaca.parser

import org.scalatest.funsuite.AnyFunSuite

import NonEmptyProduction as NEP

import alpaca.core.NonEmptyList as NEL

class FirstSetTest extends AnyFunSuite {
  test("FirstSet should correctly identify first sets for simple grammar") {
    val productions: List[Production] = List(
      NEP(NonTerminal("S"), NEL(NonTerminal("L"), Terminal("="), NonTerminal("R"))),
      NEP(NonTerminal("S"), NEL(NonTerminal("R"))),
      NEP(NonTerminal("L"), NEL(Terminal("1"), NonTerminal("R"))),
      NEP(NonTerminal("L"), NEL(Terminal("2"))),
      NEP(NonTerminal("R"), NEL(Terminal("3"), NonTerminal("L"))),
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
      NEP(NonTerminal("E"), NEL(NonTerminal("T"), NonTerminal("E'"))),
      NEP(NonTerminal("E'"), NEL(Terminal("+"), NonTerminal("T"), NonTerminal("E'"))),
      EmptyProduction(NonTerminal("E'")),
      NEP(NonTerminal("T"), NEL(NonTerminal("F"), NonTerminal("T'"))),
      NEP(NonTerminal("T'"), NEL(Terminal("*"), NonTerminal("F"), NonTerminal("T'"))),
      EmptyProduction(NonTerminal("T'")),
      NEP(NonTerminal("F"), NEL(Terminal("("), NonTerminal("E"), Terminal(")"))),
      NEP(NonTerminal("F"), NEL(Terminal("id"))),
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
