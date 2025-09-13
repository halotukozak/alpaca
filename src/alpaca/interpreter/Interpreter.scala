package alpaca.interpreter

import alpaca.core.show
import scala.collection.mutable
import alpaca.parser.Production
import alpaca.parser.Symbol
import alpaca.parser.Symbol.{NonTerminal, Terminal}
import alpaca.lexer.context.default.DefaultLexem
import scala.annotation.tailrec

class AST(val symbol: Symbol, val children: List[AST] = Nil) {
  def display(indentSize: Int = 0): Unit = {
    println(" " * indentSize + symbol.show)
    children.foreach(_.display(indentSize + 2))
  }
}

class Interpreter(parseTable: mutable.Map[(Int, Symbol), Int | Production]) {
  def run(code: List[DefaultLexem[?, String]]): Unit = loop(code, List(0))

  @tailrec
  private def loop(code: List[DefaultLexem[?, String]], stack: List[Int | AST]): Unit = {
    inline def handleReduction(production: Production): Unit = {
      val newStack = stack.drop(production.rhs.length * 2)
      val newState = newStack.head.asInstanceOf[Int]
      val nextSymbol = production.lhs

      if nextSymbol == NonTerminal("S'") && newState == 0 then {
        println("Accepted")
        stack.tail.head.asInstanceOf[AST].display()
      } else {
        parseTable.get((newState, nextSymbol)) match
          case Some(gotoState: Int) =>
            val children = stack.take(production.rhs.length * 2).collect { case ast: AST => ast }
            loop(code, gotoState :: AST(nextSymbol, children) :: newStack)
          case _ => throw new Error("No transition found")
      }
    }

    val state = stack.head.asInstanceOf[Int]
    val nextSymbol = Terminal(code.head.name)

    parseTable.get((state, nextSymbol)) match
      case Some(nextState: Int) => loop(code.tail, nextState :: AST(nextSymbol) :: stack)
      case Some(production: Production) => handleReduction(production)
      case None => throw new Error("No transition found")
  }
}
