package alpaca
package ebnf

import alpaca.ebnf.EBNF.*
import alpaca.parser.{Production as BNF, Symbol}
import alpaca.parser.Symbol.{NonTerminal, Terminal}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.Inside

final class EBNFTest extends AnyFunSuite with Matchers with Inside {
  private val d: Terminal = Terminal("d")
  private val A: NonTerminal = NonTerminal("A")
  private val B: NonTerminal = NonTerminal("B")
  private val C: NonTerminal = NonTerminal("C")
  private val D: NonTerminal = NonTerminal("D")
  private val E: NonTerminal = NonTerminal("E")
  private val F: NonTerminal = NonTerminal("F")

  given Conversion[Symbol, Identifier] = Identifier(_)

  test("Definition with single identifier: A = B.") {
    Definition(A, B).toBNF should contain theSameElementsAs Set(BNF(A, List(B)))
  }

  test("Concatenation: A = B C 'd.") {
    Definition(A, Concatenation(List(B, C, d))).toBNF should contain theSameElementsAs Set(BNF(A, List(B, C, d)))
  }

  test("Alternation: A = B | C.") {
    Definition(A, Alternation(Set(B, C))).toBNF should contain theSameElementsAs Set(BNF(A, List(B)), BNF(A, List(C)))
  }

  test("Optional (top-level): A = [B]. -> A = ε | B.") {
    Definition(A, Optional(B)).toBNF should contain theSameElementsAs Set(BNF(A, Nil), BNF(A, List(B)))
  }

  test("Optional inside concatenation: A = B [C] D.") {
    val prods = Definition(A, Concatenation(List(B, Optional(C), D))).toBNF

    // One production for A: A -> B X D
    inside(prods.filter(_.lhs == A)):
      case List(BNF(`A`, List(`B`, fresh: NonTerminal, `D`))) =>

        // Fresh productions: X -> ε | C
        prods.filter(_.lhs == fresh) should contain theSameElementsAs Set(BNF(fresh, Nil), BNF(fresh, List(C)))
  }

  test("Zero or more (top-level): A = {B}. -> A = ε | A B") {
    Definition(A, ZeroOrMore(B)).toBNF should contain theSameElementsAs Set(BNF(A, Nil), BNF(A, List(A, B)))
  }

  test("One or more (top-level): A = B+. -> A = B | A B") {
    Definition(A, OneOrMore(B)).toBNF should contain theSameElementsAs Set(BNF(A, List(B)), BNF(A, List(A, B)))
  }

  test("Alternation with optional: A = B | [C].") {
    Definition(A, Alternation(Set(B, Optional(C)))).toBNF should contain theSameElementsAs
      Set(BNF(A, List(B)), BNF(A, Nil), BNF(A, List(C)))
  }

  test("complex no complex") {
    List[Definition](
      Definition(NonTerminal("S"), Identifier(NonTerminal("R"))),
      Definition(
        NonTerminal("S"),
        Alternation(Set(Identifier(NonTerminal("L")), Identifier(Terminal("=")), Identifier(NonTerminal("R")))),
      ),
      Definition(NonTerminal("L"), Identifier(Terminal("ID"))),
      Definition(NonTerminal("L"), Alternation(Set(Identifier(Terminal("*")), Identifier(NonTerminal("R"))))),
      Definition(NonTerminal("R"), Identifier(NonTerminal("L"))),
      Definition(NonTerminal("root"), Identifier(NonTerminal("S"))),
    ).flatMap(_.toBNF) should contain theSameElementsAs Set(
      BNF(NonTerminal("S"), NonTerminal("R") :: Nil),
      BNF(NonTerminal("S"), List(NonTerminal("L"), Terminal("="), NonTerminal("R"))),
      BNF(NonTerminal("L"), Terminal("ID") :: Nil),
      BNF(NonTerminal("L"), List(Terminal("*"), NonTerminal("R"))),
      BNF(NonTerminal("R"), NonTerminal("L") :: Nil),
      BNF(NonTerminal("root"), NonTerminal("S") :: Nil),
    )
  }
}
