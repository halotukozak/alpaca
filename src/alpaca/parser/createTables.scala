package alpaca
package parser

import alpaca.core.{NonEmptyList as NEL, *}
import alpaca.core.Csv.toCsv
import alpaca.core.Showable.mkShow
import alpaca.parser.Symbol.{NonTerminal, Terminal}
import alpaca.parser.context.AnyGlobalCtx
import alpaca.writeToFile

import scala.collection.mutable
import scala.quoted.*
import scala.reflect.NameTransformer

inline private[parser] def createTables[Ctx <: AnyGlobalCtx, R, P <: Parser[Ctx]](
  using settings: ParserSettings,
): (parseTable: ParseTable, actionTable: ActionTable[Ctx, R]) =
  ${ createTablesImpl[Ctx, R, P]('{ settings }) }

//todo: there are many collections here, consider View, Iterator, Vector etc to optimize time and memory usage
private def createTablesImpl[Ctx <: AnyGlobalCtx: Type, R: Type, P <: Parser[Ctx]: Type](
  settings: Expr[ParserSettings],
)(using quotes: Quotes,
): Expr[(parseTable: ParseTable, actionTable: ActionTable[Ctx, R])] = {
  import quotes.reflect.*

  // todo: find a way to debug at compile time (how to avoid sandboxing?)
  val debug = mutable.ListBuffer.empty[Expr[Unit]]
  def addToDebug(expr: Expr[Unit]): Unit =
    debug += '{ if $settings.debug then $expr else {} } // todo make it compiletime if

  val ctxSymbol = TypeRepr.of[P].typeSymbol.methodMember("ctx").head
  val parserName = TypeRepr.of[P].typeSymbol.name.stripSuffix("$")
  val replaceRefs = new ReplaceRefs[quotes.type]
  val createLambda = new CreateLambda[quotes.type]

  // todo: it's extremally too long
  def extractEBNF: Tree => List[(production: Production, action: Expr[Action[Ctx, R]])] = {
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

      val extractName: PartialFunction[Tree, String] =
        case Select(This(_), name) => NameTransformer.decode(name)
        case Ident(name) => NameTransformer.decode(name)
        case Literal(StringConstant(name)) => NameTransformer.decode(name)

      val extractBind: PartialFunction[Tree, Option[Bind]] =
        case bind: Bind => Some(bind)
        case Ident("_") => None
        case x => raiseShouldNeverBeCalled(x.show)

      val skipTypedOrTest: PartialFunction[Tree, Tree] =
        case TypedOrTest(tree, _) => tree
        case tree => tree

      type EBNFExtractor = PartialFunction[
        Tree,
        (
          symbol: alpaca.parser.Symbol,
          bind: Option[Bind],
          others: List[(production: Production, action: Expr[Action[Ctx, R]])],
        ),
      ]

      val extractTerminalRef: EBNFExtractor =
        case skipTypedOrTest(
              Unapply(
                Select(
                  TypeApply(
                    Select(Apply(Select(_, "selectDynamic"), List(extractName(name))), "$asInstanceOf$"),
                    List(asInstanceOfType),
                  ),
                  "unapply",
                ),
                Nil,
                List(extractBind(bind)),
              ),
            ) =>
          (
            symbol = Terminal(name),
            bind = bind,
            others = Nil,
          )
        case skipTypedOrTest(
              Unapply(
                Apply(
                  Select(_, "unapply"),
                  List(
                    TypeApply(
                      Select(Apply(Select(_, "selectDynamic"), List(extractName(name))), "$asInstanceOf$"),
                      List(asInstanceOfType),
                    ),
                  ),
                ),
                Nil,
                List(extractBind(bind)),
              ),
            ) =>
          (
            symbol = Terminal(name),
            bind = bind,
            others = Nil,
          )

      val extractOptionalTerminal: EBNFExtractor =
        case skipTypedOrTest(
              Unapply(
                Select(
                  Apply(
                    Select(_, "Option"),
                    List(
                      TypeApply(
                        Select(Apply(Select(_, "selectDynamic"), List(extractName(name))), "$asInstanceOf$"),
                        List(asInstanceOfType),
                      ),
                    ),
                  ),
                  "unapply",
                ),
                Nil,
                List(extractBind(bind)),
              ),
            ) =>

          val fresh = NonTerminal.fresh(name)
          (
            symbol = fresh,
            bind = bind,
            others = List(
              (
                production = Production(fresh, NEL(alpaca.parser.Symbol.Empty)),
                action = '{ (_, _) => None },
              ),
              (
                production = Production(fresh, NEL(Terminal(name))),
                action = '{ (_, children) => Some(children.head) },
              ),
            ),
          )

      val extractRepeatedTerminal: EBNFExtractor =
        case skipTypedOrTest(
              Unapply(
                Select(
                  Apply(
                    Select(_, "List"),
                    List(
                      TypeApply(
                        Select(Apply(Select(_, "selectDynamic"), List(extractName(name))), "$asInstanceOf$"),
                        List(asInstanceOfType),
                      ),
                    ),
                  ),
                  "unapply",
                ),
                Nil,
                List(extractBind(bind)),
              ),
            ) =>

          val fresh = NonTerminal.fresh(name)
          (
            symbol = fresh,
            bind = bind,
            others = List(
              (
                production = Production(fresh, NEL(alpaca.parser.Symbol.Empty)),
                action = '{ (_, _) => Nil },
              ),
              (
                production = Production(fresh, NEL(NonTerminal(name), fresh)),
                action = '{ (ctx, children) =>
                  println(children)
                  ???
                },
              ),
            ),
          )

      val extractNonTerminalRef: EBNFExtractor =
        case skipTypedOrTest(Unapply(Select(extractName(name), "unapply"), Nil, List(extractBind(bind)))) =>
          (
            symbol = NonTerminal(name),
            bind = bind,
            others = Nil,
          )

      val extractOptionalNonTerminal: EBNFExtractor =
        case skipTypedOrTest(
              Unapply(
                Select(
                  Apply(
                    TypeApply(Select(_, "Option"), List(asInstanceOfType)),
                    List(extractName(name)),
                  ),
                  unapply,
                ),
                Nil,
                List(extractBind(bind)),
              ),
            ) =>
          val fresh = NonTerminal.fresh(name)
          (
            symbol = fresh,
            bind = bind,
            others = List(
              (
                production = Production(fresh, NEL(alpaca.parser.Symbol.Empty)),
                action = '{ (_, _) => None },
              ),
              (
                production = Production(fresh, NEL(NonTerminal(name))),
                action = '{ (_, children) => Some(children.head) },
              ),
            ),
          )

      val extractRepeatedNonTerminal: EBNFExtractor =
        case skipTypedOrTest(
              Unapply(
                Select(
                  Apply(
                    TypeApply(Select(_, "List"), List(asInstanceOfType)),
                    List(extractName(name)),
                  ),
                  unapply,
                ),
                Nil,
                List(extractBind(bind)),
              ),
            ) =>
          val fresh = NonTerminal.fresh(name)
          (
            symbol = fresh,
            bind = bind,
            others = List(
              (
                production = Production(fresh, NEL(alpaca.parser.Symbol.Empty)),
                action = '{ (_, _) => Nil },
              ),
              (
                production = Production(fresh, NEL(NonTerminal(name), fresh)),
                action = '{ (ctx, children) =>
                  println(children)
                  ???
                },
              ),
            ),
          )

      val extractEBNFAndAction: EBNFExtractor =
        case extractNonTerminalRef(nonterminal) => nonterminal
        case extractOptionalNonTerminal(optionalNonTerminal) => optionalNonTerminal
        case extractRepeatedNonTerminal(repeatedNonTerminal) => repeatedNonTerminal
        case extractTerminalRef(terminal) => terminal
        case extractOptionalTerminal(optionalTerminal) => optionalTerminal
        case extractRepeatedTerminal(repeatedTerminal) => repeatedTerminal
        case x => raiseShouldNeverBeCalled(x.toString)

      cases
        .flatMap:
          case CaseDef(pattern, Some(_), rhs) =>
            throw new NotImplementedError("Guards are not supported yet")
          // Tuple1
          case CaseDef(skipTypedOrTest(pattern @ Unapply(_, _, List(_))), None, rhs) =>
            val (symbol, bind, others) = extractEBNFAndAction(pattern)
            (
              production = Production(NonTerminal(ruleName), NEL(symbol)),
              action = createAction(List(bind), rhs),
            ) :: others

          // TupleN, N > 1
          case CaseDef(skipTypedOrTest(p @ Unapply(_, _, patterns)), None, rhs) =>
            val (symbols, binds, others) = patterns.map(extractEBNFAndAction).unzip3(using _.toTuple)
            (
              production = Production(NonTerminal(ruleName), NEL(symbols.head, symbols.tail*)),
              action = createAction(binds, rhs),
            ) :: others.flatten
          case x =>
            raiseShouldNeverBeCalled(x.toString)

    case ValDef(_, _, rhs) => raiseShouldNeverBeCalled(rhs.toString)
  }

  val rules =
    TypeRepr
      .of[P]
      .typeSymbol
      .declarations
      .filter(_.typeRef <:< TypeRepr.of[PartialFunction[Tuple, Any]])
      .map(_.tree)

  val table = rules.flatMap(extractEBNF).tap { table =>
    writeToFile(s"$parserName/productions.dbg")(table.map(_.production).mkShow("\n"))
    writeToFile(s"$parserName/actionTable.dbg.csv")(table.toCsv)
  }

  val root = table.collectFirst { case (p @ Production(NonTerminal("root"), _), _) => p }.get

  val parseTable = Expr {
    ParseTable(Production(parser.Symbol.Start, NEL(root.lhs)) :: table.map(_.production))
      .tap(parseTable => writeToFile(s"$parserName/parseTable.dbg.csv")(parseTable.toCsv))
  }

  val actionTable = Expr.ofList[(Production, Action[Ctx, R])] {
    table.map { case (production, action) => Expr.ofTuple(Expr(production) -> action) }
  }

  Expr.block(debug.toList, '{ ($parseTable: ParseTable, ActionTable($actionTable.toMap)) })
}
