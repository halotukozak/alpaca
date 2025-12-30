package alpaca
package internal
package parser

import NonEmptyList as NEL

import alpaca.internal.Csv.toCsv
import alpaca.internal.lexer.Token

/**
 * An opaque type containing the parse and action tables for the parser.
 *
 * The parse table is used to drive the LR parsing algorithm, while the
 * action table maps productions to their semantic actions. These tables
 * are generated at compile time by analyzing the grammar rules.
 *
 * @tparam Ctx the parser context type
 */
opaque private[alpaca] type Tables[Ctx <: ParserCtx] <: (parseTable: ParseTable, actionTable: ActionTable[Ctx]) =
  (parseTable: ParseTable, actionTable: ActionTable[Ctx])

object Tables:
  /**
   * Automatically generates parse and action tables from a parser definition.
   *
   * This given instance triggers compile-time macro expansion to analyze
   * the parser's grammar rules and generate the necessary tables.
   *
   * @tparam Ctx the parser context type
   * @return the generated parse and action tables
   */
  inline given [Ctx <: ParserCtx]: Tables[Ctx] = ${ createTablesImpl[Ctx] }

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
 * @param quotes the Quotes instance
 * @return an expression containing the parse and action tables
 */
private def createTablesImpl[Ctx <: ParserCtx: Type](using quotes: Quotes)
  : Expr[(parseTable: ParseTable, actionTable: ActionTable[Ctx])] = withDebugSettings:
  import quotes.reflect.*

  val parserSymbol = Symbol.spliceOwner.owner.owner
  val parserTpe = parserSymbol.typeRef

  val ctxSymbol = parserSymbol.methodMember("ctx").head
  val parserName = parserSymbol.name.stripSuffix("$")
  val replaceRefs = new ReplaceRefs[quotes.type]
  val createLambda = new CreateLambda[quotes.type]
  val parserExtractor = new ParserExtractors[quotes.type, Ctx]
  import parserExtractor.*

  def extractEBNF(ruleName: String)
    : PartialFunction[Expr[Rule[?]], Seq[(production: Production, action: Expr[Action[Ctx]])]] =
    case '{ rule(${ Varargs(cases) }*) } =>
      def createAction(binds: List[Option[Bind]], rhs: Term) = createLambda[Action[Ctx]]:
        case (methSym, (ctx: Term) :: (param: Term) :: Nil) =>
          val seqApplyMethod = param.select(TypeRepr.of[Seq[Any]].typeSymbol.methodMember("apply").head)
          val seq = param.asExprOf[Seq[Any]]

          val replacements = (find = Ref(ctxSymbol), replace = ctx) ::
            binds.zipWithIndex
              .collect:
                case (Some(bind), idx) => ((bind, bind.symbol.typeRef.asType), Expr(idx))
              .unsafeFlatMap:
                case ((bind, '[t]), idx) =>
                  Some((find = bind, replace = '{ $seq($idx).asInstanceOf[t] }.asTerm.changeOwner(methSym)))

          replaceRefs(replacements*).transformTerm(rhs)(methSym)

      val extractProductionName: Function[Expr[ProductionDefinition[?]], (Tree, ValidName | Null)] =
        case '{ ($name: ValidName).apply($production: ProductionDefinition[?]) } =>
          production.asTerm -> name.value.orNull
        case other =>
          other.asTerm -> null

      cases
        .map(extractProductionName)
        .map:
          case (Lambda(_, Match(_, List(caseDef))), name) => caseDef -> name
          case (Lambda(_, Match(_, caseDefs)), name) =>
            report.errorAndAbort("Productions definition with multiple cases is not supported yet")
          case (other, name) =>
            report.errorAndAbort(show"Unexpected production definition: $other")
        .unsafeFlatMap:
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

  val rules = parserTpe.typeSymbol.declarations.collect:
    case decl if decl.typeRef <:< TypeRepr.of[Rule[?]] => decl.tree // todo: can we avoid .tree?

  debug("Rules extracted, building parse table...")

  val table = rules
    .unsafeFlatMap:
      case ValDef(ruleName, _, Some(rhs)) => extractEBNF(ruleName)(rhs.asExprOf[Rule[?]])
      case DefDef(ruleName, _, _, Some(rhs)) => extractEBNF(ruleName)(rhs.asExprOf[Rule[?]]) // todo: or error?
      case other: ValOrDefDef if other.rhs.isEmpty => report.errorAndAbort("Enable -Yretain-trees compiler flag")
    .tap: table =>
      // csv may be not the best format for this due to the commas
      debugToFile(s"$parserName/actionTable.dbg.csv")(table.toCsv)

  debug("Productions extracted, building conflict resolution table...")

  val productions = table
    .map(_.production)
    .tap: table =>
      debugToFile(s"$parserName/productions.dbg")(table.mkShow("\n"))

  debug("Productions extracted, building parse and action tables...")

  val findProduction: Expr[Production] => Production =
    val productionsByName = productions
      .collect:
        case p if p.name != null => p.name -> p
      .toMap

    val productionsByRhs = productions
      .collect: p =>
        p.rhs -> p
      .toMap
    import scala.reflect.Selectable.reflectiveSelectable
    {
      case '{ ($selector: ProductionSelector).selectDynamic(${ Expr(name) }).$asInstanceOf$[i] } =>
        productionsByName.getOrElse(name, report.errorAndAbort(show"Production with name '$name' not found"))
      case '{ alpaca.Production(${ Varargs(rhs) }*) } =>
        val args = rhs
          .map[parser.Symbol.NonEmpty]:
            case '{ type ruleType <: Rule[?]; $rule: ruleType } => NonTerminal(TypeRepr.of[ruleType].termSymbol.name)
            case '{ type name <: ValidName; $token: Token[name, ?, ?] } => Terminal(ValidName.from[name])
          .toList

        productionsByRhs.getOrElse(
          NEL.unsafe(args),
          report.errorAndAbort(show"Production with RHS '${args.mkShow(" ")}' not found"),
        )

      case definition => raiseShouldNeverBeCalled(definition)(using () => ???)
    }

  debug("Conflict resolution rules extracted, building conflict resolution table...")

  val resolutionExprs = scala.util
    .Try:
      parserTpe.typeSymbol.declaredField("resolutions").tree
    .map:
      case ValDef(_, _, Some(rhs)) => rhs.asExprOf[Set[ConflictResolution]]
      case DefDef(_, _, _, Some(rhs)) => rhs.asExprOf[Set[ConflictResolution]]
    .map:
      case '{ Set.apply(${ Varargs(resolutionExprs) }*) } => resolutionExprs
    .getOrElse(Nil)

  def extractKey(expr: Expr[Production | Token[?, ?, ?]]): ConflictKey = expr match
    case '{ $prod: Production } => ConflictKey(findProduction(prod))
    case '{ $token: Token[name, ?, ?] } => ConflictKey(ValidName.from[name])

  debug("Building conflict resolution table...")

  val conflictResolutionTable = ConflictResolutionTable(
    resolutionExprs.view
      .unsafeFlatMap:
        case '{ ($after: Production | Token[?, ?, ?]).after(${ Varargs(befores) }*) } => befores.map((_, after))
        case '{ ($before: Production | Token[?, ?, ?]).before(${ Varargs(afters) }*) } => afters.map((before, _))
      .foldLeft(Map.empty[ConflictKey, Set[ConflictKey]]):
        case (acc, (before, after)) =>
          acc.updatedWith(extractKey(before)):
            case Some(set) => Some(set + extractKey(after))
            case None => Some(Set(extractKey(after))),
  ).tap: table =>
    table.verifyNoConflicts()
    debugToFile(s"$parserName/conflictResolutions.dbg")(s"$table")

  debug("Conflict resolution table built, identifying root production...")

  val root = table
    .collectFirst:
      case (p @ Production.NonEmpty(NonTerminal("root"), _, _), _) => p
    .get

  debug("Root production identified, generating parse and action tables...")

  val parseTable = Expr(
    ParseTable(
      Production.NonEmpty(parser.Symbol.Start, NEL(root.lhs)) :: table.map(_.production),
      conflictResolutionTable,
    ).tap: parseTable =>
      debugToFile(s"$parserName/parseTable.dbg.csv")(parseTable.toCsv),
  )

  debug("Parse and action tables generated.")

  val actionTable = Expr.ofList(
    table.map:
      case (production, action) => Expr.ofTuple(Expr(production) -> action),
  )

  '{ ($parseTable: ParseTable, ActionTable($actionTable.toMap)) }
