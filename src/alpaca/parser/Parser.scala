package alpaca
package parser

import alpaca.core.{Empty, WithDefault}
import alpaca.lexer.context.Lexem
import alpaca.parser.Symbol.Terminal
import alpaca.parser.context.AnyGlobalCtx
import alpaca.parser.context.default.EmptyGlobalCtx

import java.io.FileWriter
import scala.annotation.{compileTimeOnly, experimental, tailrec}
import scala.util.Using

final case class ParserSettings(
  debug: Boolean = true,
  debugFileName: String = "parser.dbg",
)

abstract class Parser[Ctx <: AnyGlobalCtx](using Ctx WithDefault EmptyGlobalCtx)(using empty: Empty[Ctx]) {

  def root: Rule[Any]

  @experimental
  inline def parse[R](
    lexems: List[Lexem[?, ?]],
  )(using settings: ParserSettings = ParserSettings(),
  ): (ctx: Ctx, result: R | Null) = {
    val (parseTable, actionTable) = createTables[Ctx, R, this.type]

    if settings.debug then Using.resource(new FileWriter(settings.debugFileName))(_.write(parseTable.show))
    parse[R](parseTable, actionTable, lexems :+ Lexem.EOF)
  }

  @experimental
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
