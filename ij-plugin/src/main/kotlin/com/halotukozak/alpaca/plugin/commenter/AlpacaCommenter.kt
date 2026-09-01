package com.halotukozak.alpaca.plugin.commenter

import com.halotukozak.alpaca.plugin.grammar.lineCommentPrefixOf
import com.halotukozak.alpaca.plugin.parser.resolveGrammarForFile
import com.intellij.codeInsight.generation.CommenterDataHolder
import com.intellij.codeInsight.generation.SelfManagingCommenter
import com.intellij.lang.Commenter
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile

/** Carries the resolved line-comment prefix (or null, if this file's grammar has none recognizable
 *  as one -- see [lineCommentPrefixOf]) for the duration of one comment/uncomment operation. */
class AlpacaCommentState(
  val linePrefix: String?,
) : CommenterDataHolder()

/**
 * Toggles line comments (Ctrl+/) for whichever grammar the current file resolves to, inferring the
 * comment prefix the same way [com.halotukozak.alpaca.plugin.lexer.AlpacaSyntaxHighlighter] infers
 * highlighting: from an `ignored` rule's regex *shape* (`#.*`, `//.*`, ...), since Alpaca's export
 * has no explicit "this rule is a line comment" flag. Block comments aren't supported -- there's no
 * comparably reliable shape to recognize a block-comment pair from a single regex.
 *
 * Implements [SelfManagingCommenter] (not just the plain [Commenter]) because the plain interface's
 * methods take no parameters, so a fixed prefix would have to be picked once for the whole shared
 * [com.halotukozak.alpaca.plugin.lexer.AlpacaLanguage] -- [SelfManagingCommenter] instead resolves
 * it fresh per [PsiFile] via [createLineCommentingState].
 */
class AlpacaCommenter : Commenter, SelfManagingCommenter<AlpacaCommentState> {
  // CommentByLineCommentHandler gates the whole action on this being non-null *before* it ever
  // consults SelfManagingCommenter -- verified directly, since a null here silently disabled Ctrl+/
  // even though createLineCommentingState/commentLine below did correctly resolve and use the real
  // per-file prefix once unlocked. The value itself is never shown or used; only its nullness matters.
  override fun getLineCommentPrefix(): String = "#"

  override fun getBlockCommentPrefix(): String? = null

  override fun getBlockCommentSuffix(): String? = null

  override fun getCommentedBlockCommentPrefix(): String? = null

  override fun getCommentedBlockCommentSuffix(): String? = null

  override fun createLineCommentingState(
    startLine: Int,
    endLine: Int,
    document: Document,
    file: PsiFile,
  ): AlpacaCommentState {
    val virtualFile = file.virtualFile ?: return AlpacaCommentState(null)
    val tokens = resolveGrammarForFile(file.project, virtualFile)?.tokens ?: return AlpacaCommentState(null)
    val prefix = tokens.filter { it.ignored }.firstNotNullOfOrNull { lineCommentPrefixOf(it.pattern) }
    return AlpacaCommentState(prefix)
  }

  override fun createBlockCommentingState(
    startLine: Int,
    endLine: Int,
    document: Document,
    file: PsiFile,
  ): AlpacaCommentState = AlpacaCommentState(null)

  override fun getCommentPrefix(
    line: Int,
    document: Document,
    data: AlpacaCommentState,
  ): String? = data.linePrefix

  override fun isLineCommented(
    line: Int,
    offset: Int,
    document: Document,
    data: AlpacaCommentState,
  ): Boolean {
    val prefix = data.linePrefix ?: return false
    return document.charsSequence.startsWith(prefix, offset)
  }

  override fun commentLine(
    line: Int,
    offset: Int,
    document: Document,
    data: AlpacaCommentState,
  ) {
    val prefix = data.linePrefix ?: return
    document.insertString(offset, "$prefix ")
  }

  override fun uncommentLine(
    line: Int,
    offset: Int,
    document: Document,
    data: AlpacaCommentState,
  ) {
    val prefix = data.linePrefix ?: return
    if (!document.charsSequence.startsWith(prefix, offset)) return
    var end = offset + prefix.length
    if (end < document.textLength && document.charsSequence[end] == ' ') end++
    document.deleteString(offset, end)
  }

  override fun getBlockCommentPrefix(
    line: Int,
    document: Document,
    data: AlpacaCommentState,
  ): String? = null

  override fun getBlockCommentSuffix(
    line: Int,
    document: Document,
    data: AlpacaCommentState,
  ): String? = null

  override fun getBlockCommentRange(
    selectionStart: Int,
    selectionEnd: Int,
    document: Document,
    data: AlpacaCommentState,
  ): TextRange? = null

  override fun insertBlockComment(
    startOffset: Int,
    endOffset: Int,
    document: Document,
    data: AlpacaCommentState,
  ): TextRange? = null

  override fun uncommentBlockComment(
    startOffset: Int,
    endOffset: Int,
    document: Document,
    data: AlpacaCommentState,
  ) = Unit
}
