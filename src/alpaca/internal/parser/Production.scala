package alpaca
package internal
package parser

import scala.annotation.StaticAnnotation

/**
 * Represents a grammar production rule.
 *
 * A production defines how a non-terminal symbol can be expanded into
 * a sequence of symbols. For example: `E -> E + T` means the non-terminal
 * E can be produced by the sequence `E + T`.
 *
 * @param rhs the right-hand side sequence of symbols
 */
private[alpaca] enum Production(val rhs: NonEmptyList[Symbol.NonEmpty] | Symbol.Empty.type) {

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

  /** Showable instance for displaying productions in human-readable form. */
  given Showable[Production] = Showable:
    case NonEmpty(lhs, rhs, null) => show"$lhs -> ${rhs.mkShow(" ")}"
    case NonEmpty(lhs, rhs, name: String) => show"$lhs -> ${rhs.mkShow(" ")} ($name)"
    case Empty(lhs, null) => show"$lhs -> ${Symbol.Empty}"
    case Empty(lhs, name: String) => show"$lhs -> ${Symbol.Empty} ($name)"

  /** ToExpr instance for lifting productions to compile-time expressions. */
  given ToExpr[Production] with
    def apply(x: Production)(using Quotes): Expr[Production] = x match
      case NonEmpty(lhs, rhs, name) => '{ NonEmpty(${ Expr(lhs) }, ${ Expr(rhs) }, ${ Expr(name) }) }
      case Empty(lhs, name) => '{ Empty(${ Expr(lhs) }, ${ Expr(name) }) }
}
