package alpaca
package parser

import alpaca.core.{DebugSettings, Empty, WithDefault}
import alpaca.lexer.{DefinedToken, Token}
import alpaca.lexer.context.Lexem
import alpaca.parser.context.AnyGlobalCtx
import alpaca.parser.context.default.EmptyGlobalCtx
import alpaca.parser.Parser.RuleOnly

import scala.annotation.{compileTimeOnly, tailrec}

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

  def root: Rule[Any]

  // todo: make it not inlined
  inline def parse[R](
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

        case ParseAction.Reduction(NonEmptyProduction(lhs, rhs)) =>
          val newStack = stack.drop(rhs.size)
          val newState = newStack.head

          if lhs == Symbol.Start && newState.index == 0 then stack.head.node.asInstanceOf[R | Null]
          else {
            val ParseAction.Shift(gotoState) = parseTable(newState.index, lhs).runtimeChecked
            val children = stack.take(rhs.size).map(_.node).reverse
            loop(
              lexems,
              (
                gotoState,
                actionTable(NonEmptyProduction(lhs, rhs))(ctx, children).asInstanceOf[R | Lexem[?, ?] | Null],
              ) :: newStack,
            )
          }

        case ParseAction.Reduction(EmptyProduction(Symbol.Start)) if stack.head.index == 0 =>
          stack.head.node.asInstanceOf[R | Null]

        case ParseAction.Reduction(EmptyProduction(lhs)) =>
          val ParseAction.Shift(gotoState) = parseTable(stack.head.index, lhs).runtimeChecked
          loop(
            lexems,
            (gotoState, actionTable(EmptyProduction(lhs))(ctx, Nil).asInstanceOf[R | Lexem[?, ?] | Null]) :: stack,
          )
    }
    ctx -> loop(lexems, (0, null) :: Nil)
  }

  @compileTimeOnly(RuleOnly)
  inline protected final def ctx: Ctx = ???
}

object Parser {
  private final val RuleOnly = "Should never be called outside the parser definition"
}
