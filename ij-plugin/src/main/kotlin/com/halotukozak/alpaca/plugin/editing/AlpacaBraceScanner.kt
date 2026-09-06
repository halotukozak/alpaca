package com.halotukozak.alpaca.plugin.editing

import com.halotukozak.alpaca.plugin.grammar.BracketKind
import com.halotukozak.alpaca.plugin.grammar.BracketRole
import com.halotukozak.alpaca.plugin.grammar.TokenSpec
import com.halotukozak.alpaca.plugin.grammar.bracketRoleOf
import com.halotukozak.alpaca.plugin.lexer.ALPACA_BAD_CHARACTER
import com.halotukozak.alpaca.plugin.lexer.AlpacaLexer
import com.halotukozak.alpaca.plugin.lexer.AlpacaTokenTypes
import com.intellij.openapi.util.TextRange

/**
 * Grammar-agnostic brace matching: lexes text with a grammar's own [AlpacaLexer], keeps the tokens
 * whose pattern is a single `(` `)` `[` `]` `{` `}` (so brackets inside string/comment tokens are
 * ignored), and pairs them by nesting. Pure — no PSI or platform services — so it is unit-testable
 * and [AlpacaBraceHighlighter] is a thin wrapper over it.
 */
object AlpacaBraceScanner {
    private class Bracket(
        val range: TextRange,
        val role: BracketRole,
    )

    /**
     * The opening/closing brace pair (opening first) touching [caretOffset] — the bracket that
     * starts at the caret, or failing that the one that ends at it — or null if there is no bracket
     * there or it has no well-nested match.
     */
    fun matchAt(
        text: CharSequence,
        lexerId: String,
        tokenSpecs: List<TokenSpec>,
        caretOffset: Int,
    ): Pair<TextRange, TextRange>? {
        val brackets = collectBrackets(text, lexerId, tokenSpecs)
        if (brackets.isEmpty()) return null

        val targetIndex =
            brackets
                .indexOfFirst { it.range.startOffset == caretOffset }
                .takeIf { it >= 0 }
                ?: brackets.indexOfFirst { it.range.endOffset == caretOffset }.takeIf { it >= 0 }
                ?: return null

        val target = brackets[targetIndex]
        val matchIndex = if (target.role.opening) matchForward(brackets, targetIndex) else matchBackward(brackets, targetIndex)
        val match = matchIndex?.let(brackets::get) ?: return null

        return if (target.role.opening) target.range to match.range else match.range to target.range
    }

    private fun collectBrackets(
        text: CharSequence,
        lexerId: String,
        tokenSpecs: List<TokenSpec>,
    ): List<Bracket> {
        val roleByType =
            tokenSpecs
                .mapNotNull { spec -> bracketRoleOf(spec.pattern)?.let { AlpacaTokenTypes.forName(lexerId, spec.name) to it } }
                .toMap()
        if (roleByType.isEmpty()) return emptyList()

        val lexer = AlpacaLexer(lexerId, tokenSpecs)
        lexer.start(text, 0, text.length, 0)

        val brackets = mutableListOf<Bracket>()
        while (true) {
            val type = lexer.tokenType ?: break
            if (type != ALPACA_BAD_CHARACTER) {
                roleByType[type]?.let { role -> brackets += Bracket(TextRange(lexer.tokenStart, lexer.tokenEnd), role) }
            }
            lexer.advance()
        }
        return brackets
    }

    /** Index of the closer that balances the opener at [openerIndex], or null if the run isn't well nested. */
    private fun matchForward(
        brackets: List<Bracket>,
        openerIndex: Int,
    ): Int? {
        val stack = ArrayDeque<BracketKind>()
        stack.addLast(brackets[openerIndex].role.kind)
        for (i in openerIndex + 1 until brackets.size) {
            val role = brackets[i].role
            if (role.opening) {
                stack.addLast(role.kind)
            } else {
                if (stack.removeLastOrNull() != role.kind) return null
                if (stack.isEmpty()) return i
            }
        }
        return null
    }

    /** Mirror of [matchForward] for a closer at [closerIndex]. */
    private fun matchBackward(
        brackets: List<Bracket>,
        closerIndex: Int,
    ): Int? {
        val stack = ArrayDeque<BracketKind>()
        stack.addLast(brackets[closerIndex].role.kind)
        for (i in closerIndex - 1 downTo 0) {
            val role = brackets[i].role
            if (!role.opening) {
                stack.addLast(role.kind)
            } else {
                if (stack.removeLastOrNull() != role.kind) return null
                if (stack.isEmpty()) return i
            }
        }
        return null
    }
}
