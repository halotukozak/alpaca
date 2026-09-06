package com.halotukozak.alpaca.plugin.editing

import com.halotukozak.alpaca.plugin.grammar.GrammarService
import com.halotukozak.alpaca.plugin.lexer.AlpacaLanguage
import com.intellij.codeInsight.highlighting.HeavyBraceHighlighter
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile

/**
 * Brace matching for Alpaca-defined languages. Which tokens are brackets, and which open/close,
 * depends on the file's grammar — not known to a plain [com.intellij.lang.PairedBraceMatcher],
 * whose `getPairs()` takes no context. [HeavyBraceHighlighter] instead gets the file and caret
 * offset, so the grammar can be resolved and [AlpacaBraceScanner] run against it.
 *
 * The scan lexes the whole file; it runs in a background read action, and the languages Alpaca
 * targets are small, so that is fine.
 */
class AlpacaBraceHighlighter : HeavyBraceHighlighter() {
    override fun isAvailable(
        psiFile: PsiFile,
        offset: Int,
    ): Boolean = psiFile.language == AlpacaLanguage

    // Widened from the superclass's `protected` so AlpacaBraceHighlighterTest can call it directly
    // instead of going through the platform's background-highlighting machinery.
    public override fun matchBrace(
        psiFile: PsiFile,
        offset: Int,
    ): Pair<TextRange, TextRange>? {
        val virtualFile = psiFile.virtualFile ?: psiFile.originalFile.virtualFile ?: return null
        val resolved = GrammarService.getInstance(psiFile.project).resolveForFile(virtualFile) ?: return null

        // Match against the editor document's text, not psiFile.text: the caller highlights the
        // returned ranges against the document, and while an edit is uncommitted the PSI can still
        // hold the pre-edit (longer) text, which would put our offsets past the document's end.
        val text = psiFile.viewProvider.document?.immutableCharSequence ?: return null
        if (offset > text.length) return null

        val (open, close) =
            AlpacaBraceScanner.matchAt(text, resolved.lexerId, resolved.tokens, offset) ?: return null
        if (open.endOffset > text.length || close.endOffset > text.length) return null
        return Pair.create(open, close)
    }
}
