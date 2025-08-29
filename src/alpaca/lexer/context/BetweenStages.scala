package alpaca.lexer
package context

import scala.quoted.*
import scala.util.matching.Regex.Match

// todo: i do not like this name
trait BetweenStages[-Ctx <: AnyGlobalCtx] extends ((Match, Ctx) => Unit)

object BetweenStages {
  inline given [Ctx <: AnyGlobalCtx]: BetweenStages[Ctx] = ${ derivedImpl[Ctx] }

  private def derivedImpl[Ctx <: AnyGlobalCtx: Type](using quotes: Quotes): Expr[BetweenStages[Ctx]] = {
    import quotes.reflect.*

    // todo: should we add some filter?
    // should we derive only for these, which has BetweenStages or for some marker trait?
    val parents = TypeRepr.of[Ctx].baseClasses.map(_.typeRef.asType)

    val betweenStages = Expr.ofList(
      parents
        .collect { case '[type ctx >: Ctx <: AnyGlobalCtx; ctx] =>
          '{ compiletime.summonInline[BetweenStages[ctx]] }
        },
    )

    '{ (m, ctx: Ctx) => $betweenStages.foreach(_.apply(m, ctx)) }
  }
}
