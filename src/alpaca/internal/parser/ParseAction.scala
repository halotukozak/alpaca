package alpaca
package internal
package parser

import scala.reflect.TypeTest

/**
 * Represents a parse action in the LR parse table.
 *
 * Parse actions determine what the parser should do in each state
 * when encountering a symbol. It can either shift to a new state
 * or reduce by a production.
 */
private[parser] type ParseAction = ParseAction.Shift | ParseAction.Reduction

private[parser] object ParseAction {

  /**
   * Shift action: read the input symbol and move to a new state.
   */
  opaque type Shift = Int

  /**
   * Reduce action: apply a production rule to reduce symbols.
   */
  opaque type Reduction = Production

  given ParseAction is Showable =
    case shift: Int => show"S$shift"
    case reduction: Production => show"$reduction"

  object Shift:
    inline def apply(inline newState: Int): Shift = newState

    inline def unapply(inline shift: Shift): Some[Int] = Some(shift)

    given TypeTest[Any, Shift] with
      def unapply(x: Any): Option[Shift & x.type] = x match
        case i: Int => Some(i.asInstanceOf[Shift & x.type])
        case _ => None
    extension (shift: Shift) inline def newState: Int = shift

  object Reduction:
    inline def apply(inline production: Production): Reduction = production

    inline def unapply(inline red: Reduction): Some[Production] = Some(red)

    given TypeTest[Any, Reduction] with
      def unapply(x: Any): Option[Reduction & x.type] = x match
        case p: Production => Some(p.asInstanceOf[Reduction & x.type])
        case _ => None
    extension (red: Reduction) inline def production: Production = red

  given ToExpr[ParseAction] with
    def apply(x: ParseAction)(using Quotes): Expr[ParseAction] = x match
      case shift: Int => '{ ${ Expr(shift) }: ParseAction.Shift }
      case production: Production => '{ ${ Expr(production) }: ParseAction.Reduction }
}
