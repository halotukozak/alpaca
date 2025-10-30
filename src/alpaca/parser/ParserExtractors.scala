package alpaca.parser

import alpaca.core.{NonEmptyList as NEL, *}
import alpaca.parser.context.AnyGlobalCtx
import alpaca.parser.ParserExtractors.*

import scala.quoted.*
import scala.reflect.NameTransformer

/**
 * Internal utility class for extracting and transforming parser patterns.
 *
 * This class is used during macro expansion to analyze parser rule definitions
 * and extract information about terminals, non-terminals, and EBNF operators
 * (optional, repeated) from pattern matching expressions.
 *
 * @tparam Q the Quotes type
 * @tparam Ctx the parser context type
 */
private[parser] final class ParserExtractors[Q <: Quotes, Ctx <: AnyGlobalCtx: Type](using val quotes: Q) {
  import quotes.reflect.*

  private type EBNFExtractor = PartialFunction[
    Tree,
    (
      symbol: alpaca.parser.Symbol.NonEmpty,
      bind: Option[Bind],
      others: List[(production: Production, action: Expr[Action[Ctx]])],
    ),
  ]
  val skipTypedOrTest: PartialFunction[Tree, Tree] =
    case TypedOrTest(tree, _) => tree
    case tree => tree

  val extractEBNFAndAction: EBNFExtractor =
    case extractNonTerminalRef(nonterminal) => nonterminal
    case extractOptionalNonTerminal(optionalNonTerminal) => optionalNonTerminal
    case extractRepeatedNonTerminal(repeatedNonTerminal) => repeatedNonTerminal
    case extractTerminalRef(terminal) => terminal
    case extractOptionalTerminal(optionalTerminal) => optionalTerminal
    case extractRepeatedTerminal(repeatedTerminal) => repeatedTerminal
    case x => raiseShouldNeverBeCalled(x.show)

  private val extractName: PartialFunction[Tree, String] =
    case Select(This(_), name) => NameTransformer.decode(name)
    case Ident(name) => NameTransformer.decode(name)
    case Literal(StringConstant(name)) => NameTransformer.decode(name)

  private val extractBind: PartialFunction[Tree, Option[Bind]] =
    case bind: Bind => Some(bind)
    case Ident("_") => None
    case x => raiseShouldNeverBeCalled(x.show)

  private val extractTerminalRef: EBNFExtractor =
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
      (symbol = Terminal(name), bind = bind, others = Nil)
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
      (symbol = Terminal(name), bind = bind, others = Nil)

  private val extractOptionalTerminal: EBNFExtractor =
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
          (production = Production.Empty(fresh), action = '{ noneAction }),
          (production = Production.NonEmpty(fresh, NEL(Terminal(name))), action = '{ someAction }),
        ),
      )

  private val extractRepeatedTerminal: EBNFExtractor =
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
          (production = Production.Empty(fresh), action = '{ emptyRepeatedAction }),
          (production = Production.NonEmpty(fresh, NEL(fresh, NonTerminal(name))), action = '{ repeatedAction }),
        ),
      )

  private val extractNonTerminalRef: EBNFExtractor =
    case skipTypedOrTest(Unapply(Select(extractName(name), "unapply"), Nil, List(extractBind(bind)))) =>
      (symbol = NonTerminal(name), bind = bind, others = Nil)

  private val extractOptionalNonTerminal: EBNFExtractor =
    case skipTypedOrTest(Unapply(Select(Select(Select(_, name), "Option"), "unapply"), Nil, List(extractBind(bind)))) =>
      val fresh = NonTerminal.fresh(name)
      (
        symbol = fresh,
        bind = bind,
        others = List(
          (production = Production.Empty(fresh), action = '{ noneAction }),
          (production = Production.NonEmpty(fresh, NEL(NonTerminal(name))), action = '{ someAction }),
        ),
      )

  private val extractRepeatedNonTerminal: EBNFExtractor =
    case skipTypedOrTest(Unapply(Select(Select(Select(_, name), "List"), "unapply"), Nil, List(extractBind(bind)))) =>
      val fresh = NonTerminal.fresh(name)
      (
        symbol = fresh,
        bind = bind,
        others = List(
          (production = Production.Empty(fresh), action = '{ emptyRepeatedAction }),
          (production = Production.NonEmpty(fresh, NEL(fresh, NonTerminal(name))), action = '{ repeatedAction }),
        ),
      )
}

//noinspection ScalaWeakerAccess
private object ParserExtractors {
  val repeatedAction: Action[AnyGlobalCtx] =
    case (_, Seq(currList: List[?], newElem)) => currList.appended(newElem)
    case x => raiseShouldNeverBeCalled(x.toString)

  val emptyRepeatedAction: Action[AnyGlobalCtx] = (_, _) => Nil

  val someAction: Action[AnyGlobalCtx] =
    case (_, Seq(elem)) => Some(elem)
    case x => raiseShouldNeverBeCalled(x.toString)

  val noneAction: Action[AnyGlobalCtx] = (_, _) => None
}
