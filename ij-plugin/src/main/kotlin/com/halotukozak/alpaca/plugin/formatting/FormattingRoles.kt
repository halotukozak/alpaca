package com.halotukozak.alpaca.plugin.formatting

import com.halotukozak.alpaca.plugin.grammar.BracketRole
import com.halotukozak.alpaca.plugin.grammar.TokenSpec
import com.halotukozak.alpaca.plugin.grammar.bracketRoleOf
import com.halotukozak.alpaca.plugin.grammar.literalTextOf
import com.halotukozak.alpaca.plugin.lexer.AlpacaTokenTypes
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

/**
 * Classifies one grammar's tokens by shape, the same way
 * [com.halotukozak.alpaca.plugin.editing.AlpacaBraceScanner] and
 * [com.halotukozak.alpaca.plugin.lexer.AlpacaSyntaxHighlighter] do: a rule whose pattern is a
 * fixed `(`/`)`/`[`/`]`/`{`/`}`, `,`, `;`, or `.` gets a role here; anything else (including an
 * identifier-shaped keyword) is unclassified and only ever gets [AlpacaBlock]'s generic default
 * spacing and indent.
 */
class FormattingRoles private constructor(
    private val bracketRoles: Map<IElementType, BracketRole>,
    val openers: TokenSet,
    val closers: TokenSet,
    val commas: TokenSet,
    val semicolons: TokenSet,
    val dots: TokenSet,
) {
    fun roleOf(type: IElementType): BracketRole? = bracketRoles[type]

    companion object {
        fun of(
            grammarId: String,
            tokens: List<TokenSpec>,
        ): FormattingRoles {
            val bracketRoles = HashMap<IElementType, BracketRole>()
            val commas = mutableListOf<IElementType>()
            val semicolons = mutableListOf<IElementType>()
            val dots = mutableListOf<IElementType>()
            for (spec in tokens) {
                val type = AlpacaTokenTypes.forName(grammarId, spec.name)
                bracketRoleOf(spec.pattern)?.let { bracketRoles[type] = it }
                when (literalTextOf(spec.pattern)) {
                    "," -> commas += type
                    ";" -> semicolons += type
                    "." -> dots += type
                }
            }
            val openers = bracketRoles.filterValues { it.opening }.keys
            val closers = bracketRoles.filterValues { !it.opening }.keys
            return FormattingRoles(
                bracketRoles = bracketRoles,
                openers = TokenSet.create(*openers.toTypedArray()),
                closers = TokenSet.create(*closers.toTypedArray()),
                commas = TokenSet.create(*commas.toTypedArray()),
                semicolons = TokenSet.create(*semicolons.toTypedArray()),
                dots = TokenSet.create(*dots.toTypedArray()),
            )
        }
    }
}
