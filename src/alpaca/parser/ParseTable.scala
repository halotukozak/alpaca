package alpaca
package parser

import alpaca.core.*
import alpaca.parser.Symbol.{NonTerminal, Terminal}

import java.io.FileWriter
import scala.collection.mutable
import scala.quoted.*
import scala.reflect.NameTransformer
import scala.util.Using
import scala.runtime.FunctionXXL
import alpaca.lexer.context.Lexem
import scala.collection.MapView

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

type F = Seq[Any] => Any

opaque type ActionTable <: Map[Production, F] = Map[Production, F]

inline def createTables[P <: Parser[?]]: (ParseTable, ActionTable) = ${ applyImpl[P] }

//todo: there are many collections here, consider View, Iterator, Vector etc to optimize time and memory usage
private def applyImpl[P <: Parser[?]: Type](using quotes: Quotes): Expr[(ParseTable, ActionTable)] = {
  import quotes.reflect.*

  val replaceRefs = new ReplaceRefs[quotes.type]
  val createLambda = new CreateLambda[quotes.type]

  def extractProductions: PartialFunction[Tree, List[(production: Production, lambda: Expr[F])]] =
    case ValDef(ruleName, _, Some(Lambda(_, Match(_, cases: List[CaseDef])))) =>

      type SymbolExtractor = PartialFunction[Tree, (symbol: parser.Symbol, bind: Option[Bind])]

      def extractName: PartialFunction[Tree, String] =
        case Select(This(kupadupa), name) => name

      def extractBind: PartialFunction[Tree, Option[Bind]] =
        case bind: Bind => Some(bind)
        case Ident("_") => None
        case x => raiseShouldNeverBeCalled(x.show)

      val extractTerminalRef: SymbolExtractor =
        case Unapply(
              Select(
                TypeApply(
                  Select(Apply(Select(_, "selectDynamic"), List(Literal(StringConstant(name)))), "$asInstanceOf$"),
                  List(asInstanceOfType),
                ),
                "unapply",
              ),
              _,
              List(extractBind(bind)),
            ) =>
          // (name, asInstanceOfType, bind) //available for future
          Terminal(NameTransformer.decode(name)) -> bind

      val extractNonTerminalRef: SymbolExtractor =
        case Unapply(Select(Select(_, name), "unapply"), Nil, List(extractBind(bind))) =>
          NonTerminal(name) -> bind

      // val extractOptional: SymbolExtractor =
      //   case Bind(bind, Typed(_, Applied(tpt, List(arg @ Singleton(extractName(name))))))
      //       if tpt.tpe <:< TypeRepr.of[Option] =>

      //     throw new NotImplementedError("Optional symbols are not yet supported")
      //   // if arg.tpe <:< TypeRepr.of[Rule] then NonTerminal(name, isOptional = true)
      //   // else if arg.tpe <:< TypeRepr.of[Token[?, ?, ?]] then Terminal(name, isOptional = true)
      //   // else raiseShouldNeverBeCalled(arg.tpe.show)

      // val extractRepeated: SymbolExtractor =
      //   case Bind(bind, Typed(_, Applied(tpt, List(arg @ Singleton(extractName(name))))))
      //       if tpt.tpe <:< TypeRepr.of[Seq] =>
      //     throw new NotImplementedError("Repeated symbols are not yet supported")
      //   // if arg.tpe <:< TypeRepr.of[Rule] then NonTerminal(name, isRepeated = true)
      //   // else if arg.tpe <:< TypeRepr.of[Token[?, ?, ?]] then Terminal(name, isRepeated = true)
      //   // else raiseShouldNeverBeCalled(arg.tpe.show)

      val skipTypedOrTest: PartialFunction[Tree, Tree] =
        case TypedOrTest(tree, _) => tree
        case tree => tree

      val extractSymbol: SymbolExtractor = skipTypedOrTest.andThen:
        case extractTerminalRef(terminal) => terminal
        case extractNonTerminalRef(nonterminal) => nonterminal
        // case (extractOptional(optional)) => optional
        // case (extractRepeated(repeated)) => repeated

      def createAction(binds: List[Option[Bind]], rhs: Term) = createLambda[F] { case (methSym, (param: Term) :: Nil) =>
        val seqApplyMethod = param.select(TypeRepr.of[Seq[Any]].typeSymbol.methodMember("apply").head)
        val seq = param.asExprOf[Seq[Any]]

        val replacements = binds.zipWithIndex
          .collect:
            case (Some(bind), idx) => ((bind.symbol, bind.symbol.typeRef.asType), Expr(idx))
          .map:
            case ((bind, '[t]), idx) =>
              (
                find = bind,
                replace = '{ $seq.apply($idx).asInstanceOf[t] }.asTerm,
              )
            case x => raiseShouldNeverBeCalled(x.toString)

        replaceRefs(replacements*).transformTerm(rhs)(methSym)
      }

      cases
        .map:
          case CaseDef(pattern, Some(_), rhs) => throw new NotImplementedError("Guards are not supported yet")
          case CaseDef(skipTypedOrTest(pattern @ Unapply(_, _, List(_))), None, rhs) =>
            val (symbol, bind) = extractSymbol(pattern)
            Production(NonTerminal(ruleName), symbol :: Nil) -> createAction(List(bind), rhs)
          case CaseDef(skipTypedOrTest(Unapply(_, _, patterns)), None, rhs) =>
            val (symbols, binds) = patterns.map(extractSymbol).unzip(using _.toTuple)
            Production(NonTerminal(ruleName), symbols) -> createAction(binds, rhs)

  val rules =
    TypeRepr
      .of[P]
      .typeSymbol
      .declaredFields
      .filter(_.typeRef <:< TypeRepr.of[PartialFunction[Tuple, Any]])
      .map(_.tree)

  val actions = rules.flatMap(extractProductions)

  val root = actions.collectFirst { case (p @ Production(NonTerminal("root"), _), _) => p }.get

  val parseTable: Expr[ParseTable] = Expr(
    ParseTable(Production(parser.Symbol.Start, List(root.lhs)) :: actions.map(_.production)),
  )

  val actionTable = Expr.ofList {
    actions.map { case (production, lambda) => Expr.ofTuple((Expr(production), lambda)) }
  }

  '{ ($parseTable, $actionTable.toMap) }
}

object ParseTable {
  def apply(productions: List[Production]): ParseTable = {
    val firstSet = FirstSet(productions)
    var currStateId = 0
    val states =
      mutable.ListBuffer(
        State.fromItem(
          State.empty,
          productions.find(_.lhs == parser.Symbol.Start).get.toItem(),
          productions,
          firstSet,
        ),
      )
    val table = mutable.Map.empty[(Int, parser.Symbol), ParseAction]

    while states.sizeIs > currStateId do {
      val currState = states(currStateId)

      for item <- currState if item.isLastItem do {
        table += ((currStateId, item.lookAhead) -> ParseAction.Reduction(item.production))
      }

      for stepSymbol <- currState.possibleSteps do {
        val newState = currState.nextState(stepSymbol, productions, firstSet)

        states.indexOf(newState) match
          case -1 =>
            table += ((currStateId, stepSymbol) -> ParseAction.Shift(states.length))
            states += newState
          case stateId =>
            table += ((currStateId, stepSymbol) -> ParseAction.Shift(stateId))
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
