package alpaca
package internal
package parser

import NonEmptyList as NEL

import alpaca.internal.lexer.Token
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
private[parser] final class ParserExtractors[Q <: Quotes, Ctx <: ParserCtx: Type](using val quotes: Q)(using Log):
  import quotes.reflect.*

  val skipTypedOrTest: PartialFunction[Tree, Tree] =
    case TypedOrTest(tree, _) => tree
    case tree => tree

  private type SymbolExtractor = PartialFunction[Tree, (name: String, bind: Option[Bind], extractor: String | Null)]

  private enum Extractor[T: Type] extends SymbolExtractor:
    case Terminal extends Extractor[Token[?, ?, ?]]
    case NonTerminal extends Extractor[Rule[?]]

    private val underlying: SymbolExtractor =
      case skipTypedOrTest(
            Unapply(Select(Extractor.Unpack(term, name, extractor), Names.Unapply), Nil, List(Extractor.Bind(bind))),
          ) if term.tpe <:< TypeRepr.of[T] =>
        (NameTransformer.decode(name), bind, extractor)
    override def isDefinedAt(x: Tree): Boolean = underlying.isDefinedAt(x)
    override def apply(x: Tree): (name: String, bind: Option[Bind], extractor: String | Null) = underlying.apply(x)

  private object Extractor:
    private val Name: PartialFunction[Term, String] =
      case Select(_, name) => name
      case Ident(name) => name
      case Literal(StringConstant(name)) => name
      case TypeApply(
            Select(Apply(Extractor.Name(Names.SelectDynamic), List(Extractor.Name(name))), Names.AsInstanceOf),
            List(_),
          ) =>
        name

    private val Unpack: PartialFunction[Tree, (qualifier: Term, name: String, extractor: String | Null)] =
      case Select(q @ Extractor.Name(name), extractor) => (q, name, extractor)
      case Apply(q @ Extractor.Name(extractor), List(Extractor.Name(name))) => (q, name, extractor)
      case q @ Extractor.Name(name) => (q, name, null)

    private val Bind: PartialFunction[Tree, Option[Bind]] =
      case bind: Bind => Some(bind)
      case Ident("_") => None

    val Symbol: SymbolExtractor =
      case Extractor.Terminal(name, bind, extractor) => (name, bind, extractor)
      case Extractor.NonTerminal(name, bind, extractor) => (name, bind, extractor)

  val extractEBNFAndAction: PartialFunction[
    Tree,
    (
      symbol: parser.Symbol.NonEmpty,
      bind: Option[Bind],
      others: List[(production: Production, action: Expr[Action[Ctx]])],
    ),
  ] =
    case Extractor.NonTerminal(name, bind, null) =>
      Log.trace(show"extracted non-terminal ref: $name")
      (symbol = NonTerminal(name), bind = bind, others = Nil)

    case Extractor.Terminal(name, bind, null) =>
      Log.trace(show"extracted terminal ref: $name")
      (symbol = Terminal(name), bind = bind, others = Nil)

    case Extractor.Symbol(name, bind, Names.Option) =>
      Log.trace(show"extracted optional: $name")
      val fresh = NonTerminal.fresh(name)
      (
        symbol = fresh,
        bind = bind,
        others = List(
          (production = Production.Empty(fresh), action = '{ Log ?=> noneAction }),
          (
            production = Production.NonEmpty(fresh, NEL(NonTerminal(name))),
            action = '{ someAction },
          ),
        ),
      )

    case Extractor.Symbol(name, bind, Names.List) =>
      Log.trace(show"extracted repeated: $name")
      val fresh = NonTerminal.fresh(name)
      (
        symbol = fresh,
        bind = bind,
        others = List(
          (production = Production.Empty(fresh), action = '{ emptyRepeatedAction }),
          (
            production = Production.NonEmpty(fresh, NEL(fresh, NonTerminal(name))),
            action = '{ repeatedAction },
          ),
        ),
      )

private object ParserExtractors:
  private object Names:
    final val SelectDynamic = "selectDynamic"
    final val Unapply = "unapply"
    final val List = "List"
    final val Option = "Option"
    final val AsInstanceOf = "$asInstanceOf$"

  val repeatedAction: Action[ParserCtx] =
    case (_, Seq(currList: List[?], newElem)) => currList.appended(newElem)
    case x => raiseShouldNeverBeCalled[List[?]](x)

  val emptyRepeatedAction: Action[ParserCtx] = (_, _) => Nil

  val someAction: Action[ParserCtx] =
    case (_, Seq(elem)) => Some(elem)
    case x => raiseShouldNeverBeCalled[Option[?]](x)

  val noneAction: Action[ParserCtx] = (_, _) => None
