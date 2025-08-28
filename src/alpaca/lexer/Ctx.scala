package alpaca.lexer

import alpaca.core.reifyAllBetweenLexems
import alpaca.showAst

import scala.CanEqual.derived
import scala.quoted.{Expr, Quotes, Type}
import scala.util.matching.Regex.Match

opaque type BetweenStages[-Ctx <: AnyGlobalCtx] <: (Match, Ctx) => Unit = (Match, Ctx) => Unit

object BetweenStages {
  inline given derived[Ctx <: AnyGlobalCtx]: BetweenStages[Ctx] = ${ derivedImpl[Ctx] }

  private def derivedImpl[Ctx <: AnyGlobalCtx: Type](using quotes: Quotes): Expr[BetweenStages[Ctx]] = {
    import quotes.reflect.*

    val parents = TypeRepr.of[Ctx].baseClasses.map(_.typeRef.asType) // todo: should we add some filter?

    val betweenStages = Expr.ofList(
      parents
        .map { case '[type ctx >: Ctx <: AnyGlobalCtx; ctx] =>
          '{ compiletime.summonInline[BetweenStages[ctx]] }
        },
    )

    '{ (m, ctx: Ctx) => $betweenStages.foreach(_.apply(m, ctx)) }
  }
}

given BetweenStages[AnyGlobalCtx] = (m: Match, ctx: AnyGlobalCtx) => {
  ??? // todo: https://github.com/halotukozak/alpaca/issues/51
}

type AnyGlobalCtx = GlobalCtx[?]

//todo: find a way to make Ctx immutable with mutable-like changes
trait GlobalCtx[LexemTpe <: Lexem[?]] {
  type Lexem = LexemTpe

  var lastLexem: Lexem | Null
  var text: CharSequence
}

trait PositionTracking {
  this: GlobalCtx[?] =>

  var position: Int
}

given BetweenStages[PositionTracking & AnyGlobalCtx] = (m: Match, ctx: PositionTracking) => {
  ??? // todo: https://github.com/halotukozak/alpaca/issues/51
}

case class EmptyGlobalCtx[LexemTpe <: Lexem[?]](
  var lastLexem: LexemTpe | Null = null,
  var text: CharSequence = "",
) extends GlobalCtx[LexemTpe]

case class DefaultGlobalCtx[LexemTpe <: Lexem[?]](
  var lastLexem: LexemTpe | Null = null,
  var text: CharSequence = "",
  var position: Int = 0,
) extends GlobalCtx[LexemTpe]
    with PositionTracking

transparent inline given ctx(using c: AnyGlobalCtx): c.type = c
