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
private[parser] enum ParseAction:

  /**
   * Shift action: read the input symbol and move to a new state.
   */
  case Shift(newState: Int)

  /**
   * Reduce action: apply a production rule to reduce symbols.
   */
  case Reduction(production: Production)

private[parser] object ParseAction:

  given Showable[ParseAction] = Showable:
    case Shift(state) => show"S$state"
    case Reduction(production) => show"$production"

  // $COVERAGE-OFF$
  given ToExpr[ParseAction] with
    def apply(x: ParseAction)(using Quotes): Expr[ParseAction] = x match
      case Shift(state) => '{ Shift(${ Expr(state) }) }
      case Reduction(production) => '{ Reduction(${ Expr(production) }) }
// $COVERAGE-ON$
