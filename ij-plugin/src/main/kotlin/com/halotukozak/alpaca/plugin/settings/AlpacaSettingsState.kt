package com.halotukozak.alpaca.plugin.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/**
 * One extension-to-grammar mapping: files with [extension] are highlighted using the lexer
 * exported as [lexerGrammarId], and (if set) parsed using the parser exported as [parserGrammarId].
 * The ids are separate because Alpaca exports each `lexer{...}`/parser call site under its own
 * name, with nothing on the export side linking a parser back to its lexer.
 */
data class GrammarAssociation
  @JvmOverloads
  constructor(
    var extension: String = "",
    var lexerGrammarId: String = "",
    var parserGrammarId: String = "",
  )

@Service(Service.Level.PROJECT)
@State(name = "AlpacaSettings", storages = [Storage("alpaca.xml", roamingType = RoamingType.DISABLED)])
class AlpacaSettingsState : PersistentStateComponent<AlpacaSettingsState.State> {
  class State {
    var exportDirectory: String = ""
    var associations: MutableList<GrammarAssociation> = mutableListOf()
  }

  private var state = State()

  override fun getState(): State = state

  override fun loadState(state: State) {
    this.state = state
  }

  var exportDirectory: String
    get() = state.exportDirectory
    set(value) {
      state.exportDirectory = value
    }

  var associations: MutableList<GrammarAssociation>
    get() = state.associations
    set(value) {
      state.associations = value
    }

  /** The export directory to scan: the configured one, falling back to `ALPACA_GRAMMAR_EXPORT_DIR`. */
  fun resolvedExportDirectory(): String? =
    exportDirectory.ifBlank { System.getenv("ALPACA_GRAMMAR_EXPORT_DIR").orEmpty() }.ifBlank { null }

  /** The configured mapping for [extension], if any (case-insensitive). */
  fun associationForExtension(extension: String): GrammarAssociation? = associations.firstOrNull { it.extension.equals(extension, ignoreCase = true) }

  companion object {
    fun getInstance(project: Project): AlpacaSettingsState = project.service()
  }
}
