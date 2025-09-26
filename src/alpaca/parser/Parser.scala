package alpaca
package parser

import alpaca.core.*
import alpaca.lexer.Token
import alpaca.lexer.context.Lexem
import alpaca.parser.Symbol.{NonTerminal, Terminal}
import alpaca.parser.context.AnyGlobalCtx
import alpaca.parser.context.default.EmptyGlobalCtx

import scala.annotation.experimental
import scala.quoted.{Expr, Quotes, Type}

type ParserDefinition[Ctx <: AnyGlobalCtx] = Unit

abstract class Parser[Ctx <: AnyGlobalCtx] {
  inline def parse[R](
    lexems: List[Lexem[?, ?]],
  )(using empty: Empty[Ctx],
  ): (ctx: Ctx, result: Option[R]) =
    parse[R](parseTable[this.type], lexems)

  private def parse[R](
    parseTable: ParseTable,
    lexems: List[Lexem[?, ?]],
  )(using empty: Empty[Ctx],
  ): (ctx: Ctx, result: Option[R]) = {
    def parse(input: List[DefaultLexem[?, ?]]): R | Null = {
      @tailrec def loop(input: List[DefaultLexem[?, ?]], stack: List[(state: Int, result: R | Null)]): R | Null = {
        inline def handleReduction(production: Production): R | Null = {
          val newStack = stack.drop(production.rhs.length)
          val newState = newStack.head.state
          val nextSymbol = production.lhs

          if nextSymbol == NonTerminal("S'") && newState == 0 then {
            stack.tail.head.result
          } else {
            parseTable.get((newState, nextSymbol)) match
              case Some(gotoState: Int) =>
                val children = stack.take(production.rhs.length).collect { case (_, r) if r != null => r.nn }
                loop(input, (gotoState, null) :: newStack)
              case _ => throw new Error("No transition found")
          }
        }

        val lexem :: rest = input: @unchecked

        parseTable.get((stack.head.state, Terminal(lexem.name))) match
          case Some(nextState: Int) => loop(rest, (nextState, create(lexem, Nil)) :: stack)
          case Some(production: Production) => handleReduction(production)
          case None => throw new Error("No transition found")
      }
      loop(input, List((0, null)))
    }
    (null.asInstanceOf[Ctx], None)
  }

  protected given ctx: Ctx = ???
}

inline def parseTable[P <: Parser[?]]: ParseTable = ${ parseTableImpl[P] }

private def parseTableImpl[P <: Parser[?]: Type](using quotes: Quotes): Expr[ParseTable] = {
  import quotes.reflect.*

  def extractProductions: PartialFunction[Tree, List[Production]] = {
    case Apply(term, List(Lambda(_, Match(_, cases: List[CaseDef])))) =>
      type SymbolExtractor = PartialFunction[Tree, alpaca.parser.Symbol]

      def extractName: PartialFunction[Tree, String] = { case Select(This(kupadupa), name) =>
        name
      }

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

      val extractNonTerminalRef: SymbolExtractor = {
        case Unapply(Select(extractName(name), "unapply"), Nil, List(bind)) =>
          // (name, bind) //available for future
          NonTerminal(name)
      }

      val extractOptional: SymbolExtractor = {
        case Bind(bind, Typed(_, Applied(tpt, List(arg @ Singleton(extractName(name))))))
            if tpt.tpe <:< TypeRepr.of[Option] =>
          if arg.tpe <:< TypeRepr.of[Rule] then NonTerminal(name, isOptional = true)
          else if arg.tpe <:< TypeRepr.of[Token[?, ?, ?]] then Terminal(name, isOptional = true)
          else raiseShouldNeverBeCalled(arg.tpe.show)
      }

      val extractRepeated: SymbolExtractor = {
        case Bind(bind, Typed(_, Applied(tpt, List(arg @ Singleton(extractName(name))))))
            if tpt.tpe <:< TypeRepr.of[Seq] =>
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

      val name = Symbol.spliceOwner.name

      val productions = cases.map {
        case CaseDef(pattern, Some(_), _) => throw new NotImplementedError("Guards are not supported yet")
        // todo: i hope we can abandon this case
        case CaseDef(pattern @ TypedOrTest(Unapply(_, _, List(_)), _), None, _) =>
          Production(NonTerminal(name), List(extractSymbol(pattern)))
        case CaseDef(TypedOrTest(Unapply(_, _, patterns), _), None, _) =>
          Production(NonTerminal(name), patterns.map(extractSymbol))
        case CaseDef(pattern, None, _) =>
          Production(NonTerminal(name), List(extractSymbol(pattern)))
      }

      productions
  }

  val rules =
    TypeRepr
      .of[P]
      .typeSymbol
      .declaredFields
      .filter(_.typeRef <:< TypeRepr.of[Rule])
      .map(_.tree.asInstanceOf[ValDef])

  val productions = rules.flatMap:
    case ValDef(name, _, Some(extractProductions(rule))) => rule

  Expr(ParseTable(productions))
}
