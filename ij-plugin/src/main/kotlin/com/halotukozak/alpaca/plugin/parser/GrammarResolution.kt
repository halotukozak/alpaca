package com.halotukozak.alpaca.plugin.parser

import com.halotukozak.alpaca.plugin.grammar.GrammarDirectory
import com.halotukozak.alpaca.plugin.grammar.ParserGrammar
import com.halotukozak.alpaca.plugin.grammar.TokenSpec
import com.halotukozak.alpaca.plugin.settings.AlpacaSettingsState
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Path

/** The grammar a given file resolves to: always a lexer, and (if the Settings association names
 *  one) a parser -- see [AlpacaFileElementType] and [com.halotukozak.alpaca.plugin.completion.AlpacaCompletionEngine],
 *  the two places that need this to actually drive the file's grammar. */
data class ResolvedGrammar(
  val lexerId: String,
  val tokens: List<TokenSpec>,
  val parserGrammar: ParserGrammar?,
)

/** Resolves [virtualFile]'s grammar from Settings | Tools | Alpaca: the extension-to-grammar
 *  association, then the actual exported lexer/parser data from the configured export directory. */
fun resolveGrammarForFile(
  project: Project,
  virtualFile: VirtualFile,
): ResolvedGrammar? {
  val settings = AlpacaSettingsState.getInstance(project)
  val exportDirectory = settings.resolvedExportDirectory() ?: return null
  val extension = virtualFile.extension ?: return null
  val association = settings.associationForExtension(extension) ?: return null

  val grammars = GrammarDirectory.scan(Path.of(exportDirectory))
  val tokens = grammars.lexers.firstOrNull { it.id == association.lexerGrammarId }?.tokens ?: return null
  val parserGrammar =
    association.parserGrammarId
      .takeIf { it.isNotBlank() }
      ?.let { parserId -> grammars.parsers.firstOrNull { it.id == parserId } }

  return ResolvedGrammar(association.lexerGrammarId, tokens, parserGrammar)
}
