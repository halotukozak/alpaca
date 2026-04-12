package alpaca
package internal
package lexer

import scala.annotation.implicitNotFound

/**
 * A hook for updating context between lexing stages.
 *
 * This trait defines a function that is called after each token match
 * to update the global context. It can be used to track line numbers,
 * column positions, or other custom state.
 *
 * @tparam Ctx the global context type
 */
@implicitNotFound("Define OnTokenMatch for ${Ctx} (or its subclasses)")
trait OnTokenMatch[Ctx <: LexerCtx] extends ((Token[?, Ctx, ?], String, Ctx) => Unit)

object OnTokenMatch:

  /**
   * Automatically derives a OnTokenMatch instance for a context type.
   *
   * This macro combines OnTokenMatch instances from all parent traits
   * of the context type to create a composite update function.
   *
   * @tparam Ctx the context type
   * @return a OnTokenMatch instance
   */
  inline given auto[Ctx <: LexerCtx]: OnTokenMatch[Ctx] = ${ autoImpl[Ctx] }

  // $COVERAGE-OFF$
  private def autoImpl[Ctx <: LexerCtx: Type](using quotes: Quotes): Expr[OnTokenMatch[Ctx]] = withLog:
    import quotes.reflect.*

    logger.trace(show"deriving OnTokenMatch for ${Type.of[Ctx]}")

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

    val derivedOnTokenMatch = parents
      .map:
        case '[type ctx >: Ctx <: LexerCtx; ctx] =>
          logger.trace(show"summoning OnTokenMatch for parent ${Type.of[ctx]}")
          Expr
            .summonIgnoring[OnTokenMatch[ctx]]('{ OnTokenMatch }.asTerm.symbol.methodMember("auto")*)
            .getOrElse(report.errorAndAbort(show"No OnTokenMatch instance found for ${Type.of[ctx]}"))

    '{ (token, m, ctx) =>
      ${ Expr.block(derivedOnTokenMatch.map(bs => '{ $bs.apply(token, m, ctx) }), '{}) }
    }
// $COVERAGE-ON$
