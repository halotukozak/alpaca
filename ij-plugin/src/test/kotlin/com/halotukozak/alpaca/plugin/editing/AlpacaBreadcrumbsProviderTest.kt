package com.halotukozak.alpaca.plugin.editing

import com.halotukozak.alpaca.plugin.lexer.AlpacaFileTypeRegistrar
import com.halotukozak.alpaca.plugin.settings.AlpacaSettingsState
import com.halotukozak.alpaca.plugin.settings.GrammarAssociation
import com.intellij.openapi.application.runWriteAction
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase

private const val LEXER_ID = "MathTest.CalcLexer@L11"
private const val PARSER_ID = "MathTest.MathParser@L39"

/**
 * MathParser's grammar has exactly two nonterminals, `Expr` and `root`, and every `Expr`
 * alternative reduces to the same `Expr` element type (see [com.halotukozak.alpaca.plugin.parser.AlpacaLrDriver],
 * which names composites after the production's LHS, not its per-alternative label) -- so the
 * crumb chain below is entirely about which nodes [AlpacaBreadcrumbsProvider.acceptElement] keeps
 * or drops, not about telling different rules apart.
 */
class AlpacaBreadcrumbsProviderTest : BasePlatformTestCase() {
    private val provider = AlpacaBreadcrumbsProvider()

    override fun setUp() {
        super.setUp()
        val settings = AlpacaSettingsState.getInstance(project)
        settings.exportDirectory = "/tmp/alpaca-grammar-export"
        settings.associations =
            mutableListOf(GrammarAssociation(extension = "calc", lexerGrammarId = LEXER_ID, parserGrammarId = PARSER_ID))
        runWriteAction { AlpacaFileTypeRegistrar.ensureRegistered("calc", LEXER_ID) }
    }

    /** Accepted crumb labels from the caret marked by `<caret>` in [text], innermost first. */
    private fun crumbs(text: String): List<String> {
        val file = myFixture.configureByText("test.calc", text)
        val crumbs = mutableListOf<String>()
        var element: PsiElement? = file.findElementAt(myFixture.caretOffset)
        while (element != null && element !is PsiFile) {
            if (provider.acceptElement(element)) crumbs += provider.getElementInfo(element)
            element = provider.getParent(element)
        }
        return crumbs
    }

    fun `test skips the root's pure pass-through unit production`() {
        // root -> Expr is a unit production (same text range as its Expr child): filtered. The
        // literal-wrapping Expr around a single "int" token is not a unit production of another
        // composite (the terminal is a leaf, invisible to PsiElement#getChildren), so it stays.
        assertEquals(listOf("Expr"), crumbs("<caret>42"))
    }

    fun `test keeps every level that adds its own tokens`() {
        val result = crumbs("sin(<caret>1 + 2)")

        // Every level here is an Expr (see the class doc), so this is really asserting the depth:
        // the literal, the "1 + 2" sum, and the outer sin(...) call each get their own crumb.
        assertEquals(listOf("Expr", "Expr", "Expr"), result)
    }

    fun `test a crumb is just the nonterminal name, no source snippet`() {
        val result = crumbs("sin(<caret>1 + 222222222222222222)")

        assertEquals(listOf("Expr", "Expr", "Expr"), result)
    }
}
