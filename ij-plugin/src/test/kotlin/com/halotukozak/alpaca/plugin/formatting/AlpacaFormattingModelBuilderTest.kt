package com.halotukozak.alpaca.plugin.formatting

import com.halotukozak.alpaca.plugin.lexer.AlpacaFileTypeRegistrar
import com.halotukozak.alpaca.plugin.settings.AlpacaSettingsState
import com.halotukozak.alpaca.plugin.settings.GrammarAssociation
import com.intellij.openapi.application.runWriteAction
import com.intellij.testFramework.fixtures.BasePlatformTestCase

private const val LEXER_ID = "MathTest.CalcLexer@L11"
private const val PARSER_ID = "MathTest.MathParser@L39"

/**
 * Exercises the real "Reformat Code" editor action against [AlpacaFormattingModelBuilder],
 * registered for the real `MathParser` grammar (see the exported `MathTest.CalcLexer@L11.tokens.json`
 * for its token shapes).
 */
class AlpacaFormattingModelBuilderTest : BasePlatformTestCase() {
    override fun setUp() {
        super.setUp()
        val settings = AlpacaSettingsState.getInstance(project)
        settings.exportDirectory = "/tmp/alpaca-grammar-export"
        settings.associations =
            mutableListOf(GrammarAssociation(extension = "calc", lexerGrammarId = LEXER_ID, parserGrammarId = PARSER_ID))
        runWriteAction { AlpacaFileTypeRegistrar.ensureRegistered("calc", LEXER_ID) }
    }

    private fun reformat(text: String): String {
        myFixture.configureByText("test.calc", text)
        myFixture.performEditorAction("ReformatCode")
        return myFixture.editor.document.text
    }

    fun `test spaces an operator with no spaces around it`() {
        assertEquals("1 + 2", reformat("1+2"))
    }

    fun `test collapses extra spaces around an operator to one`() {
        assertEquals("1 + 2", reformat("1    +    2"))
    }

    fun `test removes space just inside a paren pair`() {
        assertEquals("(1 + 2)", reformat("( 1 + 2 )"))
    }

    fun `test removes space before a comma but keeps one after it`() {
        // atan2 is a keyword-shaped token like any other, so the generic default (one space
        // between tokens with no special rule) still applies right before its own "(" -- the
        // grammar-agnostic design has no notion of "function call" to special-case that away.
        assertEquals("atan2 (1, 1)", reformat("atan2 ( 1 , 1 )"))
    }

    fun `test indents a wrapped paren group by one level and keeps the wrap`() {
        val result =
            reformat(
                """
                tan(
                (1 + 2) * (3 - 4)
                )
                """.trimIndent(),
            )

        assertEquals(
            """
            tan (
                (1 + 2) * (3 - 4)
            )
            """.trimIndent(),
            result,
        )
    }

    fun `test still applies the generic default spacing when the file's grammar is unresolved`() {
        // The extension is still registered as an Alpaca file type, but no association names a
        // grammar for it -- the same situation a stale or mistyped Settings entry would leave.
        AlpacaSettingsState.getInstance(project).associations = mutableListOf()

        assertEquals("1 + 2", reformat("1    +    2"))
    }
}
