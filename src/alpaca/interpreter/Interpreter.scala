package alpaca.interpreter

import alpaca.core.show
import scala.collection.mutable
import alpaca.parser.Production
import alpaca.parser.Symbol
import alpaca.parser.Symbol.{NonTerminal, Terminal}
import alpaca.lexer.context.default.DefaultLexem
import scala.annotation.tailrec
import alpaca.parser.{ParseTable, ParseAction}

class AST(val symbol: Symbol, val children: List[AST] = Nil) {
  override def toString: String = {
    val childrenStr = children.map(_.toString.split("\n").map("  " + _).mkString("\n"))
    (List(symbol.show) ++ childrenStr).mkString("\n")
  }
}

class State(val nr: Int, val node: Option[AST])

class Interpreter(parseTable: ParseTable) {
  def run(code: List[DefaultLexem[?, String]]): AST = loop(code, List(State(0, None)))

  @tailrec
  private def loop(code: List[DefaultLexem[?, String]], stack: List[State]): AST = {
    inline def handleReduction(production: Production): AST = {
      val newStack = stack.drop(production.rhs.length)
      val newState = newStack.head
      val nextSymbol = production.lhs

      if nextSymbol == NonTerminal("S'") && newState.nr == 0 then {
        stack.head.node.get
      } else {
        parseTable.get((newState.nr, nextSymbol)) match
          case Some(ParseAction.Shift(gotoState)) =>
            val children = stack.take(production.rhs.length).map(_.node.get)
            loop(code, State(gotoState, Some(AST(nextSymbol, children))) :: newStack)
          case _ => throw new Error("No transition found")
      }
    }

    val state = stack.head
    val nextSymbol = Terminal(code.head.name)

    parseTable.get((state.nr, nextSymbol)) match
      case Some(ParseAction.Shift(gotoState)) => loop(code.tail, State(gotoState, Some(AST(nextSymbol))) :: stack)
      case Some(ParseAction.Reduction(production)) => handleReduction(production)
      case None => throw new Error("No transition found")
  }
}
