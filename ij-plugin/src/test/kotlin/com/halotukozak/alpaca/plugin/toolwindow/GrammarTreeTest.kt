package com.halotukozak.alpaca.plugin.toolwindow

import com.halotukozak.alpaca.plugin.grammar.ParserGrammar
import com.halotukozak.alpaca.plugin.grammar.ProductionSpec
import com.halotukozak.alpaca.plugin.grammar.ResolvedGrammar
import com.halotukozak.alpaca.plugin.grammar.SymbolSpec
import com.halotukozak.alpaca.plugin.grammar.TokenSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** [buildGrammarTree]/[alternativesOf] are plain data in, plain data out -- no PSI, no platform
 *  test fixture needed. */
class GrammarTreeTest {
    private val plusToken = TokenSpec(name = "\\+", pattern = "\\+", ignored = false)
    private val intToken = TokenSpec(name = "int", pattern = "\\d+", ignored = false)
    private val whitespaceToken = TokenSpec(name = "ws", pattern = "[ \\t]+", ignored = true)

    /** `Expr -> Expr '+' Expr` (named `plus`) and `Expr -> int` (unnamed): the shape MathParser's
     *  own grammar has, minimally reproduced so a test doesn't depend on the real export fixture. */
    private val plusProduction =
        ProductionSpec(
            "Expr",
            listOf(SymbolSpec("nonterminal", "Expr"), SymbolSpec("terminal", "\\+"), SymbolSpec("nonterminal", "Expr")),
            "plus",
        )
    private val literalProduction = ProductionSpec("Expr", listOf(SymbolSpec("terminal", "int")), name = null)

    @Test
    fun `lists every token, marking ignored ones`() {
        val resolved = ResolvedGrammar("Lexer", listOf(intToken, whitespaceToken), parserGrammar = null)

        val tokens = buildGrammarTree(resolved).children.single()

        assertEquals("Tokens (2)" to true, tokens.primaryText to tokens.bold)
        assertEquals(
            listOf("int" to "\\d+", "ws" to "[ \\t]+  [ignored]"),
            tokens.children.map { it.primaryText to it.secondaryText },
        )
    }

    @Test
    fun `omits the productions branch entirely when there is no parser grammar`() {
        val resolved = ResolvedGrammar("Lexer", listOf(intToken), parserGrammar = null)

        assertEquals(listOf("Tokens (1)"), buildGrammarTree(resolved).children.map { it.primaryText })
    }

    @Test
    fun `groups productions by nonterminal and labels each alternative`() {
        val grammar = ParserGrammar("Parser", listOf(plusProduction, literalProduction))
        val resolved = ResolvedGrammar("Lexer", listOf(plusToken, intToken), grammar)

        val productions = buildGrammarTree(resolved).children[1]

        assertEquals("Productions (1)", productions.primaryText)
        val expr = productions.children.single()
        assertEquals("Expr" to true, expr.primaryText to expr.bold)
        assertEquals(
            listOf("plus" to "Expr '+' Expr", "(unnamed)" to "int"),
            expr.children.map { it.primaryText to it.secondaryText },
        )
    }

    @Test
    fun `a named alternative's nonterminal references are expandable, its terminals are not`() {
        val grammar = ParserGrammar("Parser", listOf(plusProduction, literalProduction))
        val resolved = ResolvedGrammar("Lexer", listOf(plusToken, intToken), grammar)

        val alternatives =
            buildGrammarTree(resolved)
                .children[1]
                .children
                .single()
                .children
        val plusAlternative = alternatives.first { it.primaryText == "plus" }
        val literalAlternative = alternatives.first { it.primaryText == "(unnamed)" }

        // Expr -> Expr '+' Expr: both Expr occurrences become their own expandable child (not
        // merged into one), the '+' terminal contributes nothing.
        assertEquals(listOf("Expr", "Expr"), plusAlternative.children.map { it.primaryText })
        assertTrue(plusAlternative.children.all { it.expandable })
        assertEquals(emptyList<GrammarTreeNode>(), literalAlternative.children)
    }

    @Test
    fun `alternativesOf computes the same rows for one nonterminal on demand`() {
        val grammar = ParserGrammar("Parser", listOf(plusProduction, literalProduction))
        val resolved = ResolvedGrammar("Lexer", listOf(plusToken, intToken), grammar)

        val eager =
            buildGrammarTree(resolved)
                .children[1]
                .children
                .single()
                .children
        val lazy = alternativesOf("Expr", resolved)

        assertEquals(eager, lazy)
    }

    @Test
    fun `alternativesOf is empty for an unknown nonterminal or no parser grammar`() {
        val withParser = ResolvedGrammar("Lexer", listOf(intToken), ParserGrammar("Parser", listOf(literalProduction)))
        val withoutParser = ResolvedGrammar("Lexer", listOf(intToken), parserGrammar = null)

        assertEquals(emptyList<GrammarTreeNode>(), alternativesOf("NoSuchRule", withParser))
        assertEquals(emptyList<GrammarTreeNode>(), alternativesOf("Expr", withoutParser))
    }

    @Test
    fun `labels an epsilon production`() {
        val epsilon = ProductionSpec("Opt", emptyList(), name = null)
        val grammar = ParserGrammar("Parser", listOf(epsilon))
        val resolved = ResolvedGrammar("Lexer", emptyList(), grammar)

        val alternative =
            buildGrammarTree(resolved)
                .children[1]
                .children
                .single()
                .children
                .single()

        assertEquals("(unnamed)" to "ε", alternative.primaryText to alternative.secondaryText)
    }
}
