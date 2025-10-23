package alpaca
package parser

import alpaca.core.{DebugSettings, Empty, WithDefault}
import alpaca.lexer.{DefinedToken, Token}
import alpaca.lexer.context.Lexem
import alpaca.parser.context.AnyGlobalCtx
import alpaca.parser.context.default.EmptyGlobalCtx
import alpaca.parser.Parser.RuleOnly

import java.io.FileWriter
import scala.annotation.{compileTimeOnly, experimental, tailrec}
import scala.util.Using

/**
 * Configuration settings for the parser.
 *
 * @param debug whether to generate debug output
 * @param debugFileName the file name for debug output
 */
final case class ParserSettings(
  debug: Boolean = true,
  debugFileName: String = "parser.dbg",
)

/**
 * Base class for parsers.
 *
 * Users should extend this class and define their grammar rules as `Rule` instances.
 * The parser uses an LR parsing algorithm with automatic parse table generation.
 *
 * Example:
 * {{{
 * object CalcParser extends Parser[CalcContext] {
 *   val Expr: Rule[Int] =
 *     case (Expr(expr1), CalcLexer.PLUS(_), Expr(expr2)) => expr1 + expr2
 *     case (Expr(expr1), CalcLexer.MINUS(_), Expr(expr2)) => expr1 - expr2
 *     case CalcLexer.NUMBER(n) => n.value
 *
 *   val root: Rule[Int] =
 *     case Expr(expr) => expr
 * }
 * }}}
 *
 * @tparam Ctx the global context type, defaults to EmptyGlobalCtx
 */
abstract class Parser[Ctx <: AnyGlobalCtx](using Ctx WithDefault EmptyGlobalCtx)(using empty: Empty[Ctx]) {

  type Rule[+T] = PartialFunction[Tuple | Lexem[?, ?], T]

  extension [T](rule: Rule[T]) {
    @compileTimeOnly(RuleOnly)
    inline def alwaysAfter(rules: (Token[?, ?, ?] | Rule[Any])*): Rule[T] = ???
    @compileTimeOnly(RuleOnly)
    inline def alwaysBefore(rules: (Token[?, ?, ?] | Rule[Any])*): Rule[T] = ???

    @compileTimeOnly(RuleOnly)
    inline def unapply(x: Any): Option[T] = ???

    @compileTimeOnly(RuleOnly)
    inline def List: PartialFunction[Any, List[T]] = ???

    @compileTimeOnly(RuleOnly)
    inline def Option: PartialFunction[Any, Option[T]] = ???
  }

  extension (token: DefinedToken[?, ?, ?]) {
    @compileTimeOnly(RuleOnly)
    inline def unapply(x: Any): Option[token.LexemTpe] = ???
    @compileTimeOnly(RuleOnly)
    inline def List: PartialFunction[Any, Option[List[token.LexemTpe]]] = ???
    @compileTimeOnly(RuleOnly)
    inline def Option: PartialFunction[Any, Option[token.LexemTpe]] = ???
  }

  /**
   * The root rule of the grammar.
   *
   * This is the starting point for parsing.
   */
  def root: Rule[Any]

  /**
   * Parses a list of lexems using the defined grammar.
   *
   * This method builds the parse table at compile time and uses it to
   * parse the input lexems using an LR parsing algorithm.
   *
   * @tparam R the result type
   * @param lexems   the list of lexems to parse
   * @param debugSettings parser settings (optional)
   * @return a tuple of (context, result), where result may be null on parse failure
   */
  inline def parse[R]( // todo: make it not inlined
    lexems: List[Lexem[?, ?]],
  )(using inline debugSettings: DebugSettings[?, ?],
  ): (ctx: Ctx, result: R | Null) = {
    val (parseTable, actionTable) = createTables[Ctx, R, this.type]
    parse[R](parseTable, actionTable, lexems :+ Lexem.EOF)
  }

  private def parse[R](
    parseTable: ParseTable,
    actionTable: ActionTable[Ctx, R],
    lexems: List[Lexem[?, ?]],
  ): (ctx: Ctx, result: R | Null) = {
    type State = (index: Int, node: R | Lexem[?, ?] | Null)
    val ctx = empty()

    @tailrec def loop(lexems: List[Lexem[?, ?]], stack: List[State]): R | Null = {
      val nextSymbol = Terminal(lexems.head.name)
      parseTable(stack.head.index, nextSymbol) match
        case ParseAction.Shift(gotoState) =>
          loop(lexems.tail, (gotoState, lexems.head) :: stack)

        case ParseAction.Reduction(production @ Production(nextSymbol, rhs)) =>
          val newStack = stack.drop(rhs.size)
          val newState = newStack.head

          if nextSymbol == Symbol.Start && newState.index == 0 then stack.head.node.asInstanceOf[R | Null]
          else {
            val ParseAction.Shift(gotoState) = parseTable(newState.index, nextSymbol).runtimeChecked
            val children = stack.take(rhs.size).map(_.node).reverse
            loop(
              lexems,
              (gotoState, actionTable(production)(ctx, children).asInstanceOf[R | Lexem[?, ?] | Null]) :: newStack,
            )
          }
    }

    ctx -> loop(lexems, (0, null) :: Nil)
  }

  /**
   * Provides access to the parser context within rule definitions.
   *
   * This is compile-time only and can only be used inside parser rule definitions.
   */
  @compileTimeOnly(RuleOnly)
  inline protected final def ctx: Ctx = ???
}

object Parser {
  private final val RuleOnly = "Should never be called outside the parser definition"
}
