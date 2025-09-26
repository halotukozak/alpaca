package alpaca
package parser

import alpaca.core.*
import alpaca.parser.Symbol.{NonTerminal, Terminal}

import java.io.FileWriter
import scala.collection.mutable
import scala.quoted.*
import scala.reflect.NameTransformer
import scala.util.Using

enum ParseAction {
  case Shift(newState: Int)
  case Reduction(production: Production)
}

object ParseAction {
  given Showable[ParseAction] =
    case ParseAction.Shift(newState) => show"S$newState"
    case ParseAction.Reduction(production) => show"$production"

  given ToExpr[ParseAction] with
    def apply(x: ParseAction)(using Quotes): Expr[ParseAction] = x match
      case ParseAction.Shift(i) => '{ ParseAction.Shift(${ Expr(i) }) }
      case ParseAction.Reduction(p) => '{ ParseAction.Reduction(${ Expr(p) }) }
}

opaque type ParseTable <: Map[(state: Int, stepSymbol: Symbol), ParseAction] =
  Map[(state: Int, stepSymbol: Symbol), ParseAction]

object ParseTable {
  inline def apply[P <: Parser[?]]: ParseTable = ${ applyImpl[P] }
  private def applyImpl[P <: Parser[?]: Type](using quotes: Quotes): Expr[ParseTable] = {
    import quotes.reflect.*

    def extractProductions: PartialFunction[Tree, List[Production]] = {
      case ValDef(ruleName, _, Some(Apply(term, List(Lambda(_, Match(_, cases: List[CaseDef])))))) =>
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
            Terminal(NameTransformer.decode(name))
        }

        val extractNonTerminalRef: SymbolExtractor = {
          case Unapply(Select(extractName(name), "unapply"), Nil, List(bind)) =>
            // (name, bind) //available for future
            NonTerminal(name)
        }

        val extractOptional: SymbolExtractor = {
          case Bind(bind, Typed(_, Applied(tpt, List(arg @ Singleton(extractName(name))))))
              if tpt.tpe <:< TypeRepr.of[Option] =>

            report.errorAndAbort("Optional symbols are not yet supported")
          // if arg.tpe <:< TypeRepr.of[Rule] then NonTerminal(name, isOptional = true)
          // else if arg.tpe <:< TypeRepr.of[Token[?, ?, ?]] then Terminal(name, isOptional = true)
          // else raiseShouldNeverBeCalled(arg.tpe.show)
        }

        val extractRepeated: SymbolExtractor = {
          case Bind(bind, Typed(_, Applied(tpt, List(arg @ Singleton(extractName(name))))))
              if tpt.tpe <:< TypeRepr.of[Seq] =>
            report.errorAndAbort("Repeated symbols are not yet supported")
          // if arg.tpe <:< TypeRepr.of[Rule] then NonTerminal(name, isRepeated = true)
          // else if arg.tpe <:< TypeRepr.of[Token[?, ?, ?]] then Terminal(name, isRepeated = true)
          // else raiseShouldNeverBeCalled(arg.tpe.show)
        }

        val extractSymbol: SymbolExtractor = {
          case extractTerminalRef(terminal) => terminal
          case extractNonTerminalRef(nonterminal) => nonterminal
          case extractOptional(optional) => optional
          case extractRepeated(repeated) => repeated
          case x => raiseShouldNeverBeCalled(x.toString)
        }

        cases.map {
          case CaseDef(pattern, Some(_), _) => throw new NotImplementedError("Guards are not supported yet")
          // todo: i hope we can abandon this case
          case CaseDef(pattern @ TypedOrTest(Unapply(_, _, List(_)), _), None, _) =>
            Production(NonTerminal(ruleName), List(extractSymbol(pattern)))
          case CaseDef(TypedOrTest(Unapply(_, _, patterns), _), None, _) =>
            Production(NonTerminal(ruleName), patterns.map(extractSymbol))
          case CaseDef(pattern, None, _) =>
            Production(NonTerminal(ruleName), List(extractSymbol(pattern)))
        }
    }

    val rules =
      TypeRepr
        .of[P]
        .typeSymbol
        .declaredFields
        .filter(_.typeRef <:< TypeRepr.of[Rule])
        .map(_.tree)

    val productions = rules.flatMap(extractProductions)

    Expr(ParseTable(productions))
  }

  private def apply(productions: List[Production]): ParseTable = {
    val firstSet = FirstSet(productions)
    var currStateId = 0
    val states = mutable.ListBuffer(State.fromItem(State.empty, productions.head.toItem(), productions, firstSet))
    val table = mutable.Map.empty[(Int, Symbol), ParseAction]

    while states.sizeIs > currStateId do {
      val currState = states(currStateId)

      for (item <- currState if item.isLastItem) {
        table += ((currStateId, item.lookAhead) -> ParseAction.Reduction(item.production))
      }

      for (stepSymbol <- currState.possibleSteps) {
        val newState = currState.nextState(stepSymbol, productions, firstSet)

        states.indexOf(newState) match {
          case -1 =>
            table += ((currStateId, stepSymbol) -> ParseAction.Shift(states.length))
            states += newState
          case stateId =>
            table += ((currStateId, stepSymbol) -> ParseAction.Shift(stateId))
        }
      }

      currStateId += 1
    }

    table.toMap.tap { (table: ParseTable) =>
      // todo: remove hardcoded path
      Using.resource(new FileWriter("/Users/bartlomiejkozak/IdeaProjects/alpaca/parser.dbg")) { writer =>
        writer.write(table.show)
      }
    }
  }

  given Showable[ParseTable] = { table =>
    val symbols = table.keysIterator.map(_.stepSymbol).distinct.toList

    def centerText(text: String, width: Int = 10): String =
      if text.length >= width then text
      else {
        val padding = width - text.length
        val leftPad = padding / 2
        val rightPad = padding - leftPad
        (" " * leftPad) + text + (" " * rightPad)
      }

    val result = new StringBuilder
    result.append(centerText("State"))
    result.append("|")
    for (s <- symbols) {
      result.append(centerText(s.show))
      result.append("|")
    }

    for (i <- 0 to 13) {
      result.append('\n')
      result.append(centerText(i.toString))
      result.append("|")
      for (s <- symbols) {
        result.append(centerText(table.get((i, s)).fold("")(_.show)))
        result.append("|")
      }
    }
    result.append('\n')
    result.result()
  }

  private given ToExpr[(state: Int, stepSymbol: Symbol)] with
    def apply(x: (state: Int, stepSymbol: Symbol))(using Quotes): Expr[(state: Int, stepSymbol: Symbol)] =
      Expr(x.toTuple)

  given ToExpr[ParseTable] with
    def apply(x: ParseTable)(using Quotes): Expr[ParseTable] = '{ ${ Expr(x.toList) }.toMap }
}
