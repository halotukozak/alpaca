package alpaca.parser

import scala.quoted.*
import alpaca.core.NonEmptyList.*
import alpaca.core.given
import alpaca.core.{NonEmptyList as NEL, *}
import alpaca.core.Csv.toCsv
import alpaca.core.Showable.mkShow
import alpaca.debugToFile
import alpaca.parser.context.AnyGlobalCtx

import scala.quoted.*
import scala.reflect.NameTransformer
import alpaca.lexer.context.Lexem

private[parser] final class ParserExtractors[Q <: Quotes, Ctx <: AnyGlobalCtx: Type, R: Type](using val quotes: Q) {
  import quotes.reflect.*

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
      symbol: alpaca.parser.Symbol.NonEmpty,
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
            production = Production.Empty(fresh),
            action = '{ (_, _) => None },
          ),
          (
            production = Production.NonEmpty(fresh, NEL(Terminal(name))),
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
            production = Production.Empty(fresh),
            action = '{ (_, _) => Nil },
          ),
          (
            production = Production.NonEmpty(fresh, NEL(fresh, NonTerminal(name))),
            action = '{
              { case (ctx, Seq(currList: List[?], newElem)) => currList.appended(newElem) }: Action[Ctx, R]
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
            Select(Select(Select(_, name), "Option"), "unapply"),
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
            production = Production.Empty(fresh),
            action = '{ (_, _) => None },
          ),
          (
            production = Production.NonEmpty(fresh, NEL(NonTerminal(name))),
            action = '{ (_, children) => Some(children.head) },
          ),
        ),
      )

  val extractRepeatedNonTerminal: EBNFExtractor =
    case skipTypedOrTest(
          Unapply(
            Select(Select(Select(_, name), "List"), "unapply"),
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
            production = Production.Empty(fresh),
            action = '{ (_, _) => Nil },
          ),
          (
            production = Production.NonEmpty(fresh, NEL(fresh, NonTerminal(name))),
            action = '{
              { case (ctx, Seq(currList: List[?], newElem)) => currList.appended(newElem) }: Action[Ctx, R]
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
    case x => raiseShouldNeverBeCalled(x.show)
}
