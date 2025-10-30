package alpaca
package parser

import alpaca.core.{*, given}
import alpaca.core.Showable.*
import alpaca.lexer.Token
import alpaca.parser.Symbol

import scala.annotation.{compileTimeOnly, StaticAnnotation}
import scala.quoted.*

final class name(name: ValidName) extends StaticAnnotation

/**
 * Represents a grammar production rule.
 *
 * A production defines how a non-terminal symbol can be expanded into
 * a sequence of symbols. For example: `E -> E + T` means the non-terminal
 * E can be produced by the sequence `E + T`.
 *
 * @param rhs the right-hand side sequence of symbols
 */
private[parser] enum Production(val rhs: NonEmptyList[Symbol.NonEmpty] | Symbol.Empty.type) {

  /** The left-hand side non-terminal of the production. */
  val lhs: NonTerminal

  /** An optional name for the production. */
  val name: ValidName | Null

  /**
   * Converts this production to an LR(0) item with a given lookahead.
   *
   * @param lookAhead the lookahead terminal (defaults to EOF)
   * @return an Item representing this production with the dot at position 0
   */
  def toItem(lookAhead: Terminal = Symbol.EOF): Item = Item(this, 0, lookAhead)

  case NonEmpty(
    lhs: NonTerminal & Symbol.NonEmpty,
    override val rhs: NonEmptyList[Symbol.NonEmpty],
    name: ValidName | Null = null,
  ) extends Production(rhs)

  case Empty(
    lhs: NonTerminal,
    name: ValidName | Null = null,
  ) extends Production(Symbol.Empty)
}

object Production {
  @compileTimeOnly(ConflictResolutionOnly)
  inline def apply(inline symbols: (Rule[?] | Token[?, ?, ?])*): Production = dummy
  @compileTimeOnly(ConflictResolutionOnly)
  inline def ofName(name: ValidName): Production = dummy

  given Showable[Production] =
    case NonEmpty(lhs, rhs, null) => show"$lhs -> ${rhs.mkShow(" ")}"
    case NonEmpty(lhs, rhs, name) => show"$lhs -> ${rhs.mkShow(" ")} ($name)"
    case Empty(lhs, null) => show"$lhs -> ${Symbol.Empty}"
    case Empty(lhs, name) => show"$lhs -> ${Symbol.Empty} ($name)"

  given ToExpr[Production] with
    def apply(x: Production)(using Quotes): Expr[Production] = x match
      case NonEmpty(lhs, rhs, name) => '{ NonEmpty(${ Expr(lhs) }, ${ Expr(rhs) }, ${ Expr(name) }) }
      case Empty(lhs, name) => '{ Empty(${ Expr(lhs) }, ${ Expr(name) }) }
}
