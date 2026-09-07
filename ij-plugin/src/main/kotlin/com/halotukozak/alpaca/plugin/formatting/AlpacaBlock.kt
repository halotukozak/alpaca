package com.halotukozak.alpaca.plugin.formatting

import com.intellij.formatting.Block
import com.intellij.formatting.ChildAttributes
import com.intellij.formatting.Indent
import com.intellij.formatting.Spacing
import com.intellij.formatting.SpacingBuilder
import com.intellij.lang.ASTNode
import com.intellij.psi.formatter.common.AbstractBlock

/**
 * One block per non-blank AST node. A pure-whitespace leaf gets no block at all -- its text is
 * regenerated from [getSpacing] like any other gap -- but a comment leaf, despite also being an
 * `ignored` rule like whitespace, keeps its own block so reformatting can't eat it.
 *
 * Spacing only ever touches what [FormattingRoles] classifies (no space just inside a bracket
 * pair or around `,`/`;`/`.`): anywhere else, [getSpacing] returns null and the formatter leaves
 * whatever the user actually typed alone. There is deliberately no generic "one space between any
 * two tokens" fallback -- with no semantics, that would just be a different unjustified opinion
 * (e.g. it made a keyword-shaped call like `atan2(1, 1)` reformat to `atan2 (1, 1)`, and spaced
 * out a tight-packed grammar like Brainfuck's `+++` into `+ + +`).
 *
 * Indentation is inferred purely from [roles]: when a node's own children end with a bracket
 * closer that has a matching opener earlier among the *same* direct children ([bracketSpan]),
 * whatever sits strictly between that pair is indented one level, exactly like a `{ ... }`/
 * `( ... )`/`[ ... ]` block in any conventional language -- regardless of which nonterminal
 * produced it, and regardless of what (if anything) precedes the opener, e.g. a function-call
 * production `Call -> ID "(" Args ")"` indents `Args` the same as a bare `"(" Expr ")"` would. A
 * left-recursive list production (`List -> List "," Expr`) is not itself bracket-delimited, so it
 * adds no indent of its own; only the one bracket pair enclosing the whole list does, so a wrapped
 * list ends up at exactly one indent level deeper than the bracket, not one level per recursive
 * list node.
 */
class AlpacaBlock(
    node: ASTNode,
    private val roles: FormattingRoles,
    private val spacingBuilder: SpacingBuilder,
    private val indent: Indent,
) : AbstractBlock(node, null, null) {
    override fun getIndent(): Indent = indent

    override fun isLeaf(): Boolean = node.firstChildNode == null

    override fun buildChildren(): List<Block> {
        val children = nonBlankChildren()
        val span = bracketSpan(children)
        return children.mapIndexed { index, child ->
            val interior = span != null && index > span.open && index < span.close
            AlpacaBlock(child, roles, spacingBuilder, if (interior) Indent.getNormalIndent() else Indent.getNoneIndent())
        }
    }

    override fun getSpacing(
        child1: Block?,
        child2: Block,
    ): Spacing? = spacingBuilder.getSpacing(this, child1, child2)

    override fun getChildAttributes(newChildIndex: Int): ChildAttributes {
        val span = bracketSpan(nonBlankChildren())
        val interior = span != null && newChildIndex > span.open && newChildIndex <= span.close
        return ChildAttributes(if (interior) Indent.getNormalIndent() else Indent.getNoneIndent(), null)
    }

    private fun nonBlankChildren(): List<ASTNode> = node.getChildren(null).filter { it.text.isNotBlank() }

    /** The last child's index, if it is a bracket closer with a matching opener earlier in
     *  [children] -- the opener's own index and the closer's (always [children]'s last index). */
    private fun bracketSpan(children: List<ASTNode>): BracketSpan? {
        val close = children.lastIndex
        if (close < 0) return null
        val closerRole = roles.roleOf(children[close].elementType)?.takeIf { !it.opening } ?: return null
        val open =
            children.indexOfFirst { child ->
                roles.roleOf(child.elementType)?.let { it.opening && it.kind == closerRole.kind } == true
            }
        return if (open in 0 until close) BracketSpan(open, close) else null
    }

    private data class BracketSpan(
        val open: Int,
        val close: Int,
    )
}
