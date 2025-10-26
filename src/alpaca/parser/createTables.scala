package alpaca
package parser

import alpaca.core.{NonEmptyList as NEL, *}
import alpaca.core.Csv.toCsv
import alpaca.core.Showable.mkShow
import alpaca.core.ValidName.typeToString
import alpaca.debugToFile
import alpaca.lexer.Token
import alpaca.parser.context.AnyGlobalCtx
import alpaca.parser.ConflictResolution

import scala.quoted.*

opaque type Tables[Ctx <: AnyGlobalCtx] <: (parseTable: ParseTable, actionTable: ActionTable[Ctx]) =
  (parseTable: ParseTable, actionTable: ActionTable[Ctx])

object Tables:
  inline given [Ctx <: AnyGlobalCtx](
    using inline debugSettings: DebugSettings[?, ?],
  ): Tables[Ctx] = ${ createTablesImpl[Ctx]('{ debugSettings }) }

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
 * @param quotes the Quotes instance
 * @param debugSettings debug configuration
 * @return an expression containing the parse and action tables
 */
private def createTablesImpl[Ctx <: AnyGlobalCtx: Type](
  using quotes: Quotes,
)(
  debugSettings: Expr[DebugSettings[?, ?]],
): Expr[(parseTable: ParseTable, actionTable: ActionTable[Ctx])] = {
  import quotes.reflect.*

  given DebugSettings[?, ?] = debugSettings.value.getOrElse(report.errorAndAbort("DebugSettings must be defined inline"))

  val parserSymbol = Symbol.spliceOwner.owner.owner
  val parserTpe = parserSymbol.typeRef

  val ctxSymbol = parserSymbol.methodMember("ctx").head
  val parserName = parserSymbol.name.stripSuffix("$")
  val replaceRefs = new ReplaceRefs[quotes.type]
  val createLambda = new CreateLambda[quotes.type]
  val parserExtractor = new ParserExtractors[quotes.type, Ctx]
  import parserExtractor.*

  def extractEBNF(ruleName: String): Expr[Rule[?]] => Seq[(production: Production, action: Expr[Action[Ctx]])] = {
    case '{ rule(${ Varargs(cases) }*) } =>
      def createAction(binds: List[Option[Bind]], rhs: Term) = createLambda[Action[Ctx]]:
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

      val extractProductionName: Function[Tree, (Tree, ValidName | Null)] =
        case Typed(term, tpt) =>
          // todo: maybe it is possible to pattern match on TypeTree
          val AnnotatedType(_, annot) = tpt.tpe.runtimeChecked
          val '{ new `name`($name: ValidName) } = annot.asExpr.runtimeChecked
          term -> name.value.orNull
        case other => other -> null

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

  val rules = parserTpe.typeSymbol.declarations
    .filter:
      _.typeRef <:< TypeRepr.of[Rule[?]]
    .map:
      _.tree

  val table = rules
    .flatMap:
      case ValDef(ruleName, _, Some(rhs)) => extractEBNF(ruleName)(rhs.asExprOf[Rule[?]])
      case x => raiseShouldNeverBeCalled(x.show)
    .tap: table =>
      debugToFile(s"$parserName/actionTable.dbg.csv")(table.toCsv)

  val productions = table
    .map:
      _.production
    .tap: table =>
      debugToFile(s"$parserName/productions.dbg")(table.mkShow("\n"))

  object findProduction {
    private val productionsByName = productions
      .collect:
        case p if p.name != null => p.name -> p
      .toMap

    private val productionsByRhs = productions
      .collect: p =>
        p.rhs -> p
      .toMap

    def apply(prod: Expr[Production]): Production = prod match
      case '{ Production.ofName(${ Expr(name) }) } =>
        productionsByName.getOrElse(name, report.errorAndAbort(show"Production with name '$name' not found"))
      case '{ Production(${ Varargs(rhs) }*) } =>
        val args = rhs
          .map[alpaca.parser.Symbol.NonEmpty]:
            case '{ type ruleType <: Rule[?]; $rule: ruleType } => NonTerminal(TypeRepr.of[ruleType].termSymbol.name)
            case '{ type name <: ValidName; $token: Token[name, ?, ?] } => Terminal(typeToString[name])
          .toList

        productionsByRhs.getOrElse(
          NEL.unsafe(args),
          report.errorAndAbort(show"Production with RHS '${args.mkShow(" ")}' not found"),
        )
  }

  val resolutionExprs = scala.util
    .Try:
      parserTpe.typeSymbol.declaredField("resolutions").tree
    .map:
      case ValDef(_, _, Some(rhs)) => rhs.asExprOf[Set[ConflictResolution]]
    .map:
      case '{ Set.apply(${ Varargs(resolutionExprs) }*) } => resolutionExprs
    .getOrElse(Nil)

  def parseConflictResolution(
    acc: Map[Production | String, Set[Production | String]],
    before: Expr[Production | Token[?, ?, ?]],
    after: Expr[Production | Token[?, ?, ?]],
  ): Map[Production | String, Set[Production | String]] =
    val parsedBefore = before match
      case '{ $prod: Production } => findProduction(prod)
      case '{ $token: Token[name, ?, ?] } => typeToString[name]

    val parsedAfter = after match
      case '{ $prod: Production } => findProduction(prod)
      case '{ $token: Token[name, ?, ?] } => typeToString[name]

    acc.updatedWith(parsedBefore) {
      case Some(set) => Some(set + parsedAfter)
      case None => Some(Set(parsedAfter))
    }

  val conflictResolutionTable = ConflictResolutionTable(
    resolutionExprs
      .foldLeft(Map.empty[Production | String, Set[Production | String]]):
        case (acc, '{ ($after: Production | Token[?, ?, ?]).after(${ Varargs(befores) }*) }) =>
          befores.foldLeft(acc): (acc, before) =>
            parseConflictResolution(acc, before, after)

        case (acc, '{ ($before: Production | Token[?, ?, ?]).before(${ Varargs(afters) }*) }) =>
          afters.foldLeft(acc): (acc, after) =>
            parseConflictResolution(acc, before, after)

        case (acc, expr) => raiseShouldNeverBeCalled(expr.show),
  ).tap: table =>
    debugToFile(s"$parserName/conflictResolutions.dbg")(s"$table")

  val root = table
    .collectFirst:
      case (p @ Production.NonEmpty(NonTerminal("root"), _, _), _) => p
    .get

  val parseTable = Expr(
    ParseTable(
      Production.NonEmpty(alpaca.parser.Symbol.Start, NEL(root.lhs)) :: table.map(_.production),
      conflictResolutionTable,
    ).tap: parseTable =>
      debugToFile(s"$parserName/parseTable.dbg.csv")(parseTable.toCsv),
  )

  val actionTable = Expr.ofList(
    table.map:
      case (production, action) => Expr.ofTuple(Expr(production) -> action),
  )

  '{ ($parseTable: ParseTable, ActionTable($actionTable.toMap)) }
}
