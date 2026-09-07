package com.halotukozak.alpaca.plugin.editing

import com.halotukozak.alpaca.plugin.lexer.AlpacaLanguage
import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.ui.breadcrumbs.BreadcrumbsProvider

private const val SNIPPET_LIMIT = 24

/**
 * Breadcrumbs (the path bar under the editor toolbar) for Alpaca-defined languages: one crumb per
 * composite node on the caret's ancestor chain, named after its own nonterminal.
 *
 * An LR parse tree is full of unit-production chains (`Expr -> Term -> Factor -> ...`) that add no
 * tokens of their own -- a composite node whose only child is another composite spanning the exact
 * same text range. Those would just repeat the same span at every level, so [acceptElement] skips
 * them; a node that adds even one token of its own (a bracket pair, a keyword) still gets a crumb.
 */
class AlpacaBreadcrumbsProvider : BreadcrumbsProvider {
    override fun getLanguages(): Array<Language> = arrayOf(AlpacaLanguage)

    override fun acceptElement(element: PsiElement): Boolean {
        if (element is PsiFile || element.firstChild == null) return false
        val children = element.children
        return children.size != 1 || children[0].textRange != element.textRange
    }

    override fun getElementInfo(element: PsiElement): String {
        val typeName = element.node.elementType.toString()
        val oneLine = element.text.replace('\n', ' ').trim()
        val snippet = if (oneLine.length > SNIPPET_LIMIT) oneLine.take(SNIPPET_LIMIT) + "…" else oneLine
        return if (snippet.isEmpty()) typeName else "$typeName: $snippet"
    }
}
