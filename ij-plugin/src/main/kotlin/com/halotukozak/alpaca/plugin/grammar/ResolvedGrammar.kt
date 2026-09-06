package com.halotukozak.alpaca.plugin.grammar

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/** The grammar a given file resolves to: always a lexer, and (if the Settings association names
 *  one) a parser. */
data class ResolvedGrammar(
    val lexerId: String,
    val tokens: List<TokenSpec>,
    val parserGrammar: ParserGrammar?,
)

/** Resolves [virtualFile]'s grammar from Settings | Tools | Alpaca, via the per-project
 *  [GrammarService] cache rather than re-scanning the export directory on every call. */
fun resolveGrammarForFile(
    project: Project,
    virtualFile: VirtualFile,
): ResolvedGrammar? = GrammarService.getInstance(project).resolveForFile(virtualFile)
