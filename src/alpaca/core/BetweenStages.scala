package alpaca.core

import alpaca.lexer.context.AnyGlobalCtx

import scala.quoted.*
import scala.util.matching.Regex.Match

// todo: i do not like this name
trait BetweenStages[Ctx] extends ((Match, Ctx) => Unit)

object BetweenStages {
  inline given [Ctx]: BetweenStages[Ctx] = ${ derivedImpl[Ctx] }

  private def derivedImpl[Ctx: Type](using quotes: Quotes): Expr[BetweenStages[Ctx]] = {
    import quotes.reflect.*

    // todo: should we add some filter?
    // should we derive only for these, which has BetweenStages or for some marker trait?
    val parents = TypeRepr.of[Ctx].baseClasses.map(_.typeRef.asType)

    val betweenStages = Expr.ofList(
      parents
        .collect { case '[type ctx >: Ctx; ctx] =>
          Expr.summon[BetweenStages[ctx]]
        }
        .collect { case Some(expr) => expr },
    )

    '{ (m, ctx: Ctx) => $betweenStages.foreach(_.apply(m, ctx)) }
  }
}
