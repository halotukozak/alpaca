package alpaca
package parser

import alpaca.core.Showable

import scala.quoted.*

/** Represents a grammar symbol (either terminal or non-terminal).
  *
  * In formal grammar theory, symbols are the basic building blocks of
  * productions. Terminals represent tokens from the lexer, while
  * non-terminals represent grammatical constructs.
  *
  * @param isTerminal whether this symbol is a terminal
  */
enum Symbol(val isTerminal: Boolean) {
  /** The symbol's name. */
  val name: String
  
  /** Whether this symbol is optional (appears 0 or 1 times). */
  val isOptional: Boolean
  
  /** Whether this symbol is repeated (appears 0 or more times). */
  val isRepeated: Boolean

  /** A non-terminal symbol representing a grammatical construct.
    *
    * @param name the symbol name
    * @param isOptional whether the symbol is optional
    * @param isRepeated whether the symbol is repeated
    */
  case NonTerminal(
    name: String,
    isOptional: Boolean = false,
    isRepeated: Boolean = false,
  ) extends Symbol(isTerminal = false)

  /** A terminal symbol representing a token from the lexer.
    *
    * @param name the symbol name (should match a token name)
    * @param isOptional whether the symbol is optional
    * @param isRepeated whether the symbol is repeated
    */
  case Terminal(
    name: String,
    isOptional: Boolean = false,
    isRepeated: Boolean = false,
  ) extends Symbol(isTerminal = true)
}

object Symbol {
  
  /** The augmented start symbol used internally by the parser. */
  val Start: NonTerminal = NonTerminal("S'")
  
  /** The end-of-file terminal symbol. */
  val EOF: Terminal = Terminal("$")
  
  /** The empty terminal symbol (epsilon). */
  val Empty: Terminal = Terminal("ε")

  given Showable[Symbol] = _.name

  given ToExpr[Symbol] with
    def apply(x: Symbol)(using Quotes): Expr[Symbol] = x match
      case x: NonTerminal => Expr(x)
      case x: Terminal => Expr(x)

  given ToExpr[NonTerminal] with
    def apply(x: NonTerminal)(using Quotes): Expr[NonTerminal] = '{
      NonTerminal(${ Expr(x.name) }, ${ Expr(x.isOptional) }, ${ Expr(x.isRepeated) })
    }

  given ToExpr[Terminal] with
    def apply(x: Terminal)(using Quotes): Expr[Terminal] = '{
      Terminal(${ Expr(x.name) }, ${ Expr(x.isOptional) }, ${ Expr(x.isRepeated) })
    }
}
