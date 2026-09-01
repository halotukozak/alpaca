package com.halotukozak.alpaca.plugin.lexer

import com.intellij.lang.Language
import com.intellij.psi.tree.IElementType

/** Token type for a position no grammar's rules matched at. */
val ALPACA_BAD_CHARACTER: IElementType = IElementType("ALPACA_BAD_CHARACTER", Language.ANY)

/**
 * Caches one [IElementType] per (grammar id, rule name) pair, so the same rule always maps to the
 * same instance. Keyed by grammar id, not just rule name: [AlpacaLanguage] is one shared
 * singleton, so two different grammars can both have a rule named e.g. `NUM` without colliding.
 */
object AlpacaTokenTypes {
  private val cache = HashMap<Pair<String, String>, IElementType>()

  @Synchronized
  fun forName(grammarId: String, name: String): IElementType = cache.getOrPut(grammarId to name) { IElementType(name, AlpacaLanguage) }
}
