package com.halotukozak.alpaca.plugin.lexer

import com.halotukozak.alpaca.plugin.grammar.GrammarDirectory
import com.halotukozak.alpaca.plugin.grammar.TokenSpec
import com.halotukozak.alpaca.plugin.settings.AlpacaSettingsState
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Path

/**
 * Resolves which exported grammar applies to a given file, by its extension and the mappings in
 * Settings, and builds a highlighter for it. Registered once for the shared [AlpacaLanguage].
 */
class AlpacaSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
  override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter {
    val (grammarId, tokens) = resolve(project, virtualFile) ?: ("unresolved" to emptyList())
    return AlpacaSyntaxHighlighter(grammarId, tokens)
  }

  private fun resolve(project: Project?, virtualFile: VirtualFile?): Pair<String, List<TokenSpec>>? {
    if (project == null || virtualFile == null) return null
    val settings = AlpacaSettingsState.getInstance(project)
    val exportDirectory = settings.resolvedExportDirectory() ?: return null
    val extension = virtualFile.extension ?: return null
    val grammarId = settings.associationForExtension(extension)?.lexerGrammarId ?: return null
    val tokens =
      GrammarDirectory
        .scan(Path.of(exportDirectory))
        .lexers
        .firstOrNull { it.id == grammarId }
        ?.tokens
        ?: return null
    return grammarId to tokens
  }
}
