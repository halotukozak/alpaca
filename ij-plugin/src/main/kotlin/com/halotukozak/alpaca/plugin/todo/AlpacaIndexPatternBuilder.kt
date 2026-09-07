package com.halotukozak.alpaca.plugin.todo

import com.halotukozak.alpaca.plugin.grammar.lineCommentPrefixOf
import com.halotukozak.alpaca.plugin.grammar.resolveGrammarForFile
import com.halotukozak.alpaca.plugin.lexer.AlpacaLanguage
import com.halotukozak.alpaca.plugin.lexer.AlpacaLexer
import com.halotukozak.alpaca.plugin.lexer.AlpacaTokenTypes
import com.intellij.lexer.Lexer
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.search.IndexPatternBuilder
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import java.util.concurrent.ConcurrentHashMap

/**
 * Feeds `TODO`/`FIXME` (and any custom *Settings | Editor | TODO* pattern) scanning for
 * Alpaca-defined languages, so those markers show up in the TODO tool window and the commit
 * dialog's check.
 *
 * A "comment" here is an `ignored` token rule whose pattern has the `prefix.*` line-comment shape
 * ([lineCommentPrefixOf]) -- the same rule [com.halotukozak.alpaca.plugin.commenter.AlpacaCommenter]
 * uses, so an `ignored` whitespace rule like `[ \t\r\n]+` is not mistaken for a comment. The start
 * delta strips the `#` / `//` / ... prefix so a marker still counts when written tight against it
 * (`#TODO`).
 */
class AlpacaIndexPatternBuilder : IndexPatternBuilder {
    // IElementType instances are per-(grammarId, ruleName) singletons (AlpacaTokenTypes), so this
    // app-level map stays correct across grammars. Populated in getCommentTokenSet, which has the
    // file; read back in the fileless getCommentStartDelta the platform calls per token.
    private val prefixByCommentType = ConcurrentHashMap<IElementType, String>()

    override fun getIndexingLexer(file: PsiFile): Lexer? {
        val resolved = resolve(file) ?: return null
        return AlpacaLexer(resolved.lexerId, resolved.tokens)
    }

    override fun getCommentTokenSet(file: PsiFile): TokenSet? {
        val resolved = resolve(file) ?: return null
        val commentTypes =
            resolved.tokens.mapNotNull { spec ->
                if (!spec.ignored) return@mapNotNull null
                val prefix = lineCommentPrefixOf(spec.pattern) ?: return@mapNotNull null
                AlpacaTokenTypes.forName(resolved.lexerId, spec.name).also { prefixByCommentType[it] = prefix }
            }
        return if (commentTypes.isEmpty()) null else TokenSet.create(*commentTypes.toTypedArray())
    }

    override fun getCommentStartDelta(tokenType: IElementType): Int = prefixByCommentType[tokenType]?.length ?: 0

    override fun getCommentStartDelta(
        tokenType: IElementType,
        tokenText: CharSequence,
    ): Int {
        val prefix = prefixByCommentType[tokenType] ?: return 0
        return if (tokenText.startsWith(prefix)) prefix.length else 0
    }

    override fun getCommentEndDelta(tokenType: IElementType): Int = 0

    private fun resolve(file: PsiFile) =
        if (file.language != AlpacaLanguage) {
            null
        } else {
            (file.virtualFile ?: file.originalFile.virtualFile)?.let { resolveGrammarForFile(file.project, it) }
        }
}
