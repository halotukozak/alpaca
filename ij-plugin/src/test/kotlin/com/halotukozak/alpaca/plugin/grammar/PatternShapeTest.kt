package com.halotukozak.alpaca.plugin.grammar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PatternShapeTest {
  @Test
  fun `recognizes a hash line comment`() {
    assertEquals("#", lineCommentPrefixOf("#.*"))
  }

  @Test
  fun `recognizes a multi-character line comment prefix`() {
    assertEquals("//", lineCommentPrefixOf("//.*"))
  }

  @Test
  fun `rejects a pattern without the trailing dot-star`() {
    assertNull(lineCommentPrefixOf("#"))
  }

  @Test
  fun `rejects a pattern whose prefix is not a plain literal`() {
    assertNull(lineCommentPrefixOf("[a-z]+.*"))
  }

  @Test
  fun `rejects a pattern that is only the dot-star`() {
    assertNull(lineCommentPrefixOf(".*"))
  }
}
