package alpaca
package parser

import alpaca.core.{NonEmptyList as NEL, *}
import alpaca.core.Csv.toCsv
import alpaca.core.Showable.mkShow
import alpaca.debugToFile
import alpaca.lexer.context.Lexem
import alpaca.parser.context.AnyGlobalCtx

import scala.quoted.*
import scala.reflect.NameTransformer

/**
 * Creates the parse table and action table for a parser at compile time.
 *
 * This inline function is called by the Parser.parse method to generate
 * the tables needed for parsing. It extracts the grammar from the parser's
 * rule definitions and constructs the LR parse table and action table.
 *
 * @tparam Ctx the parser context type
 * @tparam R the result type of the parser
 * @tparam P the parser type
 * @param debugSettings debug configuration for generating debug output
 * @return a tuple of (parse table, action table)
 */
inline private[parser] def createTables[Ctx <: AnyGlobalCtx, R, P <: Parser[Ctx]](
  using inline debugSettings: DebugSettings[?, ?],
): (parseTable: ParseTable, actionTable: ActionTable[Ctx, R]) =
  ${ createTablesImpl[Ctx, R, P]('{ debugSettings }) }

/**
 * Macro implementation that builds parse and action tables at compile time.
 *
 * This is a complex macro that:
 * 1. Extracts grammar rules from the parser definition
 * 2. Converts them to productions with semantic actions
 * 3. Constructs the LR parse table
 * 4. Generates debug output if enabled
 *
 * Note: This implementation uses various collection types (List, Map, etc.).
 * Future optimizations may consider View, Iterator, or Vector to improve
 * compile-time performance and memory usage for large grammars.
 *
 * @tparam Ctx the parser context type
 * @tparam R the result type
 * @tparam P the parser type
 * @param quotes the Quotes instance
 * @param debugSettings debug configuration
 * @return an expression containing the parse and action tables
 */
private def createTablesImpl[Ctx <: AnyGlobalCtx: Type, R: Type, P <: Parser[Ctx]: Type](
  using quotes: Quotes,
)(
  debugSettings: Expr[DebugSettings[?, ?]],
): Expr[(parseTable: ParseTable, actionTable: ActionTable[Ctx, R])] = {
  import quotes.reflect.*

  given DebugSettings[?, ?] = debugSettings.value.getOrElse(report.errorAndAbort("DebugSettings must be defined inline"))

  val ctxSymbol = TypeRepr.of[P].typeSymbol.methodMember("ctx").head
  val parserName = TypeRepr.of[P].typeSymbol.name.stripSuffix("$")
  val replaceRefs = new ReplaceRefs[quotes.type]
  val createLambda = new CreateLambda[quotes.type]
  val parserExtractor = new ParserExtractors[quotes.type, Ctx, R]
  import parserExtractor.*

  def extractEBNF(ruleName: String): Expr[Rule] => Seq[(production: Production, action: Expr[Action[Ctx, R]])] = {
    case '{ rule(${ Varargs(cases) }*) } =>
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

      val extractProductionName: Function[Tree, (Tree, Option[ValidName])] =
        case Typed(term, tpt) =>
          // todo: maybe it is possible to pattern match on TypeTree
          val AnnotatedType(_, annot) = tpt.tpe.runtimeChecked
          val '{ new `name`($name: ValidName) } = annot.asExpr.runtimeChecked
          term -> name.value
        case other => other -> None

      cases
        .map: expr =>
          extractProductionName(expr.asTerm)
        .map:
          case (Lambda(_, Match(_, List(caseDef))), name) => caseDef -> name
          case (Lambda(_, Match(_, caseDefs)), name) =>
            report.errorAndAbort("Productions definition with multiple cases is not supported yet")
          case x =>
            raiseShouldNeverBeCalled(x.toString)
        .flatMap:
          case (CaseDef(pattern, Some(_), rhs), name) =>
            throw new NotImplementedError("Guards are not supported yet")
          // Tuple1
          case (CaseDef(skipTypedOrTest(pattern @ Unapply(_, _, List(_))), None, rhs), name) =>
            val (symbol, bind, others) = extractEBNFAndAction(pattern)
            (
              production = Production.NonEmpty(NonTerminal(ruleName), NEL(symbol), name),
              action = createAction(List(bind), rhs),
            ) :: others

          // TupleN, N > 1
          case (CaseDef(skipTypedOrTest(p @ Unapply(_, _, patterns)), None, rhs), name) =>
            val (symbols, binds, others) = patterns.map(extractEBNFAndAction).unzip3(using _.toTuple)
            (
              production = Production.NonEmpty(NonTerminal(ruleName), NEL(symbols.head, symbols.tail*), name),
              action = createAction(binds, rhs),
            ) :: others.flatten
          case (x, name) =>
            raiseShouldNeverBeCalled(x.show)
    case x => raiseShouldNeverBeCalled(x.show)
  }

  val rules =
    TypeRepr
      .of[P]
      .typeSymbol
      .declarations
      .filter(_.typeRef <:< TypeRepr.of[Rule])
      .map(_.tree)

  val table = rules
    .flatMap:
      case ValDef(ruleName, _, Some(rhs)) => extractEBNF(ruleName)(rhs.asExprOf[Rule])
      case x => raiseShouldNeverBeCalled(x.show)
    .tap: table =>
      debugToFile(s"$parserName/productions.dbg")(table.map(_.production).mkShow("\n"))
      debugToFile(s"$parserName/actionTable.dbg.csv")(table.toCsv)

  val root = table.collectFirst { case (p @ Production.NonEmpty(NonTerminal("root"), _, _), _) => p }.get

  val parseTable = Expr {
    ParseTable(Production.NonEmpty(parser.Symbol.Start, NEL(root.lhs)) :: table.map(_.production))
      .tap(parseTable => debugToFile(s"$parserName/parseTable.dbg.csv")(parseTable.toCsv))
  }

  val actionTable = Expr.ofList[(Production, Action[Ctx, R])] {
    table.map { case (production, action) => Expr.ofTuple(Expr(production) -> action) }
  }

  '{ ($parseTable: ParseTable, ActionTable($actionTable.toMap)) }
}
