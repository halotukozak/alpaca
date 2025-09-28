package alpaca.parser

import alpaca.core.{show, Showable}

/** Type-safe representation of parser actions */
enum Action {
  case Shift(stateId: Int)
  case Reduce(production: Production)
  case Accept
  case Error
}

object Action {
  given Showable[Action] = {
    case Shift(stateId) => show"S$stateId"
    case Reduce(production) => production.show
    case Accept => "Accept"
    case Error => "Error"
  }
}