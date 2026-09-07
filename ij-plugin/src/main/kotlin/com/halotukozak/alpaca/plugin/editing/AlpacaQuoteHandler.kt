package com.halotukozak.alpaca.plugin.editing

import com.halotukozak.alpaca.plugin.grammar.resolveGrammarForFile
import com.halotukozak.alpaca.plugin.grammar.stringQuoteOf
import com.intellij.codeInsight.editorActions.QuoteHandler
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.highlighter.HighlighterIterator
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.ProjectLocator

/**
 * "Insert pair quote" (and typing over a closing quote) for Alpaca-defined languages.
 *
 * The plugin registers one file type per grammar with a runtime-generated name, so this can't go
 * through the file-type-keyed `quoteHandler` EP; it's a `lang.quoteHandler` on the shared
 * [com.halotukozak.alpaca.plugin.lexer.AlpacaLanguage] instead, and works out per file whether the
 * grammar even has string literals.
 *
 * A "string literal" is a highlighter token that starts with one of the quote characters the
 * file's grammar actually uses for a string-shaped rule ([stringQuoteOf] -- the same shape
 * [com.halotukozak.alpaca.plugin.lexer.AlpacaSyntaxHighlighter] paints as a string). A grammar
 * with no such rule (a calculator, Brainfuck) resolves to an empty quote set and this handler
 * stays completely inert.
 */
class AlpacaQuoteHandler : QuoteHandler {
    override fun isOpeningQuote(
        iterator: HighlighterIterator,
        offset: Int,
    ): Boolean = offset == iterator.start && startsStringLiteral(iterator.document, iterator.start)

    override fun isClosingQuote(
        iterator: HighlighterIterator,
        offset: Int,
    ): Boolean {
        val chars = iterator.document.charsSequence
        val start = iterator.start
        val end = iterator.end
        // A closed literal: opens and closes with the same quote, and the caret is on that close.
        return end - start >= 2 &&
            offset == end - 1 &&
            startsStringLiteral(iterator.document, start) &&
            chars[end - 1] == chars[start]
    }

    override fun hasNonClosedLiteral(
        editor: Editor,
        iterator: HighlighterIterator,
        offset: Int,
    ): Boolean {
        val document = editor.document
        val quotes = quoteChars(document)
        if (quotes.isEmpty()) return false
        val chars = document.charsSequence
        val lineEnd = document.getLineEndOffset(document.getLineNumber(offset))
        try {
            while (!iterator.atEnd() && iterator.start < lineEnd) {
                val start = iterator.start
                if (start < chars.length && chars[start] in quotes) {
                    if (iterator.end - start < 2 || chars[iterator.end - 1] != chars[start]) return true
                }
                iterator.advance()
            }
        } finally {
            while (!iterator.atEnd() && iterator.start != offset) iterator.retreat()
        }
        return false
    }

    override fun isInsideLiteral(iterator: HighlighterIterator): Boolean = startsStringLiteral(iterator.document, iterator.start)

    private fun startsStringLiteral(
        document: Document,
        offset: Int,
    ): Boolean {
        val chars = document.charsSequence
        return offset in 0 until chars.length && chars[offset] in quoteChars(document)
    }

    private fun quoteChars(document: Document): Set<Char> {
        val virtualFile = FileDocumentManager.getInstance().getFile(document) ?: return emptySet()
        val project = ProjectLocator.getInstance().guessProjectForFile(virtualFile) ?: return emptySet()
        val resolved = resolveGrammarForFile(project, virtualFile) ?: return emptySet()
        return resolved.tokens.mapNotNullTo(HashSet()) { stringQuoteOf(it.pattern) }
    }
}
