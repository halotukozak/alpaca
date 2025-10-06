package alpaca
package parser

import alpaca.core.*
import alpaca.core.Showable.mkShow
import alpaca.parser.Symbol.{NonTerminal, Terminal}
import alpaca.parser.context.AnyGlobalCtx
import alpaca.writeToFile

import scala.collection.mutable
import scala.quoted.*
import scala.reflect.NameTransformer
import alpaca.core.NonEmptyList as NEL

inline def createTables[Ctx <: AnyGlobalCtx, R, P <: Parser[Ctx]](
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

      type EBNFExtractor =
        PartialFunction[(pattern: Tree, rhs: Tree), List[(production: Production, action: Expr[Action[Ctx, R]])]]

      object extractName:
        def unapply(tree: Tree): Option[String] = Some(
          NameTransformer.decode {
            tree match
              case Select(This(_), name) => name
              case Ident(name) => name
              case Literal(StringConstant(name)) => name
              case _ => raiseShouldNeverBeCalled(tree.toString)
          },
        )

      val extractBind: PartialFunction[Tree, Option[Bind]] =
        case bind: Bind => Some(bind)
        case Ident("_") => None
        case x => raiseShouldNeverBeCalled(x.show)

      val extractTerminalRef: EBNFExtractor =
        case (
              Unapply(
                Select(
                  TypeApply(
                    Select(Apply(Select(_, "selectDynamic"), List(extractName(name))), "$asInstanceOf$"),
                    List(asInstanceOfType),
                  ),
                  "unapply",
                ),
                _,
                List(extractBind(bind)),
              ),
              rhs: Term,
            ) =>
          List(
            (
              production = Production(NonTerminal(ruleName), NonEmptyList(Terminal(name))),
              action = createAction(List(bind), rhs),
            ),
          )

      val extractNonTerminalRef: EBNFExtractor =
        case (Unapply(Select(extractName(name), "unapply"), Nil, List(extractBind(bind))), rhs: Term) =>
          List(
            (
              production = Production(NonTerminal(ruleName), NonEmptyList(NonTerminal(name))),
              action = createAction(List(bind), rhs),
            ),
          )

      val extractOptionalNonTerminal: EBNFExtractor =
        case (
              Unapply(
                Select(Apply(TypeApply(Ident("Option"), List(tTpe)), List(extractName(name))), "unapply"),
                Nil,
                List(extractBind(bind)),
              ),
              rhs: Term,
            ) =>
          val fresh = NonTerminal.fresh(name)
          List(
            (
              production = Production(fresh, NEL(alpaca.parser.Symbol.Empty)),
              action = '{ (_, _) => None },
            ),
            (
              production = Production(fresh, NEL(NonTerminal(name))),
              action = '{ (_, children) => Some(children.head) },
            ),
            (
              production = Production(NonTerminal(ruleName), NEL(fresh)),
              action = createAction(bind :: Nil, rhs),
            ),
          )

      val extractRepeatedNonTerminal: EBNFExtractor =
        case (
              Unapply(
                Select(Apply(TypeApply(Ident("List"), List(tTpe)), List(extractName(name))), "unapply"),
                Nil,
                List(extractBind(bind)),
              ),
              rhs: Term,
            ) =>
          val fresh = NonTerminal.fresh(name)
          List(
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
            (
              production = Production(NonTerminal(ruleName), NEL(fresh)),
              action = createAction(bind :: Nil, rhs),
            ),
          )

      val extractOptionalTerminal: EBNFExtractor =
        case (
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
              rhs: Term,
            ) =>
          val fresh = NonTerminal.fresh(name)
          List(
            (
              production = Production(fresh, NEL(alpaca.parser.Symbol.Empty)),
              action = '{ (_, _) => None },
            ),
            (
              production = Production(fresh, NEL(Terminal(name))),
              action = '{ (_, children) => Some(children.head) },
            ),
            (
              production = Production(NonTerminal(name), NEL(fresh)),
              action = createAction(bind :: Nil, rhs),
            ),
          )

      val skipTypedOrTest: PartialFunction[Tree, Tree] =
        case (TypedOrTest(tree, _)) => tree
        case tree => tree

      val skipTypedOrTest2: PartialFunction[(pattern: Tree, rhs: Tree), (pattern: Tree, rhs: Tree)] =
        case (TypedOrTest(tree, _), rhs) => (tree, rhs)
        case (tree, rhs) => (tree, rhs)

      val extractEBNFAndAction: EBNFExtractor = skipTypedOrTest2.andThen:
        case extractTerminalRef(terminal) => terminal
        case extractNonTerminalRef(nonterminal) => nonterminal
        case extractOptionalNonTerminal(optionalNonTerminal) => optionalNonTerminal
        case extractRepeatedNonTerminal(repeatedNonTerminal) => repeatedNonTerminal
        case extractOptionalTerminal(optionalTerminal) => optionalTerminal
        case x => raiseShouldNeverBeCalled(x.toString)

      cases
        .flatMap:
          case CaseDef(pattern, Some(_), rhs) => throw new NotImplementedError("Guards are not supported yet")
          case CaseDef(skipTypedOrTest(pattern @ Unapply(_, _, List(_))), None, rhs) =>
            extractEBNFAndAction((pattern, rhs))
          case CaseDef(skipTypedOrTest(Unapply(_, _, patterns)), None, rhs) =>
            patterns.flatMap(pattern => extractEBNFAndAction((pattern, rhs)))

    case ValDef(_, _, rhs) => raiseShouldNeverBeCalled(rhs.toString)
  }

  val rules =
    TypeRepr
      .of[P]
      .typeSymbol
      .declarations
      .filter(_.typeRef <:< TypeRepr.of[PartialFunction[Tuple, Any]])
      .map(_.tree)

  val table = rules.flatMap(extractEBNF).tap(table => writeToFile("actionTable.dbg")(table.mkShow("\n")))

  val root = table.collectFirst { case (p @ Production(NonTerminal("root"), _), _) => p }.get

  val parseTable = Expr {
    ParseTable(Production(parser.Symbol.Start, NEL(root.lhs)) :: table.map(_.production))
      .tap(parseTable => writeToFile("parseTable.dbg")(show"$parseTable"))
  }

  val actionTable = Expr.ofList[(Production, Action[Ctx, R])] {
    table.map { case (production, action) => Expr.ofTuple(Expr(production) -> action) }
  }

  Expr.block(debug.toList, '{ ($parseTable: ParseTable, ActionTable($actionTable.toMap)) })
}
