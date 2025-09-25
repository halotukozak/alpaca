package alpaca.parser

import alpaca.lexer.context.default.DefaultLexem
import alpaca.parser.Symbol.{NonTerminal, Terminal}
import alpaca.parser.context.AnyGlobalCtx

import scala.annotation.tailrec

type ParserDefinition[Ctx <: AnyGlobalCtx] = Unit

//inline it with lexer ctx
transparent inline given ctx(using c: AnyGlobalCtx): c.type = c

trait Dupa[T] extends ((DefaultLexem[?, ?], List[T]) => T)

class Parser[Ctx <: AnyGlobalCtx](parseTable: Map[(Int, Symbol), Int | Production]) {

  type R

  given Ctx = ???

  given create: Dupa[R] = ???

  def parse(input: List[DefaultLexem[?, ?]]): R | Null = {
    @tailrec def loop(input: List[DefaultLexem[?, ?]], stack: List[(state: Int, result: R | Null)]): R | Null = {
      inline def handleReduction(production: Production): R | Null = {
        val newStack = stack.drop(production.rhs.length)
        val newState = newStack.head.state
        val nextSymbol = production.lhs

        if nextSymbol == NonTerminal("S'") && newState == 0 then {
          stack.tail.head.result
        } else {
          parseTable.get((newState, nextSymbol)) match
            case Some(gotoState: Int) =>
              val children = stack.take(production.rhs.length).collect { case (_, r) if r != null => r.nn }
              loop(input, (gotoState, null) :: newStack)
            case _ => throw new Error("No transition found")
        }
      }

      val lexem :: rest = input: @unchecked

      parseTable.get((stack.head.state, Terminal(lexem.name))) match
        case Some(nextState: Int) => loop(rest, (nextState, create(lexem, Nil)) :: stack)
        case Some(production: Production) => handleReduction(production)
        case None => throw new Error("No transition found")
    }
    loop(input, List((0, null)))
  }

  def this() = this(???)
}
