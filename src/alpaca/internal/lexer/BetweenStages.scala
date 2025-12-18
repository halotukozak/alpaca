package alpaca
package internal
package lexer

import scala.util.matching.Regex.Match

/**
 * A hook for updating context between lexing stages.
 *
 * This trait defines a function that is called after each token match
 * to update the global context. It can be used to track line numbers,
 * column positions, or other custom state.
 */
private[alpaca] trait BetweenStages:
  type Self <: LexerCtx

  def apply(token: Token[Self], m: Match, c: Self): Unit

private[alpaca] object BetweenStages {

  /**
   * Automatically derives a BetweenStages instance for a context type.
   *
   * This macro combines BetweenStages instances from all parent traits
   * of the context type to create a composite update function.
   *
   * @tparam Ctx the context type
   * @return a BetweenStages instance
   */
  inline given [Ctx <: LexerCtx] => (Ctx has BetweenStages) = ${ autoImpl[Ctx] }

  private def autoImpl[Ctx <: LexerCtx: Type](using quotes: Quotes): Expr[Ctx has BetweenStages] = {
    import quotes.reflect.*

    val parents = TypeRepr
      .of[Ctx]
      .baseClasses
      .map(_.typeRef)
      .filter(_ <:< TypeRepr.of[LexerCtx])
      // we need to filter self type. Maybe I will change it in future since subtyping check does not work
      // and by symbol is disgusting :/
      .filterNot(_.typeSymbol == TypeRepr.of[Ctx].typeSymbol)
      .map(_.asType)

    val derivedBetweenStages = Expr.ofList {
      parents
        .map:
          case '[type ctx >: Ctx <: LexerCtx; ctx] =>
            Expr
              .summonIgnoring[ctx has BetweenStages]('{ BetweenStages }.asTerm.symbol.methodMember("auto")*)
              .getOrElse(report.errorAndAbort(s"No BetweenStages instance found for ${Type.show[ctx]}"))
    }

    '{ (token, m, ctx) =>
      $derivedBetweenStages.foreach(_.apply(token, m, ctx)) // todo: do not init List
    }
  }
}
