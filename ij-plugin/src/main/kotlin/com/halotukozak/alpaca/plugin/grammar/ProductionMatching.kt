package com.halotukozak.alpaca.plugin.grammar

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiWhiteSpace

/**
 * The production alternative that built [element], if its own child-symbol sequence uniquely
 * matches exactly one *named* alternative of its nonterminal in [resolved]'s exported grammar;
 * `null` if there's no parser grammar, no alternative is named, or none matches uniquely.
 *
 * Every alternative of a nonterminal reduces to the same PSI element type ([AlpacaLrDriver][
 * com.halotukozak.alpaca.plugin.parser.AlpacaLrDriver] names composites after the production's
 * LHS, not its per-alternative label), so which one actually matched a given node is otherwise
 * nowhere on the node itself -- only in `<parser>.productions.json`. This recovers it by comparing
 * the node's own child symbols (its composite children's element types and its leaf children's
 * token names, ignored/whitespace/error children skipped) against each same-LHS production's `rhs`.
 */
fun matchedAlternativeName(
    element: PsiElement,
    resolved: ResolvedGrammar,
): String? {
    val grammar = resolved.parserGrammar ?: return null
    val lhs = element.node.elementType.toString()
    val candidates = grammar.productions.filter { it.lhs == lhs && it.name != null }
    if (candidates.isEmpty()) return null

    val ignoredTokenNames =
        resolved.tokens
            .asSequence()
            .filter { it.ignored }
            .map { it.name }
            .toSet()
    val childSymbols =
        element.node
            .getChildren(null)
            .map { it.psi }
            .filterNot { it is PsiWhiteSpace || it is PsiComment || it is PsiErrorElement }
            .map { it.node.elementType.toString() }
            .filterNot { it in ignoredTokenNames }

    return candidates.singleOrNull { it.rhs.map(SymbolSpec::name) == childSymbols }?.name
}
