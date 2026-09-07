package com.halotukozak.alpaca.plugin.toolwindow

import com.halotukozak.alpaca.plugin.grammar.ParserGrammar
import com.halotukozak.alpaca.plugin.grammar.ProductionSpec
import com.halotukozak.alpaca.plugin.grammar.ResolvedGrammar
import com.halotukozak.alpaca.plugin.grammar.SymbolSpec
import com.halotukozak.alpaca.plugin.grammar.TokenSpec
import org.junit.Assert.assertEquals
import org.junit.Test

/** [buildGrammarTree] is plain data in, plain data out -- no PSI, no platform fixture needed. */
class GrammarTreeTest {
    private val plusToken = TokenSpec(name = "\\+", pattern = "\\+", ignored = false)
    private val intToken = TokenSpec(name = "int", pattern = "\\d+", ignored = false)
    private val whitespaceToken = TokenSpec(name = "ws", pattern = "[ \\t]+", ignored = true)

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
        val plus =
            ProductionSpec(
                "Expr",
                listOf(SymbolSpec("nonterminal", "Expr"), SymbolSpec("terminal", "\\+"), SymbolSpec("nonterminal", "Expr")),
                "plus",
            )
        val literal = ProductionSpec("Expr", listOf(SymbolSpec("terminal", "int")), name = null)
        val grammar = ParserGrammar("Parser", listOf(plus, literal))
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
