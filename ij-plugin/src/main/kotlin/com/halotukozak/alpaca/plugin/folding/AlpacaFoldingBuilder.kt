package com.halotukozak.alpaca.plugin.folding

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

private const val PLACEHOLDER_TEXT = "..."

/**
 * Folds every composite (nonterminal) PSI node whose own text spans more than one line. Entirely
 * grammar-agnostic: it only looks at where a node starts/ends in the document, never at what kind
 * of node it is.
 */
class AlpacaFoldingBuilder : FoldingBuilderEx() {
    override fun buildFoldRegions(
        root: PsiElement,
        document: Document,
        quick: Boolean,
    ): Array<FoldingDescriptor> {
        val seenRanges = HashSet<TextRange>()
        val descriptors = mutableListOf<FoldingDescriptor>()
        for (child in root.children) collect(child, document, seenRanges, descriptors)
        return descriptors.toTypedArray()
    }

    private fun collect(
        element: PsiElement,
        document: Document,
        seenRanges: MutableSet<TextRange>,
        out: MutableList<FoldingDescriptor>,
    ) {
        val range = element.textRange
        // A unit production (`A -> B` with nothing else) reduces to the exact same range as its only
        // child; only the outermost of such a chain needs a fold region, so skip an exact repeat.
        if (range.endOffset <= document.textLength && spansMultipleLines(range, document) && seenRanges.add(range)) {
            // Passed explicitly rather than relying on the [getPlaceholderText] override below: a
            // descriptor built without it defaults its own placeholder to the folded text itself.
            out.add(FoldingDescriptor(element.node, range, null, PLACEHOLDER_TEXT))
        }
        for (child in element.children) collect(child, document, seenRanges, out)
    }

    private fun spansMultipleLines(
        range: TextRange,
        document: Document,
    ): Boolean = document.getLineNumber(range.startOffset) != document.getLineNumber(range.endOffset)

    override fun getPlaceholderText(node: ASTNode): String = PLACEHOLDER_TEXT

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false
}
