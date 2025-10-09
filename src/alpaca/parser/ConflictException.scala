package alpaca.parser

import alpaca.core.show

sealed class ConflictException(message: String) extends Exception(message)

final class ShiftReduceConflict(symbol: Symbol, production: Production, path: String) extends ConflictException(show"""
  Shift \"${symbol.name}\" vs Reduce ${production.rhs} -> ${production.lhs}
  In situation like:
  $path
  Consider marking production ${production.show} to be alwaysBefore or alwaysAfter "${symbol.name}"
""")

final class ReduceReduceConflict(production1: Production, production2: Production, path: String) extends ConflictException(show"""
  Reduce ${production1.rhs} -> ${production1.lhs} vs Reduce ${production2.rhs} -> ${production2.lhs}
  In situation like:
  $path
  Consider marking one of the productions to be alwaysBefore or alwaysAfter the other
""")
