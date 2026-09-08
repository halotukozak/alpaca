package com.halotukozak.alpaca.plugin.grammar

/**
 * A human-readable label for [symbol]: its fixed spelling in single quotes when [tokens] resolves
 * it to one (`'+'`, `'('`), or its bare rule name otherwise (a nonterminal, or a terminal with no
 * single fixed spelling, e.g. `int`). Shared by every surface that spells out a production's
 * right-hand side (Quick Documentation, the grammar tool window).
 */
fun symbolLabel(
    symbol: SymbolSpec,
    tokens: List<TokenSpec>,
): String {
    if (symbol.kind != "terminal") return symbol.name
    val pattern = tokens.firstOrNull { it.name == symbol.name }?.pattern
    val literal = pattern?.let(::literalTextOf)
    return if (literal != null) "'$literal'" else symbol.name
}
