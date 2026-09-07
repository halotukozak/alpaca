package com.halotukozak.alpaca.plugin.toolwindow

import com.halotukozak.alpaca.plugin.grammar.ProductionSpec
import com.halotukozak.alpaca.plugin.grammar.ResolvedGrammar
import com.halotukozak.alpaca.plugin.grammar.TokenSpec
import com.halotukozak.alpaca.plugin.grammar.symbolLabel

/**
 * One row in the grammar tool window, independent of any UI toolkit -- easy to build and test
 * without a Swing component or a platform test fixture. [secondaryText], when present, is meant
 * to be rendered dimmed/secondary (a token's pattern, a production's right-hand side) -- the same
 * role Structure View's location string plays, kept out of [primaryText] so a renderer can tell
 * the two apart instead of parsing one flat string.
 */
data class GrammarTreeNode(
    val primaryText: String,
    val secondaryText: String? = null,
    val bold: Boolean = false,
    val children: List<GrammarTreeNode> = emptyList(),
)

/**
 * The whole of [resolved]'s grammar as a browsable tree: every lexer token (name, pattern, and
 * whether it's `ignored`), then every parser production grouped by its nonterminal, each
 * alternative labelled the same way Quick Documentation labels it (see [symbolLabel]) -- but for
 * the grammar as a whole, not one node at a time. No parser grammar (lexer-only association)
 * omits the productions branch entirely rather than showing it empty.
 */
fun buildGrammarTree(resolved: ResolvedGrammar): GrammarTreeNode {
    val children =
        mutableListOf(
            GrammarTreeNode(
                "Tokens (${resolved.tokens.size})",
                bold = true,
                children = resolved.tokens.map { tokenNode(it) },
            ),
        )

    resolved.parserGrammar?.let { grammar ->
        val nonterminals =
            grammar.productions
                .groupBy { it.lhs }
                .map { (lhs, alternatives) ->
                    GrammarTreeNode(
                        lhs,
                        bold = true,
                        children = alternatives.map { alternativeNode(it, resolved.tokens) },
                    )
                }
        children += GrammarTreeNode("Productions (${nonterminals.size})", bold = true, children = nonterminals)
    }

    return GrammarTreeNode(resolved.lexerId, bold = true, children = children)
}

private fun tokenNode(token: TokenSpec): GrammarTreeNode {
    val secondary = if (token.ignored) "${token.pattern}  [ignored]" else token.pattern
    return GrammarTreeNode(token.name, secondary)
}

private fun alternativeNode(
    production: ProductionSpec,
    tokens: List<TokenSpec>,
): GrammarTreeNode {
    val rhs = if (production.rhs.isEmpty()) "ε" else production.rhs.joinToString(" ") { symbolLabel(it, tokens) }
    return GrammarTreeNode(production.name ?: "(unnamed)", rhs)
}
