package alpaca
package internal
package parser

/**
 * Represents a parse action in the LR parse table.
 *
 * Parse actions determine what the parser should do in each state
 * when encountering a symbol. It can either shift to a new state
 * or reduce by a production.
 */
private[parser] sealed trait ParseAction extends Any

private[parser] object ParseAction:

  /**
   * Shift action: read the input symbol and move to a new state.
   */
  sealed case class Shift(state: Int) extends AnyVal with ParseAction

  /**
   * Reduce action: apply a production rule to reduce symbols.
   */
  sealed case class Reduction(production: Production) extends AnyVal with ParseAction

  given Showable[ParseAction] = Showable:
    case Shift(state) => show"S$state"
    case Reduction(production) => show"$production"

  // $COVERAGE-OFF$
  given ToExpr[ParseAction] with
    def apply(x: ParseAction)(using Quotes): Expr[ParseAction] = x match
      case Shift(state) => '{ ParseAction.Shift(${ Expr(state) }) }
      case Reduction(production) => '{ ParseAction.Reduction(${ Expr(production) }) }
// $COVERAGE-ON$
