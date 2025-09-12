package alpaca
package parser

import alpaca.core.*
import alpaca.parser.context.AnyGlobalCtx
import alpaca.parser.context.default.EmptyGlobalCtx

import scala.annotation.experimental
import scala.quoted.{Expr, Quotes, Type}

import alpaca.lexer.context.Lexem
import alpaca.parser.Symbol.NonTerminal
import alpaca.parser.Symbol.Terminal

type ParserDefinition[Ctx <: AnyGlobalCtx] = Unit

class Parser[Ctx <: AnyGlobalCtx](parseTable: ParseTable) {
  def parse[R](lexems: List[Lexem[?, ?]])(using empty: Empty[Ctx]): (ctx: Ctx, result: Option[R]) =
    (null.asInstanceOf[Ctx], None)
}

@experimental //for IJ  :/
inline def parser[Ctx <: AnyGlobalCtx & Product](
  using Ctx WithDefault EmptyGlobalCtx,
)(
  inline rules: Ctx ?=> ParserDefinition[Ctx],
)(using
  copy: Copyable[Ctx],
): Parser[Ctx] =
  ${ parserImpl[Ctx]('{ rules }, '{ summon }) }

@experimental //for IJ  :/
private def parserImpl[Ctx <: AnyGlobalCtx: Type](
  rules: Expr[Ctx ?=> ParserDefinition[Ctx]],
  copy: Expr[Copyable[Ctx]],
)(using quotes: Quotes,
): Expr[Parser[Ctx]] = {
  import quotes.reflect.*

  val Lambda(oldCtx :: Nil, Block(statements, _)) = rules.asTerm.underlying.runtimeChecked

  val productions: List[Production] = statements.flatMap {
    case ValDef(name, _, Some(Apply(Ident("rule"), List(Lambda(_, Match(_, cases: List[CaseDef])))))) =>
      cases.map {
        case CaseDef(TypedOrTest(Unapply(_, _, patterns), _), _, _) =>
          val rhs: Seq[alpaca.parser.Symbol] = patterns.map {
            case TypedOrTest(
                  Unapply(
                    Select(
                      TypeApply(
                        Select(
                          Apply(
                            Select(_, "selectDynamic"),
                            List(Literal(StringConstant(name))),
                          ),
                          "$asInstanceOf$",
                        ),
                        List(id),
                      ),
                      "unapply",
                    ),
                    _,
                    List(bind),
                  ),
                  _,
                ) =>
              Terminal(name)
            case _ => raiseShouldNeverBeCalled()
          }
          Production(NonTerminal(name), rhs)
        case CaseDef(pattern, Some(_), _) => throw new NotImplementedError("Guards are not supported yet")
        case CaseDef(x, _, _) => raiseShouldNeverBeCalled(x.show)
      }
    case x => raiseShouldNeverBeCalled(x.show)
  }

  val table = Expr(ParseTable(productions))

  '{ new Parser[Ctx]($table) }
}
