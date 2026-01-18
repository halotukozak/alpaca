package alpaca
package internal
package parser

import alpaca.internal.parser.ParseAction.Reduction

import scala.annotation.constructorOnly

/**
 * Base class for parser conflict exceptions.
 *
 * Parser conflicts occur when the grammar is ambiguous and the parser
 * cannot decide which action to take in a given state.
 *
 * @param message the error message
 */
sealed class ConflictException(message: Shown) extends AlpacaException(message)

/**
 * Exception thrown when there is a shift/reduce conflict.
 *
 * This occurs when the parser cannot decide whether to shift a symbol
 * or reduce by a production. It often indicates an ambiguous grammar.
 */
final class ShiftReduceConflict(symbol: Symbol, red: Reduction, path: List[Symbol])(using @constructorOnly log: Log)
  extends ConflictException(
    show"""
          |Shift \"$symbol\" vs Reduce $red
          |In situation like:
          |${path.filter(_ != Symbol.EOF).mkShow("", " ", " ...")}
          |Consider marking production $red to be alwaysBefore or alwaysAfter "$symbol"
          |""".stripMargin,
  )

/**
 * Exception thrown when there is a reduce/reduce conflict.
 *
 * This occurs when the parser cannot decide which of two productions
 * to reduce by. This always indicates an ambiguous grammar.
 */
final class ReduceReduceConflict(red1: Reduction, red2: Reduction, path: List[Symbol])(using @constructorOnly log: Log)
  extends ConflictException(
    show"""
          |Reduce $red1 vs Reduce $red2
          |In situation like:
          |${path.filter(_ != Symbol.EOF).mkShow("", " ", " ...")}
          |Consider marking one of the productions to be alwaysBefore or alwaysAfter the other
          |""".stripMargin,
  )

/**
 * Exception thrown when before/after rules introduce a cycle.
 *
 * This arises when conflict-resolution metadata marks elements as
 * both preceding and following the same node, so the ordering
 * constraints cannot be satisfied.
 */
final class InconsistentConflictResolution(node: ConflictKey, path: List[ConflictKey])(using @constructorOnly log: Log)
  extends ConflictException(
    show"""
          |Inconsistent conflict resolution detected:
          |${path.dropWhile(_ != node).mkShow(" before ")} before $node
          |There are elements being both before and after $node at the same time.
          |Consider revising the before/after rules to eliminate cycles
          |""".stripMargin,
  )
