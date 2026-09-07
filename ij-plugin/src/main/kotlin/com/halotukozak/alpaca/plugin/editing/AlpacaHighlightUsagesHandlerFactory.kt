package com.halotukozak.alpaca.plugin.editing

import com.halotukozak.alpaca.plugin.lexer.AlpacaLanguage
import com.intellij.codeInsight.highlighting.HighlightUsagesHandlerBase
import com.intellij.codeInsight.highlighting.HighlightUsagesHandlerFactoryBase
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementType
import com.intellij.util.Consumer

/**
 * Highlights every other occurrence of the identifier under the caret (the same thing the platform
 * does automatically for a caret resting on a name, and on demand via *Edit | Find | Highlight
 * Usages in File*).
 *
 * An Alpaca-defined language has no name resolution -- no declarations, scopes, or references -- so
 * "the same identifier" can only mean *the same token text, of the same token type, elsewhere in
 * this file*. That is purely lexical and needs nothing from the grammar: it works for whatever a
 * grammar happens to call its identifier rule, and stays quiet for grammars that have no
 * word-shaped tokens at all.
 */
class AlpacaHighlightUsagesHandlerFactory : HighlightUsagesHandlerFactoryBase() {
    override fun createHighlightUsagesHandler(
        editor: Editor,
        psiFile: PsiFile,
        target: PsiElement,
    ): HighlightUsagesHandlerBase<PsiElement>? {
        if (psiFile.language != AlpacaLanguage) return null
        // `target` is the leaf at the caret (HighlightUsagesHandlerFactoryBase.findTarget). Only a
        // word-shaped leaf is worth chasing -- highlighting every `(` or every `,` would be noise.
        if (target.firstChild != null || !isWordLike(target.text)) return null
        return AlpacaHighlightUsagesHandler(editor, psiFile, target)
    }

    private fun isWordLike(text: String): Boolean =
        text.isNotEmpty() &&
            Character.isJavaIdentifierStart(text[0]) &&
            text.all { Character.isJavaIdentifierPart(it) } &&
            text.any { it.isLetter() }
}

private class AlpacaHighlightUsagesHandler(
    editor: Editor,
    psiFile: PsiFile,
    private val target: PsiElement,
) : HighlightUsagesHandlerBase<PsiElement>(editor, psiFile) {
    override fun getTargets(): List<PsiElement> = listOf(target)

    override fun selectTargets(
        targets: List<PsiElement>,
        selectionConsumer: Consumer<in List<PsiElement>>,
    ) = selectionConsumer.consume(targets)

    override fun computeUsages(targets: List<PsiElement>) {
        val type = target.elementType
        val text = target.text
        forEachLeaf(myFile) { leaf ->
            if (leaf.elementType == type && leaf.textMatches(text)) addOccurrence(leaf)
        }
        buildStatusText(text, myReadUsages.size)
    }

    private fun forEachLeaf(
        element: PsiElement,
        action: (PsiElement) -> Unit,
    ) {
        var child = element.firstChild
        if (child == null) {
            action(element)
            return
        }
        while (child != null) {
            forEachLeaf(child, action)
            child = child.nextSibling
        }
    }
}
