package alpaca
package internal
package parser

import alpaca.internal.*
import alpaca.internal.lexer.{DefinedToken, Lexem, Token}
import alpaca.internal.parser.*

import scala.annotation.{compileTimeOnly, tailrec, StaticAnnotation}

/**
 * Base class for parsers.
 *
 * Users should extend this class and define their grammar rules as `Rule` instances.
 * The parser uses an LR parsing algorithm with automatic parse table generation.
 *
 * @tparam Ctx the global context type, defaults to EmptyGlobalCtx
 */
abstract class Parser[Ctx <: ParserCtx](
  using Ctx withDefault ParserCtx.Empty,
)(using
  empty: Empty[Ctx],
  tables: Tables[Ctx],
) {
  extension (token: DefinedToken[?, ?, ?]) {
    @compileTimeOnly(RuleOnly)
    inline def unapply(x: Any): Option[token.LexemTpe] = dummy
    @compileTimeOnly(RuleOnly)
    inline def List: PartialFunction[Any, Option[List[token.LexemTpe]]] = dummy
    @compileTimeOnly(RuleOnly)
    inline def Option: PartialFunction[Any, Option[token.LexemTpe]] = dummy
  }

  /**
   * The root rule of the grammar.
   *
   * This is the starting point for parsing.
   */
  def root: Rule[?]

  def resolutions: Set[ConflictResolution] = Set.empty

  /**
   * Parses a list of lexems using the defined grammar.
   *
   * This method builds the parse table at compile time and uses it to
   * parse the input lexems using an LR parsing algorithm.
   *
   * If the context extends `ParserErrorRecovery`, syntax errors are collected
   * and the parser attempts to continue by skipping unexpected tokens.
   * Otherwise, the parser returns null on parse failure.
   *
   * @tparam R the result type
   * @param lexems   the list of lexems to parse
   * @param debugSettings parser settings (optional)
   * @return a tuple of (context, result), where result may be null on parse failure
   */
  def parse[R](lexems: List[Lexem[?, ?]])(using debugSettings: DebugSettings[?, ?]): (ctx: Ctx, result: R | Null) = {
    type State = (index: Int, node: R | Lexem[?, ?] | Null)
    val ctx = empty()

    def loop(lexems: List[Lexem[?, ?]], stack: List[State]): R | Null = {
      @tailrec def innerLoop(lexems: List[Lexem[?, ?]], stack: List[State]): R | Null = {
        if lexems.isEmpty then return null // Safety check

        val currentLexem = lexems.head
        val nextSymbol = Terminal(currentLexem.name)
        val currentState = stack.head.index

        tables.parseTable.get(currentState, nextSymbol) match
          case Some(action) =>
            action.runtimeChecked match
              case ParseAction.Shift(gotoState) =>
                innerLoop(lexems.tail, (gotoState, currentLexem) :: stack)

              case ParseAction.Reduction(prod @ Production.NonEmpty(lhs, rhs, name)) =>
                val newStack = stack.drop(rhs.size)
                val newState = newStack.head

                if lhs == Symbol.Start && newState.index == 0 then stack.head.node.asInstanceOf[R | Null]
                else {
                  val ParseAction.Shift(gotoState) = tables.parseTable(newState.index, lhs).runtimeChecked
                  val children = stack.take(rhs.size).map(_.node).reverse
                  innerLoop(
                    lexems,
                    (
                      gotoState,
                      tables.actionTable(prod)(ctx, children).asInstanceOf[R | Lexem[?, ?] | Null],
                    ) :: newStack,
                  )
                }

              case ParseAction.Reduction(Production.Empty(Symbol.Start, name)) if stack.head.index == 0 =>
                stack.head.node.asInstanceOf[R | Null]

              case ParseAction.Reduction(prod @ Production.Empty(lhs, name)) =>
                val ParseAction.Shift(gotoState) = tables.parseTable(stack.head.index, lhs).runtimeChecked
                innerLoop(
                  lexems,
                  (gotoState, tables.actionTable(prod)(ctx, Nil).asInstanceOf[R | Lexem[?, ?] | Null]) :: stack,
                )

          case None =>
            // No valid action found - handle error recovery or return null
            ctx match
              case errCtx: ParserErrorRecovery =>
                // Record the error
                val expected = tables.parseTable.expectedTerminals(currentState)
                errCtx.parserErrors += SyntaxError.UnexpectedToken(currentLexem, expected)

                // Panic mode error recovery: skip tokens until we find one we can use
                val remainingLexems = lexems.tail
                if remainingLexems.isEmpty then null
                else innerLoop(remainingLexems, stack)

              case _ =>
                // No error recovery - return null to indicate parse failure
                null
      }

      innerLoop(lexems, stack)
    }

    ctx -> loop(lexems :+ Lexem.EOF, (0, null) :: Nil)
  }

  /**
   * Provides access to the parser context within rule definitions.
   *
   * This is compile-time only and can only be used inside parser rule definitions.
   */
  @compileTimeOnly(RuleOnly)
  inline protected final def ctx: Ctx = dummy
}
