package alpaca.parser

import scala.collection.mutable

/** Type-safe action table that ensures correct relationships between productions and actions */
class ActionTable private(private val table: mutable.Map[(Int, Symbol), Action]) {
  
  /** Get action for given state and symbol */
  def get(state: Int, symbol: Symbol): Option[Action] = table.get((state, symbol))
  
  /** Get action for given state and symbol, throwing if not found */
  def apply(state: Int, symbol: Symbol): Action = 
    table.getOrElse((state, symbol), throw new NoSuchElementException(s"No action for state $state and symbol $symbol"))
  
  /** Add a shift action - ensures only Int state IDs are used for shifts */
  def addShift(fromState: Int, symbol: Symbol, toState: Int): Unit = {
    table += ((fromState, symbol) -> Action.Shift(toState))
  }
  
  /** Add a reduce action - ensures only valid productions are used for reductions */
  def addReduce(state: Int, symbol: Symbol, production: Production): Unit = {
    table += ((state, symbol) -> Action.Reduce(production))
  }
  
  /** Add an accept action */
  def addAccept(state: Int, symbol: Symbol): Unit = {
    table += ((state, symbol) -> Action.Accept)
  }
  
  /** Convert to old format for compatibility */
  def toOldFormat: mutable.Map[(Int, Symbol), Int | Production] = {
    table.view.mapValues(Action.toOldType).to(mutable.Map)
  }
  
  /** Get all entries */
  def entries: Iterator[((Int, Symbol), Action)] = table.iterator
  
  /** Size of the table */
  def size: Int = table.size
}

object ActionTable {
  /** Create a new empty action table */
  def empty: ActionTable = new ActionTable(mutable.Map.empty)
  
  /** Create from old format */
  def fromOldFormat(oldTable: mutable.Map[(Int, Symbol), Int | Production]): ActionTable = {
    val newTable = oldTable.view.mapValues(Action.fromOldType).to(mutable.Map)
    new ActionTable(newTable)
  }
}