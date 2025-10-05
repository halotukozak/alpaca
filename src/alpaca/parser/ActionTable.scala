package alpaca
package parser

import alpaca.parser.context.AnyGlobalCtx
import alpaca.core.Showable
import scala.quoted.*
import alpaca.core.show

type Action[Ctx <: AnyGlobalCtx, R] = (Ctx, Seq[Any]) => Any

opaque type ActionTable[Ctx <: AnyGlobalCtx, R] = Map[Production, Action[Ctx, R]]

object ActionTable {
  private[parser] def apply[Ctx <: AnyGlobalCtx, R](table: Map[Production, Action[Ctx, R]]): ActionTable[Ctx, R] = table

  extension [Ctx <: AnyGlobalCtx, R](table: ActionTable[Ctx, R])
    def apply(production: Production): Action[Ctx, R] = table(production)
}

enum ParseAction:
  case Shift(newState: Int)
  case Reduction(production: Production)

object ParseAction {
  given Showable[ParseAction] =
    case ParseAction.Shift(newState) => show"S$newState"
    case ParseAction.Reduction(production) => show"$production"

  given ToExpr[ParseAction] with
    def apply(x: ParseAction)(using Quotes): Expr[ParseAction] = x match
      case ParseAction.Shift(i) => '{ ParseAction.Shift(${ Expr(i) }) }
      case ParseAction.Reduction(p) => '{ ParseAction.Reduction(${ Expr(p) }) }
}
