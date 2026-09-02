package com.halotukozak.alpaca.plugin.lexer

import com.halotukozak.alpaca.plugin.grammar.TokenSpec
import org.junit.Assert.assertEquals
import org.junit.Test

class AlpacaLexerTest {
  private fun tokenize(specs: List<TokenSpec>, text: String): List<Pair<String, String>> {
    val lexer = AlpacaLexer("AlpacaLexerTest", specs)
    lexer.start(text, 0, text.length, 0)
    val tokens = mutableListOf<Pair<String, String>>()
    while (lexer.getTokenType() != null) {
      tokens += lexer.getTokenType().toString() to text.substring(lexer.getTokenStart(), lexer.getTokenEnd())
      lexer.advance()
    }
    return tokens
  }

  @Test
  fun `tokenizes a simple expression`() {
    val num = TokenSpec(name = "NUM", pattern = "[0-9]+", ignored = false)
    val plus = TokenSpec(name = "PLUS", pattern = "\\+", ignored = false)
    val ws = TokenSpec(name = "WS", pattern = "\\s+", ignored = true)

    val tokens = tokenize(listOf(num, plus, ws), "1 + 22")

    assertEquals(
      listOf("NUM" to "1", "WS" to " ", "PLUS" to "+", "WS" to " ", "NUM" to "22"),
      tokens,
    )
  }

  @Test
  fun `picks the longest match among overlapping rules, ties favor rule order`() {
    val keyword = TokenSpec(name = "IF", pattern = "if", ignored = false)
    val ident = TokenSpec(name = "IDENT", pattern = "[a-z]+", ignored = false)

    val tokens = tokenize(listOf(keyword, ident), "iffy if")

    assertEquals(
      listOf("IDENT" to "iffy", "ALPACA_BAD_CHARACTER" to " ", "IF" to "if"),
      tokens,
    )
  }

  @Test
  fun `emits a single BAD_CHARACTER token for text no rule matches`() {
    val num = TokenSpec(name = "NUM", pattern = "[0-9]+", ignored = false)

    val tokens = tokenize(listOf(num), "1$2")

    assertEquals(
      listOf("NUM" to "1", "ALPACA_BAD_CHARACTER" to "$", "NUM" to "2"),
      tokens,
    )
  }

  @Test
  fun `never selects a zero-length match, to avoid getting stuck`() {
    val maybeDigits = TokenSpec(name = "DIGITS_OPT", pattern = "[0-9]*", ignored = false)

    val tokens = tokenize(listOf(maybeDigits), "a")

    assertEquals(listOf("ALPACA_BAD_CHARACTER" to "a"), tokens)
  }
}
