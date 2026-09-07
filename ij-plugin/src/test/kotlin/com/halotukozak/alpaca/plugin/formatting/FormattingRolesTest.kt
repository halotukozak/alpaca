package com.halotukozak.alpaca.plugin.formatting

import com.halotukozak.alpaca.plugin.grammar.BracketKind
import com.halotukozak.alpaca.plugin.grammar.TokenSpec
import com.halotukozak.alpaca.plugin.lexer.AlpacaTokenTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormattingRolesTest {
    private val grammarId = "formatting-roles-test"

    private val tokens =
        listOf(
            TokenSpec("ws", "\\s+", ignored = true),
            TokenSpec("lparen", "\\(", ignored = false),
            TokenSpec("rparen", "\\)", ignored = false),
            TokenSpec("lbracket", "\\[", ignored = false),
            TokenSpec("rbracket", "\\]", ignored = false),
            TokenSpec("comma", ",", ignored = false),
            TokenSpec("semi", ";", ignored = false),
            // Escaped, like a real grammar's member-access dot -- a bare `.` is the regex "any
            // character" and (per BrainLexer's own catch-all ignored rule) not a literal dot.
            TokenSpec("dot", "\\.", ignored = false),
            TokenSpec("word", "[a-z]+", ignored = false),
        )

    private val roles = FormattingRoles.of(grammarId, tokens)

    private fun typeOf(name: String) = AlpacaTokenTypes.forName(grammarId, name)

    @Test
    fun `classifies matching bracket pairs into openers and closers`() {
        assertTrue(roles.openers.contains(typeOf("lparen")))
        assertTrue(roles.openers.contains(typeOf("lbracket")))
        assertTrue(roles.closers.contains(typeOf("rparen")))
        assertTrue(roles.closers.contains(typeOf("rbracket")))
        assertFalse(roles.openers.contains(typeOf("rparen")))
        assertFalse(roles.closers.contains(typeOf("lparen")))
    }

    @Test
    fun `roleOf reports the bracket kind and side`() {
        val lparen = requireNotNull(roles.roleOf(typeOf("lparen")))
        assertEquals(BracketKind.PARENTHESIS, lparen.kind)
        assertTrue(lparen.opening)

        val rbracket = requireNotNull(roles.roleOf(typeOf("rbracket")))
        assertEquals(BracketKind.BRACKET, rbracket.kind)
        assertFalse(rbracket.opening)
    }

    @Test
    fun `classifies comma semicolon and dot tokens`() {
        assertTrue(roles.commas.contains(typeOf("comma")))
        assertTrue(roles.semicolons.contains(typeOf("semi")))
        assertTrue(roles.dots.contains(typeOf("dot")))
        assertFalse(roles.commas.contains(typeOf("semi")))
    }

    @Test
    fun `leaves an identifier-shaped token unclassified`() {
        assertNull(roles.roleOf(typeOf("word")))
        assertFalse(roles.commas.contains(typeOf("word")))
        assertFalse(roles.openers.contains(typeOf("word")))
    }

    @Test
    fun `an empty token list classifies nothing`() {
        val empty = FormattingRoles.of("formatting-roles-test-empty", emptyList())
        assertNull(empty.roleOf(typeOf("lparen")))
        assertTrue(empty.openers.types.isEmpty())
        assertTrue(empty.commas.types.isEmpty())
    }
}
