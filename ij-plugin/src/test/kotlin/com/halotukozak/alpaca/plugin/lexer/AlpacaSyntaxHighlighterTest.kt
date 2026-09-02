package com.halotukozak.alpaca.plugin.lexer

import com.halotukozak.alpaca.plugin.grammar.TokenSpec
import org.junit.Assert.assertEquals
import org.junit.Test

class AlpacaSyntaxHighlighterTest {
  private fun highlightsFor(spec: TokenSpec): List<String> {
    val highlighter = AlpacaSyntaxHighlighter("AlpacaSyntaxHighlighterTest", listOf(spec))
    val tokenType = AlpacaTokenTypes.forName("AlpacaSyntaxHighlighterTest", spec.name)
    return highlighter.getTokenHighlights(tokenType).map { it.externalName }
  }

  @Test
  fun `colors a single escaped punctuation char as an operator`() {
    assertEquals(listOf("ALPACA_OPERATION_SIGN"), highlightsFor(TokenSpec("plus", "\\+", ignored = false)))
  }

  @Test
  fun `colors parentheses, brackets and braces distinctly`() {
    assertEquals(listOf("ALPACA_PARENTHESES"), highlightsFor(TokenSpec("lparen", "\\(", ignored = false)))
    assertEquals(listOf("ALPACA_BRACKETS"), highlightsFor(TokenSpec("lbracket", "\\[", ignored = false)))
    assertEquals(listOf("ALPACA_BRACES"), highlightsFor(TokenSpec("lbrace", "\\{", ignored = false)))
  }

  @Test
  fun `colors a digit class as a number`() {
    assertEquals(listOf("ALPACA_NUMBER"), highlightsFor(TokenSpec("num", "[0-9]+", ignored = false)))
  }

  @Test
  fun `colors a quote-containing pattern as a string`() {
    assertEquals(listOf("ALPACA_STRING"), highlightsFor(TokenSpec("str", "\"[^\"]*\"", ignored = false)))
  }

  @Test
  fun `colors a bare word literal as a keyword`() {
    assertEquals(listOf("ALPACA_KEYWORD"), highlightsFor(TokenSpec("ifKw", "if", ignored = false)))
  }

  @Test
  fun `leaves an identifier-class pattern uncolored`() {
    assertEquals(emptyList<String>(), highlightsFor(TokenSpec("ident", "[a-z]+", ignored = false)))
  }

  @Test
  fun `colors ignored rules as the muted comment style regardless of pattern`() {
    assertEquals(listOf("ALPACA_IGNORED"), highlightsFor(TokenSpec("ws", "\\s+", ignored = true)))
  }
}
