package alpaca
package internal
package parser

import NonEmptyList as NEL

import alpaca.internal.parser.ParserExtractors.*

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
private[parser] final class ParserExtractors[Q <: Quotes, Ctx <: ParserCtx: Type](
  using val quotes: Q,
)(using DebugSettings,
) {
  import quotes.reflect.*
  private type EBNFExtractor = PartialFunction[
    Tree,
    (
      symbol: parser.Symbol.NonEmpty,
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

  // todo NameTransformer.decode once
  private val extractName: PartialFunction[Tree, String] =
    case Select(This(_), name) => NameTransformer.decode(name)
    case Ident(name) => NameTransformer.decode(name)
    case Literal(StringConstant(name)) => NameTransformer.decode(name)

  private val extractBind: PartialFunction[Tree, Option[Bind]] =
    case bind: Bind => Some(bind)
    case Ident("_") => None

  // todo: it should be compared with Symbol or sth
  private val extractMethodName: PartialFunction[Tree, String] =
    case Select(_, name) => name
    case Ident(name) => name
    case x => raiseShouldNeverBeCalled(x.toString)

  private val extractTerminalRef: EBNFExtractor =
    case skipTypedOrTest(
          Unapply(
            Select(
              Select(_, name),
              "unapply",
            ),
            Nil,
            List(extractBind(bind)),
          ),
        ) =>
      (symbol = Terminal(name), bind = bind, others = Nil)
    case skipTypedOrTest(
          Unapply(
            Select(
              TypeApply(
                Select(Apply(extractMethodName("selectDynamic"), List(extractName(name))), "$asInstanceOf$"),
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
              extractMethodName("unapply"),
              List(
                TypeApply(
                  Select(Apply(extractMethodName("selectDynamic"), List(extractName(name))), "$asInstanceOf$"),
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
                extractMethodName("Option"),
                List(
                  TypeApply(
                    Select(Apply(extractMethodName("selectDynamic"), List(extractName(name))), "$asInstanceOf$"),
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
          (
            production = Production.NonEmpty(fresh, NEL(Terminal(name))),
            action = '{ someAction(using ${ Expr(summon[DebugSettings]) }) },
          ),
        ),
      )

  private val extractRepeatedTerminal: EBNFExtractor =
    case skipTypedOrTest(
          Unapply(
            Select(
              Apply(
                extractMethodName("List"),
                List(
                  TypeApply(
                    Select(Apply(extractMethodName("selectDynamic"), List(extractName(name))), "$asInstanceOf$"),
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
          (
            production = Production.NonEmpty(fresh, NEL(fresh, NonTerminal(name))),
            action = '{ repeatedAction(using ${ Expr(summon[DebugSettings]) }) },
          ),
        ),
      )

  private val extractNonTerminalRef: EBNFExtractor =
    case skipTypedOrTest(Unapply(Select(extractName(name), "unapply"), Nil, List(extractBind(bind)))) =>
      (symbol = NonTerminal(name), bind = bind, others = Nil)

  private val extractOptionalNonTerminal: EBNFExtractor =
    case skipTypedOrTest(
          Unapply(Select(Select(extractName(name), "Option"), "unapply"), Nil, List(extractBind(bind))),
        ) =>
      val fresh = NonTerminal.fresh(name)
      (
        symbol = fresh,
        bind = bind,
        others = List(
          (production = Production.Empty(fresh), action = '{ noneAction }),
          (
            production = Production.NonEmpty(fresh, NEL(NonTerminal(name))),
            action = '{ someAction(using ${ Expr(summon[DebugSettings]) }) },
          ),
        ),
      )

  private val extractRepeatedNonTerminal: EBNFExtractor =
    case skipTypedOrTest(Unapply(Select(Select(extractName(name), "List"), "unapply"), Nil, List(extractBind(bind)))) =>
      val fresh = NonTerminal.fresh(name)
      (
        symbol = fresh,
        bind = bind,
        others = List(
          (production = Production.Empty(fresh), action = '{ emptyRepeatedAction }),
          (
            production = Production.NonEmpty(fresh, NEL(fresh, NonTerminal(name))),
            action = '{ repeatedAction(using ${ Expr(summon[DebugSettings]) }) },
          ),
        ),
      )
}

//noinspection ScalaWeakerAccess
private object ParserExtractors {
  val repeatedAction: DebugSettings ?=> Action[ParserCtx] =
    case (_, Seq(currList: List[?], newElem)) => currList.appended(newElem)
    case x => raiseShouldNeverBeCalled[List[?]](x)

  val emptyRepeatedAction: Action[ParserCtx] = (_, _) => Nil

  val someAction: DebugSettings ?=> Action[ParserCtx] =
    case (_, Seq(elem)) => Some(elem)
    case x => raiseShouldNeverBeCalled[Option[?]](x)

  val noneAction: Action[ParserCtx] = (_, _) => None
}
