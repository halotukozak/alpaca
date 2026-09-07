package com.halotukozak.alpaca.plugin.hints

import com.halotukozak.alpaca.plugin.lexer.AlpacaFileTypeRegistrar
import com.halotukozak.alpaca.plugin.settings.AlpacaSettingsState
import com.halotukozak.alpaca.plugin.settings.GrammarAssociation
import com.intellij.codeInsight.hints.declarative.CollapseState
import com.intellij.codeInsight.hints.declarative.CollapsiblePresentationTreeBuilder
import com.intellij.codeInsight.hints.declarative.HintFormat
import com.intellij.codeInsight.hints.declarative.InlayActionData
import com.intellij.codeInsight.hints.declarative.InlayPayload
import com.intellij.codeInsight.hints.declarative.InlayPosition
import com.intellij.codeInsight.hints.declarative.InlayTreeSink
import com.intellij.codeInsight.hints.declarative.InlineInlayPosition
import com.intellij.codeInsight.hints.declarative.PresentationTreeBuilder
import com.intellij.codeInsight.hints.declarative.SharedBypassCollector
import com.intellij.openapi.application.runWriteAction
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

private const val LEXER_ID = "MathTest.CalcLexer@L11"
private const val PARSER_ID = "MathTest.MathParser@L39"

/**
 * Drives [AlpacaParseTreeInlayHintsProvider]'s collector over a real MathParser PSI tree. MathParser
 * names every `Expr` alternative (`plus`, `sin`, `atan2`, ...) but its bare-literal alternatives
 * (`Expr -> int`, `Expr -> ( Expr )` is unnamed) are single-symbol or unnamed, so only the
 * operator/function nodes get a hint.
 */
class AlpacaParseTreeInlayHintsProviderTest : BasePlatformTestCase() {
    private val provider = AlpacaParseTreeInlayHintsProvider()

    override fun setUp() {
        super.setUp()
        val settings = AlpacaSettingsState.getInstance(project)
        settings.exportDirectory = "/tmp/alpaca-grammar-export"
        settings.associations =
            mutableListOf(GrammarAssociation(extension = "calc", lexerGrammarId = LEXER_ID, parserGrammarId = PARSER_ID))
        runWriteAction { AlpacaFileTypeRegistrar.ensureRegistered("calc", LEXER_ID) }
    }

    /** Every hint the provider emits for [text], as `offset to label`, in document order. */
    private fun hints(text: String): List<Pair<Int, String>> {
        val file = myFixture.configureByText("test.calc", text)
        val collector =
            provider.createCollector(file, myFixture.editor) as? SharedBypassCollector
                ?: return emptyList()
        val sink = RecordingSink()
        PsiTreeUtil.processElements(file) {
            collector.collectFromElement(it, sink)
            true
        }
        return sink.hints.sortedBy { it.first }
    }

    fun `test labels a binary operator node with its production alternative`() {
        assertEquals(listOf(0 to "plus"), hints("1 + 2"))
    }

    fun `test labels nested operators, innermost included`() {
        // `1 + 2 * 3` parses as plus(1, times(2, 3)); both operator nodes are labelled, the bare
        // int literals are not (Expr -> int is a single-symbol alternative).
        assertEquals(listOf(0 to "plus", 4 to "times"), hints("1 + 2 * 3"))
    }

    fun `test labels a function-call node`() {
        assertEquals(listOf(0 to "sin", 4 to "plus"), hints("sin(1 + 2)"))
    }

    fun `test no hints when the file's grammar has no parser association`() {
        AlpacaSettingsState.getInstance(project).associations =
            mutableListOf(GrammarAssociation(extension = "calc", lexerGrammarId = LEXER_ID, parserGrammarId = ""))

        assertEquals(emptyList<Pair<Int, String>>(), hints("1 + 2"))
    }

    fun `test no collector outside an Alpaca language`() {
        val file = myFixture.configureByText("plain.txt", "1 + 2")

        assertNull(provider.createCollector(file, myFixture.editor))
    }

    private class RecordingSink : InlayTreeSink {
        val hints = mutableListOf<Pair<Int, String>>()

        override fun addPresentation(
            position: InlayPosition,
            payloads: List<InlayPayload>?,
            tooltip: String?,
            hintFormat: HintFormat,
            builder: PresentationTreeBuilder.() -> Unit,
        ) {
            val offset = (position as InlineInlayPosition).offset
            RecordingTreeBuilder { hints += offset to it }.builder()
        }

        override fun whenOptionEnabled(
            optionId: String,
            block: () -> Unit,
        ) = block()
    }

    private class RecordingTreeBuilder(
        private val onText: (String) -> Unit,
    ) : PresentationTreeBuilder {
        override fun text(
            text: String,
            actionData: InlayActionData?,
        ) = onText(text)

        override fun list(builder: PresentationTreeBuilder.() -> Unit) = builder()

        override fun collapsibleList(
            state: CollapseState,
            expandedState: CollapsiblePresentationTreeBuilder.() -> Unit,
            collapsedState: CollapsiblePresentationTreeBuilder.() -> Unit,
        ) = Unit

        override fun clickHandlerScope(
            actionData: InlayActionData,
            builder: PresentationTreeBuilder.() -> Unit,
        ) = builder()
    }
}
