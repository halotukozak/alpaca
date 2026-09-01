package com.halotukozak.alpaca.plugin.grammar

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files

class GrammarDirectoryTest {
  @Test
  fun `scans tokens and productions json files separately, ignoring unrelated files`() {
    val dir = Files.createTempDirectory("grammar-directory-test")
    try {
      Files.writeString(
        dir.resolve("BrainLexer.BrainLexer@L9.tokens.json"),
        """[{"name":"+","pattern":"\\+","ignored":false}]""",
      )
      Files.writeString(
        dir.resolve("CalcLexer.CalcLexer@L15.tokens.json"),
        """[{"name":"Num","pattern":"[0-9]+","ignored":false}]""",
      )
      Files.writeString(
        dir.resolve("BrainParser.BrainParser@L12.productions.json"),
        """[{"lhs":"root","rhs":[],"name":null}]""",
      )
      Files.writeString(
        dir.resolve("BrainParser.BrainParser@L12.table.json"),
        """[[{"symbol":{"kind":"terminal","name":"+"},"action":{"type":"shift","state":1}}]]""",
      )
      Files.writeString(dir.resolve("README.md"), "not a grammar export")

      val grammars = GrammarDirectory.scan(dir)

      assertEquals(
        listOf(
          LexerGrammar("BrainLexer.BrainLexer@L9", listOf(TokenSpec("+", "\\+", ignored = false))),
          LexerGrammar("CalcLexer.CalcLexer@L15", listOf(TokenSpec("Num", "[0-9]+", ignored = false))),
        ),
        grammars.lexers,
      )
      assertEquals(
        listOf(
          ParserGrammar(
            "BrainParser.BrainParser@L12",
            listOf(ProductionSpec("root", emptyList(), null)),
            table = listOf(listOf(TableEntry(SymbolSpec("terminal", "+"), ActionSpec.Shift(1)))),
          ),
        ),
        grammars.parsers,
      )
    } finally {
      Files.walk(dir).sorted(Comparator.reverseOrder()).forEach(Files::delete)
    }
  }

  @Test
  fun `returns empty lexer and parser lists for a directory that doesn't exist`() {
    val missing = Files.createTempDirectory("grammar-directory-test").resolve("does-not-exist")

    val grammars = GrammarDirectory.scan(missing)

    assertEquals(emptyList<LexerGrammar>(), grammars.lexers)
    assertEquals(emptyList<ParserGrammar>(), grammars.parsers)
  }
}
