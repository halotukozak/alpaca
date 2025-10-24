package alpaca.parser

import alpaca.core.Showable.*
import alpaca.core.{show, NonEmptyList, Showable, ValidName, given}
import alpaca.parser.Symbol

import scala.annotation.StaticAnnotation
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
  val name: Option[ValidName]

  /**
   * Converts this production to an LR(0) item with a given lookahead.
   *
   * @param lookAhead the lookahead terminal (defaults to EOF)
   * @return an Item representing this production with the dot at position 0
   */
  def toItem(lookAhead: Terminal = Symbol.EOF): Item = Item(this, 0, lookAhead)

  case NonEmpty(lhs: NonTerminal, override val rhs: NonEmptyList[Symbol.NonEmpty], name: Option[ValidName] = None)
    extends Production(rhs)
  case Empty(lhs: NonTerminal, name: Option[ValidName] = None) extends Production(Symbol.Empty)
}

private[parser] object Production {
  given Showable[Production] =
    case NonEmpty(lhs, rhs, Some(name)) => show"$lhs -> ${rhs.mkShow(" ")} ($name)"
    case NonEmpty(lhs, rhs, None) => show"$lhs -> ${rhs.mkShow(" ")}"
    case Empty(lhs, Some(name)) => show"$lhs -> ${Symbol.Empty} ($name)"
    case Empty(lhs, None) => show"$lhs -> ${Symbol.Empty}"

  given ToExpr[Production] with
    def apply(x: Production)(using Quotes): Expr[Production] = x match
      case NonEmpty(lhs, rhs, name) =>
        '{
          NonEmpty(
            ${ Expr(lhs) },
            ${ Expr[NonEmptyList[Symbol]](rhs) }.asInstanceOf[NonEmptyList[Symbol.NonEmpty]],
            ${ Expr(name) },
          )
        }
      case Empty(lhs, name) => '{ Empty(${ Expr(lhs) }, ${ Expr(name) }) }
}
