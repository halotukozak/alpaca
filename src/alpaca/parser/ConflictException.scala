package alpaca.parser

import alpaca.core.show
import alpaca.core.Showable.*

sealed class ConflictException(message: Shown) extends Exception(message)

final class ShiftReduceConflict(symbol: Symbol, red: Reduction, path: List[Symbol])
  extends ConflictException(
    show"""
          |Shift \"$symbol\" vs Reduce $red
          |In situation like:
          |${path.filter(_ != Symbol.EOF).mkShow("", " ", " ...")}
          |Consider marking production ${red.production} to be alwaysBefore or alwaysAfter "$symbol"
          |""".stripMargin,
  )

final class ReduceReduceConflict(red1: Reduction, red2: Reduction, path: List[Symbol])
  extends ConflictException(
    show"""
          |Reduce $red1 vs Reduce $red2
          |In situation like:
          |${path.filter(_ != Symbol.EOF).mkShow("", " ", " ...")}
          |Consider marking one of the productions to be alwaysBefore or alwaysAfter the other
          |""".stripMargin,
  )
