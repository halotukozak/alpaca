package com.halotukozak.alpaca.plugin.grammar

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files

/** Matches the exact JSON shapes written by alpaca's internal.lexer/parser.GrammarExport. */
class GrammarFileTest {
    @Test
    fun `reads a tokens json file written by Alpaca's compile-time export`() {
        val json = """[{"name":"\\s+","pattern":"\\s+","ignored":true},{"name":"int","pattern":"[0-9]+","ignored":false}]"""
        val path = Files.createTempFile("grammar-file-test", ".tokens.json")
        try {
            Files.writeString(path, json)

            val specs = LexerGrammarFile.read(path)

            assertEquals(
                listOf(
                    TokenSpec(name = "\\s+", pattern = "\\s+", ignored = true),
                    TokenSpec(name = "int", pattern = "[0-9]+", ignored = false),
                ),
                specs,
            )
        } finally {
            Files.deleteIfExists(path)
        }
    }

    @Test
    fun `reads a productions json file written by Alpaca's compile-time export`() {
        val json =
            """[{"lhs":"root","rhs":[{"kind":"terminal","name":"T"}],"name":null},""" +
                """{"lhs":"Expr","rhs":[{"kind":"nonterminal","name":"Expr"},{"kind":"terminal","name":"+"}],"name":"plus"}]"""
        val path = Files.createTempFile("grammar-file-test", ".productions.json")
        try {
            Files.writeString(path, json)

            val specs = ParserGrammarFile.read(path)

            assertEquals(
                listOf(
                    ProductionSpec(lhs = "root", rhs = listOf(SymbolSpec("terminal", "T")), name = null),
                    ProductionSpec(
                        lhs = "Expr",
                        rhs = listOf(SymbolSpec("nonterminal", "Expr"), SymbolSpec("terminal", "+")),
                        name = "plus",
                    ),
                ),
                specs,
            )
        } finally {
            Files.deleteIfExists(path)
        }
    }

    @Test
    fun `reads a table json file written by Alpaca's compile-time export`() {
        val json =
            """[""" +
                """[{"symbol":{"kind":"terminal","name":"int"},"action":{"type":"shift","state":1}}],""" +
                """[{"symbol":{"kind":"terminal","name":"$"},""" +
                """"action":{"type":"reduce","production":{"lhs":"root","rhs":[],"name":null}}}]""" +
                """]"""
        val path = Files.createTempFile("grammar-file-test", ".table.json")
        try {
            Files.writeString(path, json)

            val rows = ParserTableFile.read(path)

            assertEquals(
                listOf(
                    listOf(TableEntry(SymbolSpec("terminal", "int"), ActionSpec.Shift(1))),
                    listOf(
                        TableEntry(
                            SymbolSpec("terminal", "$"),
                            ActionSpec.Reduce(ProductionSpec("root", emptyList(), null)),
                        ),
                    ),
                ),
                rows,
            )
        } finally {
            Files.deleteIfExists(path)
        }
    }
}
