package com.halotukozak.alpaca.plugin.lexer

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

/**
 * One [LanguageFileType] per exported grammar, all wrapping the single
 * shared [AlpacaLanguage] (`Language` itself only allows one instance per
 * class -- see that class's doc comment -- but `FileType` has no such
 * restriction). Registered with the platform dynamically at runtime by
 * [AlpacaFileTypeRegistrar], since the set of grammars is only known from
 * Settings, not compile time. Obtain via [AlpacaFileTypes.forGrammar].
 */
class AlpacaFileType(grammarId: String) : LanguageFileType(AlpacaLanguage) {
  private val name = "Alpaca:$grammarId"
  private val description = "Language defined with Alpaca ($grammarId)"

  override fun getName(): String = name

  override fun getDescription(): String = description

  override fun getDefaultExtension(): String = ""

  override fun getIcon(): Icon? = null
}

/** Caches one [AlpacaFileType] per grammar id. */
object AlpacaFileTypes {
  private val byGrammarId = HashMap<String, AlpacaFileType>()

  @Synchronized
  fun forGrammar(grammarId: String): AlpacaFileType = byGrammarId.getOrPut(grammarId) { AlpacaFileType(grammarId) }
}
