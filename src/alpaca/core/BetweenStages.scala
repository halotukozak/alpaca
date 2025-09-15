package alpaca
package core

import alpaca.lexer.context.AnyGlobalCtx

import scala.annotation.experimental
import scala.quoted.*
import scala.util.matching.Regex.Match

// marker for types which can be used as context
trait CtxMarker

// todo: i do not like this name
trait BetweenStages[Ctx <: CtxMarker] extends ((String, Match, Ctx) => Unit)

object BetweenStages {
  given BetweenStages[CtxMarker] = (name, m, ctx) => ()

  inline given [Ctx <: CtxMarker]: BetweenStages[Ctx] = ${ derivedImpl[Ctx] }

  private def derivedImpl[Ctx <: CtxMarker: Type](using quotes: Quotes): Expr[BetweenStages[Ctx]] = {
    import quotes.reflect.*

    val parents = TypeRepr
      .of[Ctx]
      .baseClasses
      .map(_.typeRef)
      .filter(_ <:< TypeRepr.of[CtxMarker])
      // we need to filter self type. Maybe I will change it in future since subtyping check does not work
      // and by symbol is disgusting :/
      .filterNot(_.typeSymbol == TypeRepr.of[Ctx].typeSymbol)
      .map(_.asType)

    val betweenStages = Expr.ofList {
      parents
        .flatMap { case '[type ctx >: Ctx <: CtxMarker; ctx] => Expr.summon[BetweenStages[ctx]] }
    }

    '{ (name, m, ctx) => $betweenStages.foreach(_.apply(name, m, ctx)) }
  }
}
