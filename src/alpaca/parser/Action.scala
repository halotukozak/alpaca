package alpaca.parser

import alpaca.core.{show, Showable}

/** Type-safe representation of parser actions */
enum Action {
  case Shift(stateId: Int)
  case Reduce(production: Production)
  case Accept
}

object Action {
  given Showable[Action] = {
    case Shift(stateId) => show"S$stateId"
    case Reduce(production) => production.show
    case Accept => "Accept"
  }
  
  /** Convert from the old Int | Production representation */
  def fromOldType(value: Int | Production): Action = value match {
    case i: Int => Shift(i)
    case p: Production => Reduce(p)
  }
  
  /** Convert to the old Int | Production representation for compatibility */
  def toOldType(action: Action): Int | Production = action match {
    case Shift(stateId) => stateId
    case Reduce(production) => production
    case Accept => throw new IllegalArgumentException("Accept action cannot be converted to old type")
  }
}