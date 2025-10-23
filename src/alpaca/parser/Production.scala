package alpaca.parser

import alpaca.core.{show, NonEmptyList, Showable}
import alpaca.core.Showable.*
import alpaca.parser.Symbol

import scala.quoted.*

sealed trait Production {
  val lhs: NonTerminal
  val rhs: NonEmptyList[Symbol.NonEmpty] | Symbol.Empty.type
  def toItem(lookAhead: Terminal = Symbol.EOF): Item = Item(this, 0, lookAhead)
}

final case class NonEmptyProduction(lhs: NonTerminal, rhs: NonEmptyList[Symbol.NonEmpty]) extends Production
final case class EmptyProduction(lhs: NonTerminal) extends Production {
  val rhs: Symbol.Empty.type = Symbol.Empty
}

private[parser] object Production {
  given Showable[Production] =
    case NonEmptyProduction(lhs, rhs: NonEmptyList[Symbol.NonEmpty]) => show"$lhs -> ${rhs.mkShow(" ")}"
    case EmptyProduction(lhs) => show"$lhs -> ${Symbol.Empty}"

  given ToExpr[Production] with
    def apply(x: Production)(using Quotes): Expr[Production] = x match
      case NonEmptyProduction(lhs, rhs) => '{ NonEmptyProduction(${ Expr(lhs) }, ${ Expr(rhs) }) }
      case EmptyProduction(lhs) => '{ EmptyProduction(${ Expr(lhs) }) }
}
