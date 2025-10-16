package alpaca
package parser

import alpaca.core.{show, Showable}
import alpaca.parser.context.AnyGlobalCtx

import scala.quoted.*

private[parser] type Action[Ctx <: AnyGlobalCtx, R] = (Ctx, Seq[Any]) => Any

opaque private[parser] type ActionTable[Ctx <: AnyGlobalCtx, R] = Map[Production, Action[Ctx, R]]

private[parser] object ActionTable {
  def apply[Ctx <: AnyGlobalCtx, R](table: Map[Production, Action[Ctx, R]]): ActionTable[Ctx, R] = table

  extension [Ctx <: AnyGlobalCtx, R](table: ActionTable[Ctx, R])
    def apply(production: Production): Action[Ctx, R] = table(production)
}

private[parser] sealed trait ParseAction
final case class Shift(newState: Int) extends ParseAction
final case class Reduction(production: Production) extends ParseAction {
  def isBefore(other: Reduction): Boolean =
    production.isBefore(other.production)
    
  def isAfter(other: Reduction): Boolean =
    production.isAfter(other.production)
    
  def isBefore(other: Symbol): Boolean =
    production.isBefore(other)
    
  def isAfter(other: Symbol): Boolean =
    production.isAfter(other)
}

private[parser] object ParseAction {
  given Showable[ParseAction] =
    case Shift(newState) => show"S$newState"
    case Reduction(production) => show"$production"

  given ToExpr[ParseAction] with
    def apply(x: ParseAction)(using Quotes): Expr[ParseAction] = x match
      case Shift(newState) => '{ Shift(${ Expr(newState) }) }
      case Reduction(production) => '{ Reduction(${ Expr(production) }) }
}
