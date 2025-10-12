package alpaca
package parser

import alpaca.core.{*, given}
import alpaca.parser.Symbol.{NonTerminal, Terminal}
import alpaca.parser.context.AnyGlobalCtx

import java.io.FileWriter
import scala.annotation.experimental
import scala.collection.mutable
import scala.quoted.*
import scala.reflect.NameTransformer
import scala.util.Using
import scala.NamedTuple.NamedTuple
import ParseAction.*
import alpaca.lexer.AlgorithmError
import scala.annotation.tailrec
import alpaca.core.Showable.mkShow

/** Represents an action the parser can take in a given state.
  *
  * The parser uses a parse table to determine what action to take based
  * on the current state and lookahead symbol.
  */
enum ParseAction:
  /** Shift a symbol onto the parse stack and transition to a new state.
    *
    * @param newState the state to transition to
    */
  case Shift(newState: Int)
  
  /** Reduce by applying a production rule.
    *
    * @param production the production to reduce by
    */
  case Reduction(production: Production)

/** Companion object for ParseAction. */
object ParseAction {
  given Showable[ParseAction] =
    case ParseAction.Shift(newState) => show"S$newState"
    case ParseAction.Reduction(Production(lhs, rhs)) => show"$lhs -> ${rhs.mkShow}"

  given ToExpr[ParseAction] with
    def apply(x: ParseAction)(using Quotes): Expr[ParseAction] = x match
      case ParseAction.Shift(i) => '{ ParseAction.Shift(${ Expr(i) }) }
      case ParseAction.Reduction(p) => '{ ParseAction.Reduction(${ Expr(p) }) }
}

/** An opaque type representing the LR parse table.
  *
  * The parse table maps from (state, symbol) pairs to parse actions.
  * This table is generated at compile time from the grammar.
  */
opaque type ParseTable = Map[(state: Int, stepSymbol: Symbol), ParseAction]

/** Type alias for semantic action functions.
  *
  * These functions are called when reducing by a production to compute
  * the semantic value of the non-terminal.
  *
  * @tparam Ctx the global context type
  * @tparam R the result type
  */
type Action[Ctx <: AnyGlobalCtx, R] = (Ctx, Seq[Any]) => Any

/** An opaque type representing the action table.
  *
  * The action table maps from productions to their semantic actions.
  * These actions are executed when the parser reduces by a production.
  *
  * @tparam Ctx the global context type
  * @tparam R the result type
  */
opaque type ActionTable[Ctx <: AnyGlobalCtx, R] = Map[Production, Action[Ctx, R]]

/** Companion object for ActionTable. */
object ActionTable {
  extension [Ctx <: AnyGlobalCtx, R](table: ActionTable[Ctx, R])
    def apply(production: Production): Action[Ctx, R] = table(production)
}

/** Creates parse and action tables from a parser definition.
  *
  * This is a compile-time macro that analyzes the parser's rules and
  * generates the LR parse table and associated action table.
  *
  * @tparam Ctx the global context type
  * @tparam R the result type
  * @tparam P the parser type
  * @return a tuple of (ParseTable, ActionTable)
  */
@experimental
inline def createTables[Ctx <: AnyGlobalCtx, R, P <: Parser[Ctx]]: (ParseTable, ActionTable[Ctx, R]) =
  ${ applyImpl[Ctx, R, P] }

