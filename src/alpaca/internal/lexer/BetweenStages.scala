package alpaca
package internal
package lexer

import java.util.regex.Matcher

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
private[alpaca] trait BetweenStages[Ctx <: LexerCtx] extends ((Token[?, Ctx, ?], Matcher, Ctx) => Unit)

private[alpaca] object BetweenStages:

  /**
   * Automatically derives a BetweenStages instance for a context type.
   *
   * This macro combines BetweenStages instances from all parent traits
   * of the context type to create a composite update function.
   *
   * @tparam Ctx the context type
   * @return a BetweenStages instance
   */
  inline given auto[Ctx <: LexerCtx]: BetweenStages[Ctx] = ${ autoImpl[Ctx] }

  private def autoImpl[Ctx <: LexerCtx: Type](using quotes: Quotes): Expr[BetweenStages[Ctx]] = supervisedWithLog:
    import quotes.reflect.*
    logger.trace(show"deriving BetweenStages for ${Type.of[Ctx]}")

    val parents = TypeRepr
      .of[Ctx]
      .baseClasses
      .iterator
      .map(_.typeRef)
      .filter(_ <:< TypeRepr.of[LexerCtx])
      // we need to filter self type. Maybe I will change it in future since subtyping check does not work
      // and by symbol is disgusting :/
      .filterNot(_.typeSymbol == TypeRepr.of[Ctx].typeSymbol)
      .map(_.asType)
      .toList

    val derivedBetweenStages = Expr.ofList:
      parents
        .map:
          case '[type ctx >: Ctx <: LexerCtx; ctx] =>
            logger.trace(show"summoning BetweenStages for parent ${Type.of[ctx]}")
            Expr
              .summonIgnoring[BetweenStages[ctx]]('{ BetweenStages }.asTerm.symbol.methodMember("auto")*)
              .getOrElse(report.errorAndAbort(show"No BetweenStages instance found for ${Type.of[ctx]}"))

    '{ (token, m, ctx) =>
      $derivedBetweenStages.foreach(_.apply(token, m, ctx)) // todo: do not init List
    }
