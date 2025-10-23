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

private[parser] enum ParseAction:
  case Shift(newState: Int)
  case Reduction(production: Production)

private[parser] object ParseAction {
  given Showable[ParseAction] =
    case ParseAction.Shift(newState) => show"S$newState"
    case ParseAction.Reduction(production) => show"$production"

  given ToExpr[ParseAction] with
    def apply(x: ParseAction)(using Quotes): Expr[ParseAction] = x match
      case ParseAction.Shift(i) => '{ ParseAction.Shift(${ Expr(i) }) }
      case ParseAction.Reduction(p) => '{ ParseAction.Reduction(${ Expr(p) }) }
}
