package alpaca
package parser

import alpaca.core.{Empty, WithDefault}
import alpaca.lexer.DefinedToken
import alpaca.lexer.context.Lexem
import alpaca.parser.Symbol.Terminal
import alpaca.parser.context.AnyGlobalCtx
import alpaca.parser.context.default.EmptyGlobalCtx

import scala.annotation.{compileTimeOnly, tailrec}

final case class ParserSettings(
  debug: Boolean = true,
  debugFileName: String = "parser.dbg",
)

abstract class Parser[Ctx <: AnyGlobalCtx](using Ctx WithDefault EmptyGlobalCtx)(using empty: Empty[Ctx]) {

  type Rule[+T] = PartialFunction[Tuple | Lexem[?, ?], T]

  extension [T](rule: Rule[T]) {
    inline def alwaysAfter(rules: Rule[Any]*): Rule[T] = ???
    inline def alwaysBefore(rules: Rule[Any]*): Rule[T] = ???

    @compileTimeOnly("Should never be called outside the parser definition")
    inline def unapply(x: Any): Option[T] = ???

    @compileTimeOnly("Should never be called outside the parser definition")
    inline def List: PartialFunction[Any, Option[List[T]]] = ???

    @compileTimeOnly("Should never be called outside the parser definition")
    inline def Option: PartialFunction[Any, Option[T]] = ???
  }

  extension (token: DefinedToken[?, ?, ?]) {
    @compileTimeOnly("Should never be called outside the parser definition")
    inline def unapply(x: Any): Option[token.LexemTpe] = ???
    @compileTimeOnly("Should never be called outside the parser definition")
    inline def List: PartialFunction[Any, Option[List[token.LexemTpe]]] = ???
    @compileTimeOnly("Should never be called outside the parser definition")
    inline def Option: PartialFunction[Any, Option[token.LexemTpe]] = ???
  }

  def root: Rule[Any]
  inline def parse[R](
    lexems: List[Lexem[?, ?]],
  )(using settings: ParserSettings = ParserSettings(),
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

  @compileTimeOnly("Should never be called outside the parser definition")
  inline protected final def ctx: Ctx = ???
}
