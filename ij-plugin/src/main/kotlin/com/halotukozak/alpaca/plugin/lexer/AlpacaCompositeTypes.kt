package com.halotukozak.alpaca.plugin.lexer

import com.intellij.psi.tree.IElementType

/**
 * Caches one composite (non-leaf) [IElementType] per (grammar id, nonterminal name) pair -- the
 * PSI node type a reduced production's nonterminal becomes, mirroring [AlpacaTokenTypes] for leaf
 * tokens. Keyed by grammar id for the same reason: [AlpacaLanguage] is one shared singleton, so two
 * different grammars can both have a nonterminal named e.g. `Expr` without colliding.
 */
object AlpacaCompositeTypes {
  private val cache = HashMap<Pair<String, String>, IElementType>()

  @Synchronized
  fun forName(grammarId: String, name: String): IElementType = cache.getOrPut(grammarId to name) { IElementType(name, AlpacaLanguage) }
}
