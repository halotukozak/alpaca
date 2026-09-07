package com.halotukozak.alpaca.plugin.hints

import com.halotukozak.alpaca.plugin.grammar.ProductionSpec
import com.halotukozak.alpaca.plugin.grammar.ResolvedGrammar
import com.halotukozak.alpaca.plugin.grammar.SymbolSpec
import com.halotukozak.alpaca.plugin.grammar.resolveGrammarForFile
import com.halotukozak.alpaca.plugin.lexer.AlpacaLanguage
import com.intellij.codeInsight.hints.declarative.HintFormat
import com.intellij.codeInsight.hints.declarative.InlayHintsCollector
import com.intellij.codeInsight.hints.declarative.InlayHintsProvider
import com.intellij.codeInsight.hints.declarative.InlayTreeSink
import com.intellij.codeInsight.hints.declarative.InlineInlayPosition
import com.intellij.codeInsight.hints.declarative.SharedBypassCollector
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace

/**
 * An inline hint at the start of each composite node, naming the *production alternative* that
 * built it -- `plus` on `a + b`, `sin` on `sin(x)`, `atan2` on `atan2(y, x)`.
 *
 * Every alternative of a nonterminal reduces to the same PSI element type (see
 * [com.halotukozak.alpaca.plugin.parser.AlpacaLrDriver], which names composites after the
 * production's LHS, not its per-alternative label), so the alternative that actually matched is
 * nowhere on the node itself -- only in `<parser>.productions.json`. This recovers it by matching
 * the node's own child-symbol sequence back against that grammar's productions. Fully
 * grammar-agnostic: both the label and the match come from the exported data.
 *
 * Only multi-symbol alternatives (`rhs.size >= 2`) are labelled. A single-nonterminal production
 * is an LR unit-production artifact (`Expr -> Term -> Factor`); a single-terminal one
 * (`Expr -> int`) says no more than the token already visible under the hint. Labelling either
 * would just be noise.
 */
class AlpacaParseTreeInlayHintsProvider : InlayHintsProvider {
    override fun createCollector(
        file: PsiFile,
        editor: Editor,
    ): InlayHintsCollector? {
        if (file.language != AlpacaLanguage) return null
        val virtualFile = file.virtualFile ?: return null
        val resolved = resolveGrammarForFile(file.project, virtualFile) ?: return null
        if (resolved.parserGrammar?.productions?.any { it.isLabelledAlternative } != true) return null
        return Collector(resolved)
    }

    private class Collector(
        resolved: ResolvedGrammar,
    ) : SharedBypassCollector {
        private val productions = resolved.parserGrammar!!.productions
        private val ignoredTokenNames =
            resolved.tokens
                .asSequence()
                .filter { it.ignored }
                .map { it.name }
                .toSet()

        override fun collectFromElement(
            element: PsiElement,
            sink: InlayTreeSink,
        ) {
            if (element is PsiFile || element.firstChild == null) return
            val label = alternativeLabelFor(element) ?: return
            sink.addPresentation(
                InlineInlayPosition(element.textRange.startOffset, relatedToPrevious = false),
                hintFormat = HintFormat.default,
            ) { text(label) }
        }

        private fun alternativeLabelFor(element: PsiElement): String? {
            val lhs = element.node.elementType.toString()
            val candidates = productions.filter { it.lhs == lhs && it.isLabelledAlternative }
            if (candidates.isEmpty()) return null

            val childSymbols =
                element.node
                    .getChildren(null)
                    .map { it.psi }
                    .filterNot { it is PsiWhiteSpace || it is PsiComment || it is PsiErrorElement }
                    .map { it.node.elementType.toString() }
                    .filterNot { it in ignoredTokenNames }

            return candidates.singleOrNull { it.rhs.map(SymbolSpec::name) == childSymbols }?.name
        }
    }
}

private val ProductionSpec.isLabelledAlternative: Boolean
    get() = name != null && rhs.size >= 2
