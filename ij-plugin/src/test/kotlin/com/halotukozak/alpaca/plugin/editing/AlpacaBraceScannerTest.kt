package com.halotukozak.alpaca.plugin.editing

import com.halotukozak.alpaca.plugin.grammar.TokenSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlpacaBraceScannerTest {
    private val tokens =
        listOf(
            TokenSpec("ws", "\\s+", ignored = true),
            TokenSpec("lparen", "\\(", ignored = false),
            TokenSpec("rparen", "\\)", ignored = false),
            TokenSpec("lbracket", "\\[", ignored = false),
            TokenSpec("rbracket", "\\]", ignored = false),
            TokenSpec("lbrace", "\\{", ignored = false),
            TokenSpec("rbrace", "\\}", ignored = false),
            TokenSpec("string", "\"[^\"]*\"", ignored = false),
            TokenSpec("word", "[a-z]+", ignored = false),
        )

    /** `|` in [textWithCaret] marks the caret; returns the matched (opening, closing) start
     *  offsets, or null. */
    private fun match(textWithCaret: String): Pair<Int, Int>? {
        val caret = textWithCaret.indexOf('|')
        val text = textWithCaret.replace("|", "")
        return AlpacaBraceScanner
            .matchAt(text, "brace-scanner-test", tokens, caret)
            ?.let { it.first.startOffset to it.second.startOffset }
    }

    @Test
    fun `matches an opening paren to its closer with the caret just before it`() {
        // f(a)  ->  ( at 1, ) at 3
        assertEquals(1 to 3, match("f|(a)"))
    }

    @Test
    fun `matches a closing paren to its opener with the caret just after it`() {
        assertEquals(1 to 3, match("f(a)|"))
    }

    @Test
    fun `respects nesting of the same bracket kind`() {
        // ((()))  ->  outer ( at 0 pairs with ) at 5
        assertEquals(0 to 5, match("|((()))"))
        // caret after the second (  ->  ( at 1 pairs with ) at 4
        assertEquals(1 to 4, match("(|(())) "))
    }

    @Test
    fun `pairs different bracket kinds when they nest properly`() {
        // {[]}  ->  { at 0 pairs with } at 3
        assertEquals(0 to 3, match("|{[]}"))
    }

    @Test
    fun `ignores brackets inside string tokens`() {
        // f( "(" )  ->  ( at 1 pairs with ) at 7, not with the ( inside the string
        assertEquals(1 to 7, match("f|( \"(\" )"))
    }

    @Test
    fun `returns null when the caret is not next to a bracket`() {
        assertNull(match("fo|o (a)"))
    }

    @Test
    fun `returns null for an unbalanced bracket`() {
        assertNull(match("f|(a"))
    }

    @Test
    fun `returns null when bracket kinds are interleaved wrong`() {
        assertNull(match("|( ]"))
    }

    @Test
    fun `returns null for a grammar with no bracket tokens`() {
        val noBrackets = listOf(TokenSpec("word", "[a-z]+", ignored = false))
        assertNull(AlpacaBraceScanner.matchAt("abc", "no-brackets", noBrackets, 0))
    }
}
