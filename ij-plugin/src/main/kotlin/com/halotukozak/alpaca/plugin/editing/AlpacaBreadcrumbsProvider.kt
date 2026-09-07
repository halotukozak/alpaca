package com.halotukozak.alpaca.plugin.editing

import com.halotukozak.alpaca.plugin.grammar.matchedAlternativeName
import com.halotukozak.alpaca.plugin.grammar.resolveGrammarForFile
import com.halotukozak.alpaca.plugin.lexer.AlpacaLanguage
import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.ui.breadcrumbs.BreadcrumbsProvider

/**
 * Breadcrumbs (the path bar under the editor toolbar) for Alpaca-defined languages: one crumb per
 * composite node on the caret's ancestor chain.
 *
 * An LR parse tree is full of unit-production chains (`Expr -> Term -> Factor -> ...`) that add no
 * tokens of their own -- a composite node whose only child is another composite spanning the exact
 * same text range. Those would just repeat the same span at every level, so [acceptElement] skips
 * them; a node that adds even one token of its own (a bracket pair, a keyword) still gets a crumb.
 *
 * A crumb is labelled with the *production alternative* that built it (`plus`, `sin`), not its
 * nonterminal: every alternative of a nonterminal reduces to the same element type (see
 * [com.halotukozak.alpaca.plugin.parser.AlpacaLrDriver]), so a chain of crumbs would otherwise
 * often repeat the same nonterminal name at every level (`Expr > Expr > Expr`) -- exactly the kind
 * of unhelpful, hard-to-scan breadcrumb this provider exists to avoid. The nonterminal name is
 * still the fallback when an alternative has no name or none matches uniquely (see
 * [matchedAlternativeName]) -- unlike Structure View, which shows the nonterminal as the primary,
 * always-present category.
 */
class AlpacaBreadcrumbsProvider : BreadcrumbsProvider {
    override fun getLanguages(): Array<Language> = arrayOf(AlpacaLanguage)

    override fun acceptElement(element: PsiElement): Boolean {
        if (element is PsiFile || element.firstChild == null) return false
        val children = element.children
        return children.size != 1 || children[0].textRange != element.textRange
    }

    override fun getElementInfo(element: PsiElement): String {
        val nonterminal = element.node.elementType.toString()
        val virtualFile = element.containingFile?.virtualFile ?: return nonterminal
        val resolved = resolveGrammarForFile(element.project, virtualFile) ?: return nonterminal
        return matchedAlternativeName(element, resolved) ?: nonterminal
    }
}
