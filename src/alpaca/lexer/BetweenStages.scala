package alpaca
package lexer

import alpaca.lexer.context.{AnyGlobalCtx, GlobalCtx}
import alpaca.soft

import scala.quoted.*
import scala.util.matching.Regex.Match

/**
 * A hook for updating context between lexing stages.
 *
 * This trait defines a function that is called after each token match
 * to update the global context. It can be used to track line numbers,
 * column positions, or other custom state.
 *
 * @tparam Ctx the global context type
 */
// todo: i do not like this name
trait BetweenStages[Ctx <: GlobalCtx] extends ((Token[?, Ctx, ?], Match, Ctx) => Unit)

object BetweenStages {

  /**
   * Automatically derives a BetweenStages instance for a context type.
   *
   * This macro combines BetweenStages instances from all parent traits
   * of the context type to create a composite update function.
   *
   * @tparam Ctx the context type
   * @return a BetweenStages instance
   */
  inline given auto[Ctx <: GlobalCtx]: BetweenStages[Ctx] = ${ autoImpl[Ctx] }

  private def autoImpl[Ctx <: GlobalCtx: Type](using quotes: Quotes): Expr[BetweenStages[Ctx]] = {
    import quotes.reflect.*

    val parents = TypeRepr
      .of[Ctx]
      .baseClasses
      .map(_.typeRef)
      .filter(_ <:< TypeRepr.of[GlobalCtx])
      // we need to filter self type. Maybe I will change it in future since subtyping check does not work
      // and by symbol is disgusting :/
      .filterNot(_.typeSymbol == TypeRepr.of[Ctx].typeSymbol)
      .map(_.asType)

    val betweenStages = Expr.ofList {
      parents
        .map { case '[type ctx >: Ctx <: GlobalCtx; ctx] =>
          Expr
            .summonIgnoring[BetweenStages[ctx]]('{ BetweenStages }.asTerm.symbol.methodMember("auto")*)
            .getOrElse {
              report.errorAndAbort(s"No BetweenStages instance found for ${Type.show[ctx]}")
            }
        }
    }

    '{ (name, m, ctx) => $betweenStages.foreach(_.apply(name, m, ctx)) }
  }
}
