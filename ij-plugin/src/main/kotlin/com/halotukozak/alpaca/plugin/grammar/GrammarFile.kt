package com.halotukozak.alpaca.plugin.grammar

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path

/**
 * One token rule as exported by Alpaca's `lexer{...}` macro (see
 * `ALPACA_GRAMMAR_EXPORT_DIR` in the alpaca library): a name, the regex
 * pattern that matches it, and whether matches are dropped from the lexeme
 * stream.
 */
@Serializable
data class TokenSpec(
    val name: String,
    val pattern: String,
    val ignored: Boolean,
)

/** Reads a `<lexer>.tokens.json` file written by Alpaca's compile-time grammar export. */
object LexerGrammarFile {
    const val SUFFIX = ".tokens.json"

    fun read(path: Path): List<TokenSpec> = Json.decodeFromString(Files.readString(path))
}

/** A single lexer's exported grammar, identified by its export file name (sans the `.tokens.json` suffix). */
data class LexerGrammar(
    val id: String,
    val tokens: List<TokenSpec>,
)

/** One grammar symbol referenced from a production's right-hand side. */
@Serializable
data class SymbolSpec(
    val kind: String,
    val name: String,
)

/**
 * One production rule as exported by Alpaca's parser `Tables[Ctx]` macro: its
 * left-hand side nonterminal, right-hand side symbols (empty for an epsilon
 * production), and the optional name it was defined with.
 */
@Serializable
data class ProductionSpec(
    val lhs: String,
    val rhs: List<SymbolSpec>,
    val name: String?,
)

/** Reads a `<parser>.productions.json` file written by Alpaca's compile-time grammar export. */
object ParserGrammarFile {
    const val SUFFIX = ".productions.json"

    fun read(path: Path): List<ProductionSpec> = Json.decodeFromString(Files.readString(path))
}

/**
 * A shift-or-reduce action for one (state, symbol) cell of an exported LR(1) parse table.
 * The `@SerialName`s match the `"type"` discriminator Alpaca's export writes.
 */
@Serializable
sealed interface ActionSpec {
    @Serializable
    @SerialName("shift")
    data class Shift(val state: Int) : ActionSpec

    @Serializable
    @SerialName("reduce")
    data class Reduce(val production: ProductionSpec) : ActionSpec
}

/** One (symbol, action) cell in an exported LR(1) table state's row. */
@Serializable
data class TableEntry(
    val symbol: SymbolSpec,
    val action: ActionSpec,
)

/**
 * Reads a `<parser>.table.json` file written by Alpaca's compile-time grammar export: the
 * parser's already conflict-resolved LR(1) table, one row of (symbol, action) entries per state
 * (dense, consecutive state ids starting at 0, matching the row's index in the returned list).
 */
object ParserTableFile {
    const val SUFFIX = ".table.json"

    fun read(path: Path): List<List<TableEntry>> = Json.decodeFromString(Files.readString(path))
}

/**
 * A single parser's exported grammar, identified by its export file name (sans the
 * `.productions.json` suffix). [table] is empty when no matching `.table.json` was found
 * alongside the productions (e.g. an export written before the table export was added).
 */
data class ParserGrammar(
    val id: String,
    val productions: List<ProductionSpec>,
    val table: List<List<TableEntry>> = emptyList(),
)

/** All grammars found in an `ALPACA_GRAMMAR_EXPORT_DIR` directory, lexers and parsers scanned separately. */
data class ExportedGrammars(
    val lexers: List<LexerGrammar>,
    val parsers: List<ParserGrammar>,
)

/**
 * Scans an `ALPACA_GRAMMAR_EXPORT_DIR` directory for `*.tokens.json` and
 * `*.productions.json` files written by Alpaca's compile-time grammar
 * export, one per `lexer{...}`/parser call site.
 */
object GrammarDirectory {
    fun scan(dir: Path): ExportedGrammars {
        if (!Files.isDirectory(dir)) return ExportedGrammars(emptyList(), emptyList())

        val lexers =
            Files.list(dir).use { entries ->
                entries
                    .filter { it.fileName.toString().endsWith(LexerGrammarFile.SUFFIX) }
                    .map { path ->
                        LexerGrammar(
                            id = path.fileName.toString().removeSuffix(LexerGrammarFile.SUFFIX),
                            tokens = LexerGrammarFile.read(path),
                        )
                    }.sorted(compareBy { it.id })
                    .toList()
            }

        val tablesById =
            Files.list(dir).use { entries ->
                entries
                    .filter { it.fileName.toString().endsWith(ParserTableFile.SUFFIX) }
                    .map { path -> path.fileName.toString().removeSuffix(ParserTableFile.SUFFIX) to ParserTableFile.read(path) }
                    .toList()
            }.toMap()

        val parsers =
            Files.list(dir).use { entries ->
                entries
                    .filter { it.fileName.toString().endsWith(ParserGrammarFile.SUFFIX) }
                    .map { path ->
                        val id = path.fileName.toString().removeSuffix(ParserGrammarFile.SUFFIX)
                        ParserGrammar(
                            id = id,
                            productions = ParserGrammarFile.read(path),
                            table = tablesById[id] ?: emptyList(),
                        )
                    }.sorted(compareBy { it.id })
                    .toList()
            }

        return ExportedGrammars(lexers, parsers)
    }
}
