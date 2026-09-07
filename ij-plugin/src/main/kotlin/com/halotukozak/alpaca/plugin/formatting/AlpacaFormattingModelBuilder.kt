package com.halotukozak.alpaca.plugin.formatting

import com.halotukozak.alpaca.plugin.grammar.GrammarService
import com.halotukozak.alpaca.plugin.lexer.AlpacaLanguage
import com.intellij.formatting.FormattingContext
import com.intellij.formatting.FormattingModel
import com.intellij.formatting.FormattingModelBuilder
import com.intellij.formatting.FormattingModelProvider
import com.intellij.formatting.Indent
import com.intellij.formatting.SpacingBuilder

/**
 * Grammar-agnostic reformatting for Alpaca-defined languages: resolves the file's grammar the
 * same way [com.halotukozak.alpaca.plugin.editing.AlpacaBraceHighlighter] and
 * [com.halotukozak.alpaca.plugin.lexer.AlpacaSyntaxHighlighterFactory] do, classifies its tokens
 * by shape ([FormattingRoles]), and builds an [AlpacaBlock] tree from that -- no per-grammar code,
 * just opinionated defaults: a single space between most tokens, none just inside a bracket pair
 * or around `,`/`;`/`.`, and one indent level for whatever a matching bracket pair encloses.
 */
class AlpacaFormattingModelBuilder : FormattingModelBuilder {
    override fun createModel(formattingContext: FormattingContext): FormattingModel {
        val file = formattingContext.containingFile
        val settings = formattingContext.codeStyleSettings
        val virtualFile = file.virtualFile ?: file.originalFile.virtualFile
        val resolved = virtualFile?.let { GrammarService.getInstance(formattingContext.project).resolveForFile(it) }
        val roles = FormattingRoles.of(resolved?.lexerId ?: "<unresolved>", resolved?.tokens.orEmpty())

        val spacingBuilder =
            SpacingBuilder(settings, AlpacaLanguage)
                .after(roles.openers)
                .spaceIf(false)
                .before(roles.closers)
                .spaceIf(false)
                .before(roles.commas)
                .spaceIf(false)
                .before(roles.semicolons)
                .spaceIf(false)
                .around(roles.dots)
                .spaceIf(false)

        val rootBlock = AlpacaBlock(file.node, roles, spacingBuilder, Indent.getNoneIndent())
        return FormattingModelProvider.createFormattingModelForPsiFile(file, rootBlock, settings)
    }
}
