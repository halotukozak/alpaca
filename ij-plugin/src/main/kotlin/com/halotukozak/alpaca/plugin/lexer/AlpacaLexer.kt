package com.halotukozak.alpaca.plugin.lexer

import com.halotukozak.alpaca.plugin.grammar.TokenSpec
import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Tokenizes text against the [TokenSpec] rules exported by one grammar's Alpaca `lexer{...}`
 * macro: at every position, each rule's pattern is tried and the longest match wins, ties broken
 * by the rule's position in [specs], matching the priority Alpaca's own lexer uses.
 *
 * A zero-length match never wins, since it would leave [advance] unable to make progress. A
 * position no rule matches produces a single-character [ALPACA_BAD_CHARACTER] token instead.
 */
class AlpacaLexer(
    private val grammarId: String,
    specs: List<TokenSpec>,
) : LexerBase() {
    private val patterns: List<Pair<TokenSpec, Pattern>> = specs.map { it to Pattern.compile(it.pattern) }

    private lateinit var buffer: CharSequence
    private lateinit var matchers: List<Pair<TokenSpec, Matcher>>
    private var bufferEnd = 0
    private var tokenStart = 0
    private var tokenEnd = 0
    private var currentTokenType: IElementType? = null

    override fun start(
        buffer: CharSequence,
        startOffset: Int,
        endOffset: Int,
        initialState: Int,
    ) {
        this.buffer = buffer
        this.bufferEnd = endOffset
        this.tokenStart = startOffset
        this.matchers =
            patterns.map { (spec, pattern) ->
                val matcher = pattern.matcher(buffer)
                matcher.useTransparentBounds(true)
                matcher.useAnchoringBounds(false)
                spec to matcher
            }
        locateToken()
    }

    override fun getState(): Int = 0

    override fun getTokenType(): IElementType? = currentTokenType

    override fun getTokenStart(): Int = tokenStart

    override fun getTokenEnd(): Int = tokenEnd

    override fun advance() {
        tokenStart = tokenEnd
        locateToken()
    }

    override fun getBufferSequence(): CharSequence = buffer

    override fun getBufferEnd(): Int = bufferEnd

    private fun locateToken() {
        if (tokenStart >= bufferEnd) {
            currentTokenType = null
            tokenEnd = tokenStart
            return
        }

        val best =
            matchers
                .asSequence()
                .mapNotNull { (spec, matcher) ->
                    matcher.region(tokenStart, bufferEnd)
                    if (!matcher.lookingAt()) return@mapNotNull null
                    val length = matcher.end() - matcher.start()
                    if (length == 0) null else spec to length
                }.maxByOrNull { (_, length) -> length }

        if (best == null) {
            currentTokenType = ALPACA_BAD_CHARACTER
            tokenEnd = tokenStart + 1
        } else {
            val (spec, length) = best
            currentTokenType = AlpacaTokenTypes.forName(grammarId, spec.name)
            tokenEnd = tokenStart + length
        }
    }
}
