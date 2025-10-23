package alpaca.parser

import alpaca.core.{show, NonEmptyList, Showable}
import alpaca.core.Showable.*
import alpaca.parser.Symbol

import scala.quoted.*

private[parser] enum Production(val rhs: NonEmptyList[Symbol.NonEmpty] | Symbol.Empty.type) {
  val lhs: NonTerminal

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
