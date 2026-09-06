package com.halotukozak.alpaca.plugin.lexer

import com.halotukozak.alpaca.plugin.grammar.TokenSpec
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import javax.swing.Icon

/**
 * The **Settings | Editor | Color Scheme | Alpaca** page.
 *
 * Alpaca's grammar export carries no per-token semantic category, so [AlpacaSyntaxHighlighter]
 * infers a fixed handful from each rule's regex shape. This page names those inferred categories
 * and lets them be recoloured; without it the [TextAttributesKey]s the highlighter uses never
 * appear in the IDE's colour settings at all.
 *
 * The preview is lexed by a real [AlpacaSyntaxHighlighter] over [DEMO_TOKENS], a small built-in
 * grammar picked so that every category in [DESCRIPTORS] shows up in [DEMO_TEXT].
 */
class AlpacaColorSettingsPage : ColorSettingsPage {
    override fun getDisplayName(): String = "Alpaca"

    override fun getIcon(): Icon? = null

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = DESCRIPTORS

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getHighlighter(): SyntaxHighlighter = AlpacaSyntaxHighlighter(DEMO_GRAMMAR_ID, DEMO_TOKENS)

    override fun getDemoText(): String = DEMO_TEXT

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? = null

    companion object {
        /** Distinct from any real grammar id so [AlpacaTokenTypes] doesn't share token instances with one. */
        const val DEMO_GRAMMAR_ID: String = "__alpaca_color_settings_demo__"

        private val DESCRIPTORS =
            arrayOf(
                AttributesDescriptor("Keyword", AlpacaSyntaxHighlighter.KEYWORD),
                AttributesDescriptor("Number", AlpacaSyntaxHighlighter.NUMBER),
                AttributesDescriptor("String", AlpacaSyntaxHighlighter.STRING),
                AttributesDescriptor("Operator", AlpacaSyntaxHighlighter.OPERATION_SIGN),
                AttributesDescriptor("Parentheses", AlpacaSyntaxHighlighter.PARENTHESES),
                AttributesDescriptor("Brackets", AlpacaSyntaxHighlighter.BRACKETS),
                AttributesDescriptor("Braces", AlpacaSyntaxHighlighter.BRACES),
                AttributesDescriptor("Dot", AlpacaSyntaxHighlighter.DOT),
                AttributesDescriptor("Comma", AlpacaSyntaxHighlighter.COMMA),
                AttributesDescriptor("Semicolon", AlpacaSyntaxHighlighter.SEMICOLON),
                AttributesDescriptor("Ignored (comments, whitespace)", AlpacaSyntaxHighlighter.IGNORED),
                AttributesDescriptor("Bad character", AlpacaSyntaxHighlighter.BAD_CHARACTER),
            )

        /** Every rule here is shaped so [AlpacaSyntaxHighlighter] classifies it into exactly one
         *  [DESCRIPTORS] category; keyword rules precede the identifier rule so a bare word lexes as
         *  a keyword on a length tie. */
        val DEMO_TOKENS: List<TokenSpec> =
            listOf(
                TokenSpec("whitespace", "\\s+", ignored = true),
                TokenSpec("comment", "#.*", ignored = true),
                TokenSpec("kw_sin", "sin", ignored = false),
                TokenSpec("kw_let", "let", ignored = false),
                TokenSpec("identifier", "[a-z]+", ignored = false),
                TokenSpec("number", "[0-9]+", ignored = false),
                TokenSpec("string", "\"[^\"]*\"", ignored = false),
                TokenSpec("op_plus", "\\+", ignored = false),
                TokenSpec("op_minus", "-", ignored = false),
                TokenSpec("op_star", "\\*", ignored = false),
                TokenSpec("op_eq", "=", ignored = false),
                TokenSpec("lparen", "\\(", ignored = false),
                TokenSpec("rparen", "\\)", ignored = false),
                TokenSpec("lbracket", "\\[", ignored = false),
                TokenSpec("rbracket", "\\]", ignored = false),
                TokenSpec("lbrace", "\\{", ignored = false),
                TokenSpec("rbrace", "\\}", ignored = false),
                TokenSpec("dot", "\\.", ignored = false),
                TokenSpec("comma", ",", ignored = false),
                TokenSpec("semicolon", ";", ignored = false),
            )

        private val DEMO_TEXT =
            """
            # Alpaca colours the tokens whose regex shape is unambiguous.
            let result = sin ( 2 + 3 ) * 42 - 1 ;
            greeting = "hello, world" ;
            config { items [ 0 ] . field , next }
            @ is matched by no rule
            """.trimIndent()
    }
}
