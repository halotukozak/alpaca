package alpaca.parser

import alpaca.core.raiseShouldNeverBeCalled
import alpaca.ebnf.EBNF
import alpaca.parser.Symbol.{NonTerminal, Terminal}

import scala.quoted.Quotes
import scala.reflect.NameTransformer

private[parser] class EBNFExtractors[Q <: Quotes](using val quotes: Q) {
  import quotes.reflect.*

  type EBNFExtractor = PartialFunction[Tree, (ebnf: EBNF, bind: Option[Bind])]

  val extractName: PartialFunction[Tree, String] =
    case Select(This(_), name) => name
    case Ident(name) => name

  val extractBind: PartialFunction[Tree, Option[Bind]] =
    case bind: Bind => Some(bind)
    case Ident("_") => None
    case x => raiseShouldNeverBeCalled(x.show)

  val extractTerminalRef: EBNFExtractor =
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
      EBNF.Identifier(Terminal(NameTransformer.decode(name))) -> bind

  val extractNonTerminalRef: EBNFExtractor =
    case Unapply(Select(extractName(name), "unapply"), Nil, List(extractBind(bind))) =>
      EBNF.Identifier(NonTerminal(name)) -> bind

  val extractOptionalNonTerminal: EBNFExtractor =
    case Unapply(
          Select(Apply(TypeApply(Ident("Option"), List(tTpe)), List(extractName(name))), "unapply"),
          Nil,
          List(extractBind(bind)),
        ) =>
      EBNF.Optional(EBNF.Identifier(NonTerminal(name))) -> bind

  val extractRepeatedNonTerminal: EBNFExtractor =
    case Unapply(
          Select(Apply(TypeApply(Ident("List"), List(tTpe)), List(extractName(name))), "unapply"),
          Nil,
          List(extractBind(bind)),
        ) =>
      EBNF.ZeroOrMore(EBNF.Identifier(NonTerminal(name))) -> bind

  val extractOptionalTerminal: EBNFExtractor =
    case Unapply(
          Apply(
            Select(_, "unapply"),
            List(
              TypeApply(
                Select(Apply(Select(_, "selectDynamic"), List(Literal(StringConstant(name)))), "$asInstanceOf$"),
                List(asInstanceOfType),
              ),
            ),
          ),
          Nil,
          List(extractBind(bind)),
        ) =>
      EBNF.Optional(EBNF.Identifier(Terminal(NameTransformer.decode(name)))) -> bind

  val skipTypedOrTest: PartialFunction[Tree, Tree] =
    case TypedOrTest(tree, _) => tree
    case tree => tree

  val extractRhs: EBNFExtractor = skipTypedOrTest.andThen:
    case extractTerminalRef(terminal) => terminal
    case extractNonTerminalRef(nonterminal) => nonterminal
    case extractOptionalNonTerminal(optionalNonTerminal) => optionalNonTerminal
    case extractRepeatedNonTerminal(repeatedNonTerminal) => repeatedNonTerminal
    case extractOptionalTerminal(optionalTerminal) => optionalTerminal
    case x => raiseShouldNeverBeCalled(x.toString)

}
