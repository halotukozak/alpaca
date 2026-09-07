package com.halotukozak.alpaca.plugin.documentation

import com.halotukozak.alpaca.plugin.grammar.ProductionSpec
import com.halotukozak.alpaca.plugin.grammar.ResolvedGrammar
import com.halotukozak.alpaca.plugin.grammar.SymbolSpec
import com.halotukozak.alpaca.plugin.grammar.TokenSpec
import com.halotukozak.alpaca.plugin.grammar.literalTextOf
import com.halotukozak.alpaca.plugin.grammar.resolveGrammarForFile
import com.halotukozak.alpaca.plugin.lexer.AlpacaLanguage
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

/**
 * Quick Documentation (Ctrl+Q / mouse hover) for Alpaca-defined languages, built entirely from the
 * exported grammar data -- no grammar-specific code.
 *
 * A leaf token shows the lexer rule that matched it (its name and regex pattern, plus whether it's
 * `ignored`). A composite node shows its nonterminal's name and every production alternative
 * exported for it -- the same shape the LR table itself was built from -- so hovering `Expr` in a
 * math grammar lists every way an `Expr` can be built (`plus`, `sin(...)`, a bare number, ...),
 * regardless of which alternative actually produced the node under the caret.
 */
class AlpacaDocumentationProvider : AbstractDocumentationProvider() {
    // Alpaca-defined languages have no PsiReference and no PsiNamedElement, so the platform's
    // default target-resolution (TargetElementUtil, built for "the reference's target" /
    // "the named declaration") finds nothing to hand to generateDoc at all -- Quick Documentation
    // would silently do nothing on hover/Ctrl+Q. This is exactly the override point the platform
    // documents for that case: "a keyword where there's no PsiReference, but for which users might
    // benefit from context help." The leaf already under the caret/mouse is a perfectly good target.
    override fun getCustomDocumentationElement(
        editor: Editor,
        file: PsiFile,
        contextElement: PsiElement?,
        targetOffset: Int,
    ): PsiElement? {
        if (file.language != AlpacaLanguage) return null
        return contextElement
    }

    override fun generateDoc(
        element: PsiElement,
        originalElement: PsiElement?,
    ): String? {
        if (element.language != AlpacaLanguage) return null
        val virtualFile = element.containingFile?.virtualFile ?: return null
        val resolved = resolveGrammarForFile(element.project, virtualFile) ?: return null
        return if (element.firstChild == null) leafDoc(element, resolved) else compositeDoc(element, resolved)
    }

    private fun leafDoc(
        element: PsiElement,
        resolved: ResolvedGrammar,
    ): String? {
        val name = element.node.elementType.toString()
        val spec = resolved.tokens.firstOrNull { it.name == name } ?: return null
        return buildString {
            append(DocumentationMarkup.DEFINITION_START)
            append("token ").append(StringUtil.escapeXmlEntities(name))
            append(DocumentationMarkup.DEFINITION_END)
            append(DocumentationMarkup.CONTENT_START)
            append("Pattern: <code>").append(StringUtil.escapeXmlEntities(spec.pattern)).append("</code>")
            if (spec.ignored) append("<br>Ignored by the parser (whitespace/comment).")
            append(DocumentationMarkup.CONTENT_END)
        }
    }

    private fun compositeDoc(
        element: PsiElement,
        resolved: ResolvedGrammar,
    ): String? {
        val name = element.node.elementType.toString()
        val productions = resolved.parserGrammar?.productions?.filter { it.lhs == name } ?: return null
        if (productions.isEmpty()) return null
        return buildString {
            append(DocumentationMarkup.DEFINITION_START)
            append(StringUtil.escapeXmlEntities(name))
            append(DocumentationMarkup.DEFINITION_END)
            append(DocumentationMarkup.CONTENT_START)
            append("<code>")
            for (production in productions) append(productionLine(production, resolved.tokens))
            append("</code>")
            append(DocumentationMarkup.CONTENT_END)
        }
    }

    private fun productionLine(
        production: ProductionSpec,
        tokens: List<TokenSpec>,
    ): String {
        val rhs =
            if (production.rhs.isEmpty()) {
                "ε"
            } else {
                production.rhs.joinToString(" ") { StringUtil.escapeXmlEntities(symbolLabel(it, tokens)) }
            }
        val label = production.name?.let { " <i>(${StringUtil.escapeXmlEntities(it)})</i>" }.orEmpty()
        return "${StringUtil.escapeXmlEntities(production.lhs)} &rarr; $rhs$label<br>"
    }

    private fun symbolLabel(
        symbol: SymbolSpec,
        tokens: List<TokenSpec>,
    ): String {
        if (symbol.kind != "terminal") return symbol.name
        val pattern = tokens.firstOrNull { it.name == symbol.name }?.pattern
        val literal = pattern?.let(::literalTextOf)
        return if (literal != null) "'$literal'" else symbol.name
    }
}
