package alpaca
package parser

import alpaca.core.Showable

import scala.quoted.*
import scala.util.Random

private[parser] trait Symbol {
  type IsEmpty <: Boolean
  def name: String
}

private[parser] sealed class NonTerminal(override val name: String) extends Symbol {
  type IsEmpty = false

  override def equals(that: Any): Boolean = that match
    case that: NonTerminal => this.name == that.name
    case _ => false

  override def hashCode(): Int = name.hashCode
}

private[parser] object NonTerminal:
  def fresh(name: String): NonTerminal =
    NonTerminal(s"${name}_${Random.alphanumeric.take(8).mkString}")
  def unapply(nonTerminal: NonTerminal): Some[String] = Some(nonTerminal.name)

private[parser] sealed class Terminal(override val name: String) extends Symbol {
  override def equals(that: Any): Boolean = that match
    case that: Terminal => this.name == that.name
    case _ => false

  override def hashCode(): Int = name.hashCode
}

private[parser] object Terminal:
  def apply(name: String): Terminal { type IsEmpty = false } = new Terminal(name) { type IsEmpty = false }
  def unapply(terminal: Terminal): Some[String] = Some(terminal.name)

private[parser] object Symbol {
  type NonEmpty = Symbol { type IsEmpty = false }

  given Showable[Symbol] = _.name

  case object Start extends NonTerminal("S'") { type IsEmpty = false }

  case object EOF extends Terminal("$") { type IsEmpty = false }

  case object Empty extends Terminal("ε") { type IsEmpty = true }

  given ToExpr[Symbol] with
    def apply(x: Symbol)(using Quotes): Expr[Symbol] = x match
      case x: NonTerminal => Expr(x)
      case x: Terminal => Expr(x)

  given ToExpr[NonTerminal] with
    def apply(x: NonTerminal)(using Quotes): Expr[NonTerminal] = '{ NonTerminal(${ Expr(x.name) }) }

  given ToExpr[Terminal] with
    def apply(x: Terminal)(using Quotes): Expr[Terminal] = '{ Terminal(${ Expr(x.name) }) }
}
