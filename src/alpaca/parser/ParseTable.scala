package alpaca
package parser

import alpaca.core.{*, given}
import alpaca.ebnf.*
import alpaca.parser.Symbol.NonTerminal
import alpaca.parser.context.AnyGlobalCtx
import alpaca.parser.ParseTable.given_Showable_ParseTable

import java.io.FileWriter
import scala.annotation.experimental
import scala.collection.mutable
import scala.quoted.*
import scala.NamedTuple.NamedTuple
import scala.util.Using

enum ParseAction:
  case Shift(newState: Int)
  case Reduction(production: Production)

object ParseAction {
  given Showable[ParseAction] =
    case ParseAction.Shift(newState) => show"S$newState"
    case ParseAction.Reduction(production) => show"$production"

  given ToExpr[ParseAction] with
    def apply(x: ParseAction)(using Quotes): Expr[ParseAction] = x match
      case ParseAction.Shift(i) => '{ ParseAction.Shift(${ Expr(i) }) }
      case ParseAction.Reduction(p) => '{ ParseAction.Reduction(${ Expr(p) }) }
}

opaque type ParseTable = Map[(state: Int, stepSymbol: Symbol), ParseAction]

type Action[Ctx <: AnyGlobalCtx, R] = (Ctx, Seq[Any]) => Any

opaque type ActionTable[Ctx <: AnyGlobalCtx, R] = Map[Production, Action[Ctx, R]]

object ActionTable {
  extension [Ctx <: AnyGlobalCtx, R](table: ActionTable[Ctx, R])
    def apply(production: Production): Action[Ctx, R] = table(production)
}

inline def createTables[Ctx <: AnyGlobalCtx, R, P <: Parser[Ctx]]
  : (parseTable: ParseTable, actionTable: ActionTable[Ctx, R], debug: () => Unit) =
  ${ applyImpl[Ctx, R, P] }

//todo: there are many collections here, consider View, Iterator, Vector etc to optimize time and memory usage

private def applyImpl[Ctx <: AnyGlobalCtx: Type, R: Type, P <: Parser[Ctx]: Type](
  using quotes: Quotes,
): Expr[(parseTable: ParseTable, actionTable: ActionTable[Ctx, R], debug: () => Unit)] = {
  import quotes.reflect.*

  val debug = mutable.ListBuffer.empty[Expr[Unit]]

  val ctxSymbol = TypeRepr.of[P].typeSymbol.methodMember("ctx").head
  val replaceRefs = new ReplaceRefs[quotes.type]
  val createLambda = new CreateLambda[quotes.type]
  val EBNFExtractors = new EBNFExtractors[quotes.type]
  import EBNFExtractors.*

  def extractEBNF: PartialFunction[Tree, List[(definition: EBNF.Definition, lambda: Expr[Action[Ctx, R]])]] =
    case ValDef(ruleName, _, Some(Lambda(_, Match(_, cases: List[CaseDef])))) =>
      def createAction(binds: List[Option[Bind]], rhs: Term) = createLambda[Action[Ctx, R]]:
        case (methSym, (ctx: Term) :: (param: Term) :: Nil) =>
          val seqApplyMethod = param.select(TypeRepr.of[Seq[Any]].typeSymbol.methodMember("apply").head)
          val seq = param.asExprOf[Seq[Any]]

          val replacements = (find = ctxSymbol, replace = ctx) ::
            binds.zipWithIndex
              .collect:
                case (Some(bind), idx) => ((bind.symbol, bind.symbol.typeRef.asType), Expr(idx))
              .map:
                case ((bind, '[t]), idx) => (find = bind, replace = '{ $seq($idx).asInstanceOf[t] }.asTerm)
                case x => raiseShouldNeverBeCalled(x.toString)

          replaceRefs(replacements*).transformTerm(rhs)(methSym)

      cases
        .map:
          case CaseDef(pattern, Some(_), rhs) => throw new NotImplementedError("Guards are not supported yet")
          case CaseDef(skipTypedOrTest(pattern @ Unapply(_, _, List(_))), None, rhs) =>
            val (ebnf, bind) = extractRhs(pattern)
            EBNF.Definition(NonTerminal(ruleName), ebnf) -> createAction(List(bind), rhs)
          case CaseDef(skipTypedOrTest(Unapply(_, _, patterns)), None, rhs) =>
            val (ebnf, binds) = patterns.map(extractRhs).unzip(using _.toTuple)
            EBNF.Definition(NonTerminal(ruleName), EBNF.Concatenation(ebnf)) -> createAction(binds, rhs)
    case ValDef(_, _, rhs) => raiseShouldNeverBeCalled(rhs.toString)

  val rules =
    TypeRepr
      .of[P]
      .typeSymbol
      .declarations
      .filter(_.typeRef <:< TypeRepr.of[PartialFunction[Tuple, Any]])
      .map(_.tree)

  val ebnfTable =
    rules.flatMap(extractEBNF).tap { table =>
      val x = Expr(table.map(_.definition).mkString("\n"))
      debug += '{ writeToFile("ebnfTable.dbg")($x) }
    }

  val actions: List[(production: Production, lambda: Expr[Action[Ctx, R]])] = {
    for
      (ebnf, lambda) <- ebnfTable
      production <- ebnf.toBNF
    yield production -> lambda
  }.tap { table =>
    val x = Expr(
      table
        .map { case (production, lambda) => show"$production => ${lambda.asTerm.show(using Printer.TreeShortCode)}" }
        .mkString("\n"),
    )
    debug += '{ writeToFile("actionTable.dbg")($x) }
  }

  val root = actions.collectFirst { case (p @ Production(NonTerminal("root"), _), _) => p }.get

  val parseTable = Expr[ParseTable](
    ParseTable(Production(parser.Symbol.Start, List(root.lhs)) :: actions.map(_.production)),
  ).tap { table =>
    debug += '{ writeToFile("parseTable.dbg")(given_Showable_ParseTable.show($table)) }
  }

  val actionTable = Expr.ofList[(Production, Action[Ctx, R])] {
    actions.map { case (production, lambda) => Expr.ofTuple(Expr(production) -> lambda) }
  }

  '{ ($parseTable, $actionTable.toMap, () => ${ Expr.block(debug.toList, '{}) }) }
}

object ParseTable {
  extension (table: ParseTable) def apply(state: Int, symbol: Symbol): ParseAction = table((state, symbol))

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
    val table = mutable.Map.empty[(state: Int, stepSymbol: Symbol), ParseAction]

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

    table.toMap
  }

  given Showable[ParseTable] = { table =>
    val symbols = table.keysIterator.map(_.stepSymbol).toSet
    val states = table.keysIterator.map(_.state).to(collection.SortedSet)

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

    for (i <- states) {
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

  given ToExpr[ParseTable] with
    def apply(x: ParseTable)(using Quotes): Expr[ParseTable] = '{ ${ Expr(x.toList) }.toMap }
}
