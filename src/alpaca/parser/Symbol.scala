package alpaca
package parser

import alpaca.core.Showable
import alpaca.parser.Symbol.*

import scala.quoted.*
import scala.util.Random

/**
 * Represents a grammar symbol (either terminal or non-terminal).
 *
 * In formal grammar theory, symbols are the basic building blocks of
 * productions. Terminals represent tokens from the lexer, while
 * non-terminals represent grammatical constructs.
 */
private[parser] trait Symbol extends Any {
  type IsEmpty <: Boolean
  def name: String
}

sealed case class NonTerminal(name: String) extends AnyVal with Symbol

object NonTerminal:

  def fresh(name: String): NonTerminal & NonEmpty =
    NonTerminal(s"${name}_${Random.alphanumeric.take(8).mkString}")

  inline def apply(inline name: String): NonTerminal & NonEmpty =
    new NonTerminal(name).asInstanceOf[NonTerminal & NonEmpty]

sealed case class Terminal(name: String) extends AnyVal with Symbol

object Terminal:
  inline def apply(inline name: String): Terminal & NonEmpty =
    new Terminal(name).asInstanceOf[Terminal & NonEmpty]

private[parser] object Symbol {
  type NonEmpty = Symbol { type IsEmpty = false }

  /** The augmented start symbol used internally by the parser. */
  val Start: NonTerminal { type IsEmpty = false } = NonTerminal("S'")

  /** The end-of-file terminal symbol. */
  val EOF: Terminal { type IsEmpty = false } = Terminal("$")

  /** The empty terminal symbol (epsilon). */
  val Empty: Terminal { type IsEmpty = true } = Terminal("ε").asInstanceOf[Terminal { type IsEmpty = true }]

  given Showable[Symbol] = _.name

  given [S <: Symbol]: ToExpr[S] with
    def apply(x: S)(using Quotes): Expr[S] =
      x.match
        case x: NonTerminal => Expr[NonTerminal](x)
        case x: Terminal => Expr[Terminal](x)
      .asInstanceOf[Expr[S]]

  given ToExpr[NonTerminal] with
    def apply(x: NonTerminal)(using Quotes): Expr[NonTerminal] = '{ NonTerminal(${ Expr(x.name) }) }

  given ToExpr[Terminal] with
    def apply(x: Terminal)(using Quotes): Expr[Terminal] = '{ Terminal(${ Expr(x.name) }) }
}
