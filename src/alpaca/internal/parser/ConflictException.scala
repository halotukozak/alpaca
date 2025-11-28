package alpaca
package internal
package parser

import alpaca.internal.parser.ParseAction.Reduction

/**
 * Base class for parser conflict exceptions.
 *
 * Parser conflicts occur when the grammar is ambiguous and the parser
 * cannot decide which action to take in a given state.
 *
 * @param message the error message
 */
sealed class ConflictException(message: Shown) extends Exception(message)

/**
 * Exception thrown when there is a shift/reduce conflict.
 *
 * This occurs when the parser cannot decide whether to shift a symbol
 * or reduce by a production. It often indicates an ambiguous grammar.
 *
 * @param symbol the symbol to potentially shift
 * @param red the reduction to potentially apply
 * @param path the path of symbols leading to this conflict
 */
final class ShiftReduceConflict(symbol: Symbol, red: Reduction, path: List[Symbol])
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
 *
 * @param red1 the first potential reduction
 * @param red2 the second potential reduction
 * @param path the path of symbols leading to this conflict
 */
final class ReduceReduceConflict(red1: Reduction, red2: Reduction, path: List[Symbol])
  extends ConflictException(
    show"""
          |Reduce $red1 vs Reduce $red2
          |In situation like:
          |${path.filter(_ != Symbol.EOF).mkShow("", " ", " ...")}
          |Consider marking one of the productions to be alwaysBefore or alwaysAfter the other
          |""".stripMargin,
  )

final class InconsistentConflictResolution(node: ConflictKey, path: List[ConflictKey])
  extends ConflictException(
    {
      given Showable[ConflictKey] =
        case Production.NonEmpty(lhs, rhs, null) => show"Reduction(${rhs.mkShow(" ")} -> $lhs)"
        case Production.Empty(lhs, null) => show"Reduction(${Symbol.Empty} -> $lhs)"
        case p: Production => show"Reduction(${p.name})"
        case s: String => show"Shift($s)"

      show"""
            |Inconsistent conflict resolution detected:
            |${path.dropWhile(_ != node).mkShow(" before ")} before $node
            |There are elements being both before and after $node at the same time.
            |Consider revising the before/after rules to eliminate cycles
            |""".stripMargin
    },
  )
