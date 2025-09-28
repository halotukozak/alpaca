package alpaca
package parser

import alpaca.core.Showable

import scala.quoted.*

enum Symbol(val isTerminal: Boolean) {
  val name: String

  case NonTerminal(name: String) extends Symbol(isTerminal = false)
  case Terminal(name: String) extends Symbol(isTerminal = true)
}

object Symbol {
  val Start: NonTerminal = NonTerminal("S'")
  val EOF: Terminal = Terminal("$")
  val Empty: Terminal = Terminal("ε")

  given Showable[Symbol] = _.name

  given ToExpr[Symbol] with
    def apply(x: Symbol)(using Quotes): Expr[Symbol] = x match
      case x: NonTerminal => Expr(x)
      case x: Terminal => Expr(x)

  given ToExpr[NonTerminal] with
    def apply(x: NonTerminal)(using Quotes): Expr[NonTerminal] = '{ NonTerminal(${ Expr(x.name) }) }
  given ToExpr[Terminal] with
    def apply(x: Terminal)(using Quotes): Expr[Terminal] = '{ Terminal(${ Expr(x.name) }) }
}
