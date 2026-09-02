package com.halotukozak.alpaca.plugin.grammar

private val REGEX_SPECIAL = "\\.^$|?*+()[]{}".toSet()

/**
 * The exact literal text [pattern] matches, if it denotes nothing but a fixed sequence of
 * characters (each either bare or backslash-escaped) with no regex classes, quantifiers, or
 * groups: `sin`, `\+`, `\*\*`. Returns null for anything else (`[a-z]+`, `\d+`, `0-9`, ...), since
 * those have no single fixed spelling.
 */
fun literalTextOf(pattern: String): String? {
    val text = StringBuilder()
    var escaped = false
    for ((i, c) in pattern.withIndex()) {
        when {
            escaped -> {
                text.append(c)
                escaped = false
            }
            c == '\\' -> {
                if (i == pattern.lastIndex) return null
                escaped = true
            }
            c in REGEX_SPECIAL -> return null
            else -> text.append(c)
        }
    }
    return text.toString()
}

/**
 * The fixed prefix [pattern] denotes for "prefix, then anything to end of line" (`#.*`, `//.*`,
 * ...), the shape Alpaca grammars typically use for line comments among their `ignored` rules.
 * Returns null for anything else.
 */
fun lineCommentPrefixOf(pattern: String): String? =
    if (!pattern.endsWith(".*")) {
        null
    } else {
        literalTextOf(pattern.dropLast(2))?.takeIf { it.isNotEmpty() }
    }
