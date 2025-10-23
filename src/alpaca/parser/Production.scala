package alpaca.parser

import alpaca.core.{show, NonEmptyList, Showable}
import alpaca.core.Showable.*
import alpaca.parser.Symbol

import scala.quoted.*

/**
 * Represents a grammar production rule.
 *
 * A production defines how a non-terminal symbol can be expanded into
 * a sequence of symbols. For example: `E -> E + T` means the non-terminal
 * E can be produced by the sequence `E + T`.
 *
 * @param lhs the left-hand side non-terminal
 * @param rhs the right-hand side sequence of symbols
 */
private[parser] enum Production(val rhs: NonEmptyList[Symbol.NonEmpty] | Symbol.Empty.type) {
  val lhs: NonTerminal

  /**
   * Converts this production to an LR(0) item with a given lookahead.
   *
   * @param lookAhead the lookahead terminal (defaults to EOF)
   * @return an Item representing this production with the dot at position 0
   */
  def toItem(lookAhead: Terminal = Symbol.EOF): Item = Item(this, 0, lookAhead)

  case NonEmpty(lhs: NonTerminal, override val rhs: NonEmptyList[Symbol.NonEmpty]) extends Production(rhs)
  case Empty(lhs: NonTerminal) extends Production(Symbol.Empty)
}

private[parser] object Production {
  given Showable[Production] =
    case NonEmpty(lhs, rhs) => show"$lhs -> ${rhs.mkShow(" ")}"
    case Empty(lhs) => show"$lhs -> ${Symbol.Empty}"

  given ToExpr[Production] with
    def apply(x: Production)(using Quotes): Expr[Production] = x match
      case NonEmpty(lhs, rhs) =>
        '{ NonEmpty(${ Expr(lhs) }, ${ Expr[NonEmptyList[Symbol]](rhs) }.asInstanceOf[NonEmptyList[Symbol.NonEmpty]]) }
      case Empty(lhs) => '{ Empty(${ Expr(lhs) }) }
}
