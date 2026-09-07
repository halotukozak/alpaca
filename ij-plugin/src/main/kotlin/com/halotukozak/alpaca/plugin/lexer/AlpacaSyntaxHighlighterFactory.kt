package com.halotukozak.alpaca.plugin.lexer

import com.halotukozak.alpaca.plugin.grammar.GrammarService
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * Resolves which exported grammar applies to a given file, by its extension and the mappings in
 * Settings, and builds a highlighter for it. Registered once for the shared [AlpacaLanguage]; the
 * grammar lookup goes through the cached [GrammarService].
 */
class AlpacaSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(
        project: Project?,
        virtualFile: VirtualFile?,
    ): SyntaxHighlighter {
        val resolved = if (project != null && virtualFile != null) GrammarService.getInstance(project).resolveForFile(virtualFile) else null
        return if (resolved != null) {
            AlpacaSyntaxHighlighter(resolved.lexerId, resolved.tokens)
        } else {
            AlpacaSyntaxHighlighter("unresolved", emptyList())
        }
    }
}
