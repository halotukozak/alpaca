package alpaca
package parser

import alpaca.core.{raiseShouldNeverBeCalled, show, Showable}
import alpaca.parser.context.AnyGlobalCtx

import scala.quoted.*

/**
 * Type alias for semantic actions in the parser.
 *
 * An action is a function that takes the parser context and a sequence
 * of child values (from recognized symbols) and produces a result value.
 * Actions define how to build the abstract syntax tree or other output
 * from the parsed input.
 *
 * Note: The return type is Any for flexibility during compilation, but
 * the actual return value should match the expected result type for the
 * production rule being reduced.
 *
 * @tparam Ctx the parser context type
 * @tparam R the result type
 */
private[parser] type Action[-Ctx <: AnyGlobalCtx] = (Ctx, Seq[Any]) => Any

/**
 * An opaque type representing the parser action table.
 *
 * The action table maps productions to their semantic actions.
 * When a production is reduced during parsing, its action is executed
 * to compute the result value for the non-terminal on the left-hand side.
 *
 * @tparam Ctx the parser context type
 * @tparam R the result type
 */
opaque private[parser] type ActionTable[Ctx <: AnyGlobalCtx] = Map[Production, Action[Ctx]]

private[parser] object ActionTable {

  /**
   * Creates an ActionTable from a map of productions to actions.
   *
   * @param table the map of productions to their actions
   * @return an ActionTable
   */
  def apply[Ctx <: AnyGlobalCtx](table: Map[Production, Action[Ctx]]): ActionTable[Ctx] = table

  extension [Ctx <: AnyGlobalCtx, R](table: ActionTable[Ctx])
    /**
     * Gets the action for a production.
     *
     * @param production the production to get the action for
     * @return the semantic action for that production
     */
    def apply(production: Production): Action[Ctx] = table(production)
}

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
   *
   * @param newState the state to transition to
   */
  case Shift(newState: Int)

  /**
   * Reduce action: apply a production rule to reduce symbols.
   *
   * @param production the production to reduce by
   */
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
