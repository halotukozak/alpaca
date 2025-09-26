package alpaca
package parser

import alpaca.core.WithDefault
import alpaca.lexer.context.Lexem
import alpaca.parser.Symbol.{NonTerminal, Terminal}
import alpaca.parser.context.AnyGlobalCtx
import alpaca.parser.context.default.EmptyGlobalCtx

import scala.annotation.{compileTimeOnly, experimental, tailrec}

abstract class Parser[Ctx <: AnyGlobalCtx](
  using Ctx WithDefault EmptyGlobalCtx,
) {

  def root: Rule[?]

  @compileTimeOnly("Should never be called outside the parser definition")
  inline def rule[T](rules: Ctx ?=> RuleDefinition[T]): Rule[T] = null.asInstanceOf[Rule[T]]

  @experimental
  inline def parse[R](lexems: List[Lexem[?, ?]]): R | Null =
    parse[R](ParseTable[this.type], lexems :+ Lexem.EOF)

  @experimental
  private def parse[R](parseTable: ParseTable, lexems: List[Lexem[?, ?]]): R | Null = {
    type State = (index: Int, node: R | Null)

    def doSth(symbol: Symbol, children: List[R | Null] = Nil): R = null.asInstanceOf[R]

    @tailrec def loop(lexems: List[Lexem[?, ?]], stack: List[State]): R | Null = {
      val nextSymbol = Terminal(lexems.head.name)
      parseTable((stack.head.index, nextSymbol)) match
        case ParseAction.Shift(gotoState) =>
          loop(lexems.tail, (gotoState, doSth(nextSymbol)) :: stack)
        case ParseAction.Reduction(production) =>
          val newStack = stack.drop(production.rhs.length)
          val newState = newStack.head
          val nextSymbol = production.lhs

          if nextSymbol == NonTerminal("S'") && newState.index == 0 then {
            stack.head.node
          } else {
            val ParseAction.Shift(gotoState) = parseTable((newState.index, nextSymbol)).runtimeChecked
            val children = stack.take(production.rhs.length).map(_.node)
            loop(lexems, (gotoState, doSth(nextSymbol, children)) :: newStack)
          }
    }
    loop(lexems, List((0, null)))
  }

}
