package com.halotukozak.alpaca.plugin.formatting

import com.halotukozak.alpaca.plugin.lexer.AlpacaFileTypeRegistrar
import com.halotukozak.alpaca.plugin.settings.AlpacaSettingsState
import com.halotukozak.alpaca.plugin.settings.GrammarAssociation
import com.intellij.openapi.application.runWriteAction
import com.intellij.testFramework.fixtures.BasePlatformTestCase

private const val LEXER_ID = "BrainLexer.BrainLexer@L9"
private const val PARSER_ID = "BrainParser.BrainParser@L12"

/**
 * The same [AlpacaFormattingModelBuilder] against a completely different grammar -- Brainfuck
 * (plus named functions), whose only bracket kind is `[`/`]` and which has no comma, semicolon,
 * or dot tokens at all -- with no code specific to either grammar. Complements
 * [AlpacaFormattingModelBuilderTest], which uses `MathParser`'s `(`/`)`, `,`.
 */
class AlpacaFormattingModelBuilderBrainfuckTest : BasePlatformTestCase() {
    override fun setUp() {
        super.setUp()
        val settings = AlpacaSettingsState.getInstance(project)
        settings.exportDirectory = "/tmp/alpaca-grammar-export"
        settings.associations =
            mutableListOf(GrammarAssociation(extension = "bf", lexerGrammarId = LEXER_ID, parserGrammarId = PARSER_ID))
        runWriteAction { AlpacaFileTypeRegistrar.ensureRegistered("bf", LEXER_ID) }
    }

    private fun reformat(text: String): String {
        myFixture.configureByText("test.bf", text)
        myFixture.performEditorAction("ReformatCode")
        return myFixture.editor.document.text
    }

    fun `test a single-operation loop needs no spacing changes`() {
        assertEquals("[+]", reformat("[+]"))
    }

    fun `test indents a wrapped loop body by one level`() {
        val result = reformat("[\n+\n]")

        assertEquals("[\n    +\n]", result)
    }

    fun `test spaces consecutive operations that have no special role`() {
        // Brainfuck's own convention packs operations tight with no separators at all, but none
        // of `+`/`-`/`>`/`<` is a bracket/comma/semicolon/dot, so they get the same generic
        // default spacing as any other unclassified token pair -- a known tradeoff of having no
        // per-grammar rules, not a bug specific to this grammar.
        assertEquals("+ + +", reformat("+++"))
    }
}
