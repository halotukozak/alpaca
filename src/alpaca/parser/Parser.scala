package alpaca
package parser

import alpaca.core.*
import alpaca.parser.context.AnyGlobalCtx
import alpaca.parser.context.default.EmptyGlobalCtx

import scala.annotation.experimental
import scala.quoted.{Expr, Quotes, Type}

import alpaca.lexer.context.Lexem
import alpaca.lexer.Token
import alpaca.parser.Symbol.NonTerminal
import alpaca.parser.Symbol.Terminal
import scala.collection.immutable.Range.Partial

type ParserDefinition[Ctx <: AnyGlobalCtx] = Unit

class Parser[Ctx <: AnyGlobalCtx](parseTable: ParseTable) {
  def parse[R](lexems: List[Lexem[?, ?]])(using empty: Empty[Ctx]): (ctx: Ctx, result: Option[R]) = {
    println(parseTable)
    (null.asInstanceOf[Ctx], None)
  }
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

  type SymbolExtractor = PartialFunction[Tree, alpaca.parser.Symbol]

  val extractTerminalRef: SymbolExtractor = {
    case TypedOrTest(
          Unapply(
            Select(
              TypeApply(
                Select(Apply(Select(_, "selectDynamic"), List(Literal(StringConstant(name)))), "$asInstanceOf$"),
                List(asInstanceOfType),
              ),
              "unapply",
            ),
            _,
            List(bind),
          ),
          _,
        ) =>
      // (name, asInstanceOfType, bind) //available for future
      Terminal(name)
  }

  val extractNonTerminalRef: SymbolExtractor = { case Unapply(Select(Ident(name), "unapply"), Nil, List(bind)) =>
    // (name, bind) //available for future
    NonTerminal(name)
  }

  val extractOptional: SymbolExtractor = {
    case Bind(bind, Typed(_, Applied(tpt, List(arg @ Singleton(Ident(name)))))) if tpt.tpe <:< TypeRepr.of[Option] =>
      if arg.tpe <:< TypeRepr.of[Rule] then NonTerminal(name, isOptional = true)
      else if arg.tpe <:< TypeRepr.of[Token[?, ?, ?]] then Terminal(name, isOptional = true)
      else raiseShouldNeverBeCalled(arg.tpe.show)
  }

  val extractRepeated: SymbolExtractor = {
    case Bind(bind, Typed(_, Applied(tpt, List(arg @ Singleton(Ident(name)))))) if tpt.tpe <:< TypeRepr.of[Seq] =>
      if arg.tpe <:< TypeRepr.of[Rule] then NonTerminal(name, isRepeated = true)
      else if arg.tpe <:< TypeRepr.of[Token[?, ?, ?]] then Terminal(name, isRepeated = true)
      else raiseShouldNeverBeCalled(arg.tpe.show)
  }

  val extractSymbol: SymbolExtractor = {
    case extractTerminalRef(terminal) => terminal
    case extractNonTerminalRef(nonterminal) => nonterminal
    case extractOptional(optional) => optional
    case extractRepeated(repeated) => repeated
    case x => raiseShouldNeverBeCalled(x.toString)
  }

  val productions: List[Production] = statements.flatMap {
    case ValDef(name, _, Some(Apply(Ident("rule"), List(Lambda(_, Match(_, cases: List[CaseDef])))))) =>
      cases.map {
        case CaseDef(pattern, Some(_), _) => throw new NotImplementedError("Guards are not supported yet")
        // todo: i hope we can abandon this case
        case CaseDef(pattern @ TypedOrTest(Unapply(_, _, List(_)), _), None, _) =>
          Production(NonTerminal(name), List(extractSymbol(pattern)))
        case CaseDef(TypedOrTest(Unapply(_, _, patterns), _), None, _) =>
          Production(NonTerminal(name), patterns.map(extractSymbol))
        case CaseDef(pattern, None, _) =>
          Production(NonTerminal(name), List(extractSymbol(pattern)))
      }
    case x => raiseShouldNeverBeCalled(x.show)
  }

  val table = Expr(ParseTable(productions))

  '{ new Parser[Ctx]($table) }
}
