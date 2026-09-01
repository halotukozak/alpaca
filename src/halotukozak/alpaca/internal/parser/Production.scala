package halotukozak
package alpaca
package internal
package parser

import halotukozak.alpaca.internal.{DebugSettings, NEL, Showable, ValidName}

import java.util.concurrent.atomic.AtomicInteger
import scala.quoted.ToExprFactory

/**
 * Represents a grammar production rule.
 *
 * A production defines how a non-terminal symbol can be expanded into
 * a sequence of symbols. For example: `E -> E + T` means the non-terminal
 * E can be produced by the sequence `E + T`.
 *
 * @param rhs the right-hand side sequence of symbols
 */
private[alpaca] enum Production(val rhs: NEL[Symbol.NonEmpty] | Symbol.Empty.type) derives ToExprFactory:

  /** The left-hand side non-terminal of the production. */
  val lhs: NonTerminal

  /** An optional name for the production. */
  val name: ValidName | Null

  /**
   * A stable per-instance index assigned once when this production is constructed.
   *
   * Not a constructor parameter, so it doesn't affect case-class equality/hashCode or
   * pattern matching. Used by [[State]]'s item `Ordering` as a cheap, collision-free
   * tie-breaker instead of recomputing `rhs.hashCode` on every comparison (see #507).
   */
  private[parser] val index: Int = Production.nextIndex.getAndIncrement()

  /**
   * Converts this production to an LR(0) item with a given lookahead.
   *
   * @param lookAhead the lookahead terminal (defaults to EOF)
   * @return an Item representing this production with the dot at position 0
   */
  def toItem(lookAhead: Terminal = Symbol.EOF)(using DebugSettings): Item = Item(this, 0, lookAhead)

  case NonEmpty(
    lhs: NonTerminal & Symbol.NonEmpty,
    override val rhs: NEL[Symbol.NonEmpty],
    name: ValidName | Null = null,
  ) extends Production(rhs)

  case Empty(
    lhs: NonTerminal,
    name: ValidName | Null = null,
  ) extends Production(Symbol.Empty)

private[alpaca] object Production:

  private val nextIndex = AtomicInteger(0)

  /** Showable instance for displaying productions in human-readable form. */
  given Showable[Production] =
    case NonEmpty(lhs, rhs, null) => show"$lhs -> ${rhs.mkShow(" ")}"
    case NonEmpty(lhs, rhs, name: String) => show"$lhs -> ${rhs.mkShow(" ")} ($name)"
    case Empty(lhs, null) => show"$lhs -> ${Symbol.Empty}"
    case Empty(lhs, name: String) => show"$lhs -> ${Symbol.Empty} ($name)"
