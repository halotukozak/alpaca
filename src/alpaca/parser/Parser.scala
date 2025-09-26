package alpaca
package parser

import alpaca.core.WithDefault
import alpaca.lexer.context.Lexem
import alpaca.parser.Symbol.{NonTerminal, Terminal}
import alpaca.parser.context.AnyGlobalCtx
import alpaca.parser.context.default.EmptyGlobalCtx

import scala.annotation.{experimental, tailrec}

abstract class Parser[Ctx <: AnyGlobalCtx](
  using Ctx WithDefault EmptyGlobalCtx,
) {

  def ctx: Ctx = ???

  def root: Rule[Any]

  @experimental
  inline def parse[R](lexems: List[Lexem[?, ?]]): R | Null = {
    val (parseTable, actionTable) = createTables[this.type]
    parse[R](parseTable, actionTable, lexems :+ Lexem.EOF)
  }

  @experimental
  private def parse[R](parseTable: ParseTable, actionTable: ActionTable, lexems: List[Lexem[?, ?]]): R | Null = {
    type State = (index: Int, node: R | Lexem[?, ?] | Null)

    def doSth(symbol: Symbol, children: List[R | Null] = Nil): R = null.asInstanceOf[R]

    @tailrec def loop(lexems: List[Lexem[?, ?]], stack: List[State]): R | Null = {
      val nextSymbol = Terminal(lexems.head.name)
      parseTable((stack.head.index, nextSymbol)) match
        case ParseAction.Shift(gotoState) =>
          loop(lexems.tail, (gotoState, lexems.head) :: stack)

        case ParseAction.Reduction(production @ Production(nextSymbol, rhs)) =>
          val newStack = stack.drop(rhs.size)
          val newState = newStack.head

          if nextSymbol == Symbol.Start && newState.index == 0 then stack.head.node.asInstanceOf[R | Null]
          else {
            val ParseAction.Shift(gotoState) = parseTable((newState.index, nextSymbol)).runtimeChecked
            val children = stack.take(rhs.size).map(_.node)
            loop(lexems, (gotoState, actionTable(production)(children).asInstanceOf[R | Lexem[?, ?] | Null]) :: newStack)
          }

    }
    loop(lexems, (0, null) :: Nil)
  }

}
