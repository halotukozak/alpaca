package com.halotukozak.alpaca.plugin.lexer

import com.halotukozak.alpaca.plugin.grammar.TokenSpec
import com.halotukozak.alpaca.plugin.grammar.literalTextOf
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType

/**
 * Colors one grammar's tokens. Alpaca's grammar export has no per-token
 * semantic category (keyword vs string vs number), so this infers a rough
 * one from each rule's regex *pattern* shape instead: a single escaped
 * punctuation character is an operator or bracket, a digit class (`0-9`/`\d`)
 * is a number, a pattern containing a quote character is a string, a bare
 * run of letters is a keyword. A pattern the shape doesn't clearly indicate
 * (e.g. an identifier class like `[a-z]+`) is left in the default text
 * color, same as real identifiers in any language -- it's not colored
 * arbitrarily just to look busy. Rules marked `ignored` (typically
 * whitespace/comments) always get the muted comment-like color regardless
 * of pattern shape.
 *
 * Registered once for the shared [AlpacaLanguage] (see plugin.xml) and
 * reused for every grammar; [grammarId] only keys the token type cache (see
 * [AlpacaTokenTypes]) so different grammars' same-named rules don't collide.
 */
class AlpacaSyntaxHighlighter(private val grammarId: String, private val specs: List<TokenSpec>) : SyntaxHighlighterBase() {
  private val keysByType: Map<IElementType, Array<TextAttributesKey>> =
    specs
      .mapNotNull { spec -> classify(spec)?.let { key -> AlpacaTokenTypes.forName(grammarId, spec.name) to arrayOf(key) } }
      .toMap()

  override fun getHighlightingLexer(): Lexer = AlpacaLexer(grammarId, specs)

  override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> =
    when (tokenType) {
      null -> EMPTY_KEYS
      ALPACA_BAD_CHARACTER -> BAD_CHARACTER_KEYS
      else -> keysByType[tokenType] ?: EMPTY_KEYS
    }

  companion object {
    private val OPERATOR_CHARS = setOf('+', '-', '*', '/', '%', '<', '>', '=', '!', '&', '|', '^', '~', ':')
    private val IDENTIFIER_LIKE = Regex("[A-Za-z]+")

    private fun classify(spec: TokenSpec): TextAttributesKey? {
      if (spec.ignored) return IGNORED

      literalTextOf(spec.pattern)?.singleOrNull()?.let { char ->
        return when (char) {
          '(', ')' -> PARENTHESES
          '[', ']' -> BRACKETS
          '{', '}' -> BRACES
          '.' -> DOT
          ',' -> COMMA
          ';' -> SEMICOLON
          in OPERATOR_CHARS -> OPERATION_SIGN
          else -> null
        }
      }

      return when {
        spec.pattern.contains("0-9") || spec.pattern.contains("\\d") -> NUMBER
        spec.pattern.any { it == '"' || it == '\'' } -> STRING
        IDENTIFIER_LIKE.matches(spec.pattern) -> KEYWORD
        else -> null
      }
    }

    val IGNORED: TextAttributesKey = createTextAttributesKey("ALPACA_IGNORED", DefaultLanguageHighlighterColors.LINE_COMMENT)
    val KEYWORD: TextAttributesKey = createTextAttributesKey("ALPACA_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
    val STRING: TextAttributesKey = createTextAttributesKey("ALPACA_STRING", DefaultLanguageHighlighterColors.STRING)
    val NUMBER: TextAttributesKey = createTextAttributesKey("ALPACA_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
    val OPERATION_SIGN: TextAttributesKey =
      createTextAttributesKey("ALPACA_OPERATION_SIGN", DefaultLanguageHighlighterColors.OPERATION_SIGN)
    val PARENTHESES: TextAttributesKey = createTextAttributesKey("ALPACA_PARENTHESES", DefaultLanguageHighlighterColors.PARENTHESES)
    val BRACKETS: TextAttributesKey = createTextAttributesKey("ALPACA_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS)
    val BRACES: TextAttributesKey = createTextAttributesKey("ALPACA_BRACES", DefaultLanguageHighlighterColors.BRACES)
    val DOT: TextAttributesKey = createTextAttributesKey("ALPACA_DOT", DefaultLanguageHighlighterColors.DOT)
    val COMMA: TextAttributesKey = createTextAttributesKey("ALPACA_COMMA", DefaultLanguageHighlighterColors.COMMA)
    val SEMICOLON: TextAttributesKey = createTextAttributesKey("ALPACA_SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON)
    val BAD_CHARACTER: TextAttributesKey = createTextAttributesKey("ALPACA_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER)

    private val BAD_CHARACTER_KEYS = arrayOf(BAD_CHARACTER)
    private val EMPTY_KEYS = emptyArray<TextAttributesKey>()
  }
}
