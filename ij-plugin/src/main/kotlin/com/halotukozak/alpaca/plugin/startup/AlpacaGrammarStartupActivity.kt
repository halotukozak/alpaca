package com.halotukozak.alpaca.plugin.startup

import com.halotukozak.alpaca.plugin.grammar.GrammarDirectory
import com.halotukozak.alpaca.plugin.lexer.AlpacaFileTypeRegistrar
import com.halotukozak.alpaca.plugin.settings.AlpacaSettingsState
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import java.nio.file.Path

/**
 * On project open, scans the configured grammar export directory (Settings |
 * Tools | Alpaca, falling back to `ALPACA_GRAMMAR_EXPORT_DIR`) for grammars
 * exported by Alpaca's compile-time `lexer{...}`/parser macros, registers a
 * file type for each configured extension-to-grammar mapping, and logs what
 * it found.
 */
class AlpacaGrammarStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val settings = AlpacaSettingsState.getInstance(project)
        val dir = settings.resolvedExportDirectory() ?: return

        writeAction {
            settings.associations.forEach { AlpacaFileTypeRegistrar.ensureRegistered(it.extension, it.lexerGrammarId) }
        }

        val grammars = GrammarDirectory.scan(Path.of(dir))
        thisLogger().info(
            "Alpaca: found ${grammars.lexers.size} lexer(s) and ${grammars.parsers.size} parser(s) in $dir: " +
                "lexers=${grammars.lexers.map { it.id }}, parsers=${grammars.parsers.map { it.id }}",
        )
    }
}
