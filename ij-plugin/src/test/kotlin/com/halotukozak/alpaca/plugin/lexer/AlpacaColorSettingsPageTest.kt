package com.halotukozak.alpaca.plugin.lexer

import com.intellij.openapi.editor.colors.TextAttributesKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlpacaColorSettingsPageTest {
    private val page = AlpacaColorSettingsPage()

    @Test
    fun `every attribute descriptor is exercised by the demo text`() {
        val highlighter = page.highlighter
        val lexer = highlighter.highlightingLexer
        val demoText = page.demoText
        lexer.start(demoText, 0, demoText.length, 0)

        val keysHit = mutableSetOf<TextAttributesKey>()
        while (lexer.tokenType != null) {
            highlighter.getTokenHighlights(lexer.tokenType).forEach(keysHit::add)
            lexer.advance()
        }

        val descriptorKeys = page.attributeDescriptors.map { it.key }.toSet()
        assertEquals(
            "demo text does not exercise: ${descriptorKeys - keysHit}",
            descriptorKeys,
            keysHit,
        )
    }

    @Test
    fun `descriptor names are unique and non-blank`() {
        val names = page.attributeDescriptors.map { it.displayName }
        assertTrue(names.none { it.isBlank() })
        assertEquals(names.size, names.toSet().size)
    }
}
