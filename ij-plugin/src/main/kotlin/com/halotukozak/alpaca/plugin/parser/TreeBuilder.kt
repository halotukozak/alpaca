package com.halotukozak.alpaca.plugin.parser

/** The end-of-input terminal name Alpaca's parser tables use (matches `Symbol.EOF` on the Scala side). */
const val EOF_TERMINAL_NAME = "$"

/**
 * The minimal tree-building surface [AlpacaLrDriver] needs. Implemented by a real
 * [com.intellij.lang.PsiBuilder] adapter in production, and by an in-memory fake in tests that
 * don't need a full IntelliJ platform test fixture.
 *
 * [M] mirrors `PsiBuilder.Marker`: an opaque handle to a not-yet-resolved position. A marker
 * resolves to a composite node via [done], or vanishes -- its would-be children rejoin whatever
 * ends up enclosing them -- via [drop]. [precede] retroactively wraps everything from a marker's
 * own start up to the current position in a *new*, independent marker: the technique bottom-up
 * (shift-reduce) parsing needs to build a properly nested tree, since a reduction only recognizes
 * "this span should have been one node" after the span has already been consumed.
 */
interface TreeBuilder<M> {
  /** The current lookahead token's terminal name (as it appears in the exported grammar), or [EOF_TERMINAL_NAME]. */
  fun currentTerminal(): String

  /** The current lookahead token's raw source text (for error messages -- [currentTerminal] is often
   *  a token's *pattern*, e.g. `\)`, not something meant for a user to read), or `"<eof>"` at end of input. */
  fun currentTokenText(): String

  /** Consumes the current token as a leaf and advances to the next one. */
  fun advance()

  /** Marks the current position, to later be resolved via [done]/[drop] or superseded via [precede]. */
  fun mark(): M

  /** Resolves [marker] as a composite node named [name], spanning from its start to the current position. */
  fun done(marker: M, name: String)

  /** Resolves [marker] without producing a node of its own; anything it would have contained rejoins its parent. */
  fun drop(marker: M)

  /** Returns a new marker starting at [marker]'s own start and extending to the current position. */
  fun precede(marker: M): M

  /** Reports a parse error at the current position, without consuming a token. */
  fun error(message: String)
}