//todo: there are many collections here, consider View, Iterator, Vector etc to optimize time and memory usage
@experimental
private def applyImpl[Ctx <: AnyGlobalCtx: Type, R: Type, P <: Parser[Ctx]: Type](
  using quotes: Quotes,
): Expr[(ParseTable, ActionTable[Ctx, R])] = {
  import quotes.reflect.*

  val ctxSymbol = TypeRepr.of[P].typeSymbol.methodMember("ctx").head
  val replaceRefs = new ReplaceRefs[quotes.type]
  val createLambda = new CreateLambda[quotes.type]

  def extractProductions: PartialFunction[Tree, List[(production: Production, lambda: Expr[Action[Ctx, R]])]] =
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

      val extractOptionalNonTerminal: SymbolExtractor =
        case Unapply(
              Select(Apply(TypeApply(Ident("Option"), List(tTpe)), List(extractName(name))), "unapply"),
              Nil,
              List(extractBind(bind)),
            ) =>
          // todo: https://github.com/halotukozak/alpaca/issues/24
          report.error("Optional non-terminals are not supported yet")
          NonTerminal(name, isOptional = true) -> bind

      val extractRepeatedNonTerminal: SymbolExtractor =
        case Unapply(
              Select(Apply(TypeApply(Ident("List"), List(tTpe)), List(extractName(name))), "unapply"),
              Nil,
              List(extractBind(bind)),
            ) =>
          // todo: https://github.com/halotukozak/alpaca/issues/23
          report.error("Repeated non-terminals are not supported yet")
          NonTerminal(name, isRepeated = true) -> bind

      val skipTypedOrTest: PartialFunction[Tree, Tree] =
        case TypedOrTest(tree, _) => tree
        case tree => tree

      val extractSymbol: SymbolExtractor = skipTypedOrTest.andThen:
        case extractTerminalRef(terminal) => terminal
        case extractNonTerminalRef(nonterminal) => nonterminal
        case extractOptionalNonTerminal(optionalNonTerminal) => optionalNonTerminal
        case extractRepeatedNonTerminal(repeatedNonTerminal) => repeatedNonTerminal

      def createAction(binds: List[Option[Bind]], rhs: Term) = createLambda[Action[Ctx, R]]:
        case (methSym, (ctx: Term) :: (param: Term) :: Nil) =>
          val seqApplyMethod = param.select(TypeRepr.of[Seq[Any]].typeSymbol.methodMember("apply").head)
          val seq = param.asExprOf[Seq[Any]]

          val replacements = (find = ctxSymbol, replace = ctx) ::
            binds.zipWithIndex
              .collect:
                case (Some(bind), idx) => ((bind.symbol, bind.symbol.typeRef.asType), Expr(idx))
              .map:
                case ((bind, '[t]), idx) => (find = bind, replace = '{ $seq.apply($idx).asInstanceOf[t] }.asTerm)
                case x => raiseShouldNeverBeCalled(x.toString)

          replaceRefs(replacements*).transformTerm(rhs)(methSym)

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

  val root = actions.collectFirst { case (p @ Production(NonTerminal("root", _, _), _), _) => p }.get

  val parseTable: Expr[ParseTable] = Expr(
    ParseTable(Production(parser.Symbol.Start, List(root.lhs)) :: actions.map(_.production)),
  )

  val actionTable = Expr.ofList {
    actions.map { case (production, lambda) => Expr.ofTuple((Expr(production), lambda)) }
  }

  '{ ($parseTable, $actionTable.toMap) }
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

    def addToTable(symbol: Symbol, action: ParseAction): Unit =
      table.get((currStateId, symbol)) match
        case None => table += ((currStateId, symbol) -> action)
        case Some(existingAction) =>
          val path = toPath(currStateId, List(symbol))
          (existingAction, action) match
            case (red1: Reduction, red2: Reduction) => throw ReduceReduceConflict(red1, red2, path)
            case (Shift(_), red: Reduction) => throw ShiftReduceConflict(symbol, red, path)
            case (red: Reduction, Shift(_)) => throw ShiftReduceConflict(symbol, red, path)
            case (Shift(_), Shift(_)) => throw AlgorithmError("Shift-Shift conflict should never happen")

    @tailrec
    def toPath(stateId: Int, acc: List[Symbol] = Nil): List[Symbol] =
      if stateId == 0 then acc
      else
        val (sourceStateId, symbol) = table.collectFirst { case (key, Shift(`stateId`)) => key }.get
        toPath(sourceStateId, symbol :: acc)

    while states.sizeIs > currStateId do {
      val currState = states(currStateId)

      for item <- currState if item.isLastItem do {
        addToTable(item.lookAhead, Reduction(item.production))
      }

      for stepSymbol <- currState.possibleSteps do {
        val newState = currState.nextState(stepSymbol, productions, firstSet)

        states.indexOf(newState) match
          case -1 =>
            addToTable(stepSymbol, Shift(states.length))
            states += newState
          case stateId =>
            addToTable(stepSymbol, Shift(stateId))
      }

      currStateId += 1
    }

    table.toMap
  }

  given Showable[ParseTable] = { table =>
    val symbols = table.keysIterator.map(_.stepSymbol).distinct.toList
    val states = table.keysIterator.map(_.state).distinct.toList.sorted

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
    def apply(x: ParseTable)(using Quotes): Expr[ParseTable] =
      '{ ${ Expr(x.toList) }.toMap }
}
