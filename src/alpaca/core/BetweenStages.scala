package alpaca
package core

import alpaca.lexer.context.AnyGlobalCtx

import scala.quoted.*

import scala.util.matching.Regex.Match

// todo: i do not like this name
trait BetweenStages[Ctx] extends ((String, Match, Ctx) => Unit)

object BetweenStages {
  inline given [Ctx]: BetweenStages[Ctx & AnyGlobalCtx] = ${ derivedImpl[Ctx & AnyGlobalCtx] }

  private def derivedImpl[Ctx: Type](using quotes: Quotes): Expr[BetweenStages[Ctx & AnyGlobalCtx]] = {
    import quotes.reflect.*

    // todo: should we add some filter?
    // should we derive only for these, which has BetweenStages or for some marker trait?
    val parents = TypeRepr.of[Ctx].baseClasses.map(_.typeRef.asType)

    val betweenStages = Expr.ofList(
      '{AnyGlobalCtx.given_BetweenStages_AnyGlobalCtx}
      ::
      parents
        .map { case '[type ctx >: Ctx; ctx] =>
          Expr.summon[BetweenStages[ctx & AnyGlobalCtx]]
        }
        .collect { case Some(expr) => expr },
    )

    '{ (name, m, ctx: Ctx & AnyGlobalCtx) => $betweenStages.foreach(_.apply(name, m, ctx)) }
  }
}
