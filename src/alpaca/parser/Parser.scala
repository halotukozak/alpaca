package alpaca.parser

import alpaca.core.{BetweenStages, Copyable, WithDefault}
import alpaca.parser.context.AnyGlobalCtx
import alpaca.parser.context.default.EmptyGlobalCtx

import scala.annotation.experimental
import scala.quoted.{Expr, Quotes, Type}

import alpaca.lexer.context.Lexem

type ParserDefinition[Ctx <: AnyGlobalCtx] = Unit

//inline it with lexer ctx
transparent inline given ctx(using c: AnyGlobalCtx): c.type = c

class Parser[Ctx <: AnyGlobalCtx] {
  def parse[R](input: List[Lexem[?, ?]]): (ctx: Ctx, result: R) =
    (null.asInstanceOf[Ctx], null.asInstanceOf[R])
}

@experimental //for IJ  :/
transparent inline def parser[Ctx <: AnyGlobalCtx & Product](
  using Ctx WithDefault EmptyGlobalCtx,
)(
  inline rules: Ctx ?=> ParserDefinition[Ctx],
)(using
  copy: Copyable[Ctx],
): Parser[Ctx] =
  ${ parserImpl[Ctx]('{ rules }, '{ summon }) }

//todo: ctxManipulation should work
//todo: more complex expressions should be supported in remaping
@experimental //for IJ  :/
private def parserImpl[Ctx <: AnyGlobalCtx: Type](
  rules: Expr[Ctx ?=> ParserDefinition[Ctx]],
  copy: Expr[Copyable[Ctx]],
)(using quotes: Quotes,
): Expr[Parser[Ctx]] = '{ new Parser[Ctx] }
