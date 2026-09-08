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
 *
 * [expandable] marks a nonterminal reference inside a production's right-hand side: [children] is
 * empty even though the node genuinely has some, because computing them means recursing into that
 * nonterminal's own alternatives, and a grammar is routinely self-referential (`Expr -> Expr '+'
 * Expr`) -- eagerly unrolling every reachable nonterminal would never terminate. A UI is expected
 * to call [alternativesOf] itself, lazily, the moment such a node is actually expanded; that keeps
 * the tree only ever as deep as a user actually clicked, never deeper.
 *
 * [sourceFile]/[sourceLine] locate the lexer/parser rule this row was defined by, letting a UI
 * offer "go to source"; null for a row with no source of its own -- a category heading (the
 * "Tokens"/"Productions" groups, a nonterminal's own heading) or a production synthesized from
 * EBNF sugar (`List`/`Option`/`SeparatedBy`) rather than written directly in the grammar.
 */
data class GrammarTreeNode(
    val primaryText: String,
    val secondaryText: String? = null,
    val bold: Boolean = false,
    val expandable: Boolean = false,
    val children: List<GrammarTreeNode> = emptyList(),
    val sourceFile: String? = null,
    val sourceLine: Int? = null,
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
                        children = alternatives.map { alternativeRow(it, resolved.tokens) },
                    )
                }
        children += GrammarTreeNode("Productions (${nonterminals.size})", bold = true, children = nonterminals)
    }

    return GrammarTreeNode(resolved.lexerId, bold = true, children = children)
}

/**
 * [nonterminal]'s own alternatives, as rows -- the same shape [buildGrammarTree] already builds
 * for every nonterminal up front, computed for just this one on demand. This is what a UI calls
 * when the user expands a nonterminal-reference node it couldn't afford to expand eagerly.
 */
fun alternativesOf(
    nonterminal: String,
    resolved: ResolvedGrammar,
): List<GrammarTreeNode> {
    val grammar = resolved.parserGrammar ?: return emptyList()
    return grammar.productions.filter { it.lhs == nonterminal }.map { alternativeRow(it, resolved.tokens) }
}

private fun tokenNode(token: TokenSpec): GrammarTreeNode {
    val secondary = if (token.ignored) "${token.pattern}  [ignored]" else token.pattern
    return GrammarTreeNode(token.name, secondary, sourceFile = token.source?.file, sourceLine = token.source?.line)
}

private fun alternativeRow(
    production: ProductionSpec,
    tokens: List<TokenSpec>,
): GrammarTreeNode {
    val rhsText = if (production.rhs.isEmpty()) "ε" else production.rhs.joinToString(" ") { symbolLabel(it, tokens) }
    val nonterminalRefs =
        production.rhs
            .filter { it.kind != "terminal" }
            .map { GrammarTreeNode(it.name, bold = true, expandable = true) }
    return GrammarTreeNode(
        production.name ?: "(unnamed)",
        rhsText,
        children = nonterminalRefs,
        sourceFile = production.source?.file,
        sourceLine = production.source?.line,
    )
}
