package alpaca
package parser

import alpaca.lexer.context.Lexem
import alpaca.parser.Symbol.{NonTerminal, Terminal}
import alpaca.parser.context.AnyGlobalCtx

import scala.annotation.{experimental, tailrec}

type ParserDefinition[Ctx <: AnyGlobalCtx] = Unit

abstract class Parser[Ctx <: AnyGlobalCtx] {
  @experimental
  inline def parse[R](lexems: List[Lexem[?, ?]]): R | Null =
    parse[R](ParseTable[this.type], lexems :+ Lexem.EOF)

  @experimental
  private def parse[R](parseTable: ParseTable, lexems: List[Lexem[?, ?]]): R | Null = {
    type State = (index: Int, node: R | Null)

    def doSth(symbol: Symbol, children: List[R | Null] = Nil): R = null.asInstanceOf[R]

    @tailrec def loop(lexems: List[Lexem[?, ?]], stack: List[State]): R | Null = {
      inline def handleReduction(production: Production): R | Null = {
        val newStack = stack.drop(production.rhs.length)
        val newState = newStack.head
        val nextSymbol = production.lhs

        if nextSymbol == NonTerminal("S'") && newState.index == 0 then {
          stack.head.node
        } else {
          parseTable.get((newState.index, nextSymbol)) match
            case Some(ParseAction.Shift(gotoState)) =>
              val children = stack.take(production.rhs.length).map(_.node)
              loop(lexems, (gotoState, doSth(nextSymbol, children)) :: newStack)
            case _ =>
              throw new Error("No transition found")
        }
      }

      val nextSymbol = Terminal(lexems.head.name)
      parseTable.get((stack.head.index, nextSymbol)) match
        case Some(ParseAction.Shift(gotoState)) =>
          loop(lexems.tail, (gotoState, doSth(nextSymbol)) :: stack)
        case Some(ParseAction.Reduction(production)) =>
          handleReduction(production)
        case None =>
          throw new Error("No transition found")
    }
    loop(lexems, List((0, null)))
  }

  protected given ctx: Ctx = ???
}
