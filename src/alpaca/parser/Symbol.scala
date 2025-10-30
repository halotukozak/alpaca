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

/**
 * Represents a non-terminal symbol in the grammar.
 *
 * Non-terminals are symbols that can be expanded into other symbols
 * according to the grammar rules. For example, in a typical expression
 * grammar, "expr" and "term" would be non-terminals.
 *
 * @param name the name of the non-terminal
 */
sealed case class NonTerminal(name: String) extends AnyVal with Symbol

/**
 * Factory methods for creating non-terminal symbols.
 */
object NonTerminal:

  /**
   * Creates a fresh non-terminal symbol with a unique name.
   *
   * This is used internally to create temporary non-terminals for
   * EBNF operators like optional and repeated patterns.
   *
   * @param name the base name for the non-terminal
   * @return a non-terminal with a unique name based on the input
   */
  def fresh(name: String): NonTerminal & NonEmpty =
    NonTerminal(s"${name}_${Random.alphanumeric.take(8).mkString}")

  /**
   * Creates a non-terminal symbol from a name.
   *
   * @param name the name of the non-terminal
   * @return a non-empty non-terminal symbol
   */
  inline def apply(inline name: String): NonTerminal & NonEmpty =
    new NonTerminal(name).asInstanceOf[NonTerminal & NonEmpty]

/**
 * Represents a terminal symbol in the grammar.
 *
 * Terminals are the basic tokens that come from the lexer and cannot
 * be expanded further. For example, numbers, identifiers, and operators
 * are typically terminals.
 *
 * @param name the name of the terminal (token name)
 */
sealed case class Terminal(name: String) extends AnyVal with Symbol

/**
 * Factory methods for creating terminal symbols.
 */
object Terminal:
  /**
   * Creates a terminal symbol from a name.
   *
   * @param name the name of the terminal (token name)
   * @return a non-empty terminal symbol
   */
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
