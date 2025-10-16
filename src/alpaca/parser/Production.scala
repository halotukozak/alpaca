package alpaca.parser

import alpaca.core.{show, NonEmptyList, Showable}
import alpaca.core.Showable.*
import alpaca.parser.Symbol
import alpaca.parser.Symbol.*

import scala.quoted.*

given ToExpr[Production | Symbol] with
  def apply(x: Production | Symbol)(using Quotes): Expr[Production | Symbol] = x match
    case prod: Production => '{ ${ Expr(prod) }: Production }
    case sym: Symbol => '{ ${ Expr(sym) }: Symbol }

private[parser] final case class Production(
  lhs: NonTerminal,
  rhs: NonEmptyList[Symbol],
  alwaysBefore: Set[Production | Symbol] = Set.empty,
  alwaysAfter: Set[Production | Symbol] = Set.empty,
) {
  def isBefore(other: Production): Boolean =
    this.alwaysBefore.contains(other) || this.alwaysBefore.exists {
      case prod: Production => prod.isBefore(other)
      case sym: Symbol => false
    }

  def isAfter(other: Production): Boolean =
    this.alwaysAfter.contains(other) || this.alwaysAfter.exists {
      case prod: Production => prod.isAfter(other)
      case sym: Symbol => false
    }
  def isBefore(other: Symbol): Boolean =
    this.alwaysBefore.contains(other) || this.alwaysBefore.exists {
      case prod: Production => prod.isBefore(other)
      case sym: Symbol => false
    }

  def isAfter(other: Symbol): Boolean =
    this.alwaysAfter.contains(other) || this.alwaysAfter.exists {
      case prod: Production => prod.isAfter(other)
      case sym: Symbol => false
    }

  def toItem(lookAhead: Terminal = Symbol.EOF): Item = Item(this, 0, lookAhead)
}

private[parser] object Production {
  given Showable[Production] =
    case Production(lhs, rhs) => show"$lhs -> ${rhs.mkShow(" ")}"

  def unapply(production: Production): (NonTerminal, NonEmptyList[Symbol]) =
    (production.lhs, production.rhs)

  given ToExpr[Production] with
    def apply(x: Production)(using Quotes): Expr[Production] =
      '{ Production(${ Expr(x.lhs) }, ${ Expr(x.rhs) }, ${ Expr(x.alwaysBefore) }, ${ Expr(x.alwaysAfter) }) }
}
