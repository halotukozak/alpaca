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

/** One of the three bracket shapes a single-character token can denote. */
enum class BracketKind { PARENTHESIS, BRACKET, BRACE }

/** A bracket token's [kind] and whether it is the opening (`(` `[` `{`) or closing (`)` `]` `}`) side. */
data class BracketRole(
    val kind: BracketKind,
    val opening: Boolean,
)

/** The [BracketRole] [pattern] denotes if it matches exactly one of `( ) [ ] { }` (bare or
 *  escaped), or null for anything else. */
fun bracketRoleOf(pattern: String): BracketRole? =
    when (literalTextOf(pattern)?.singleOrNull()) {
        '(' -> BracketRole(BracketKind.PARENTHESIS, opening = true)
        ')' -> BracketRole(BracketKind.PARENTHESIS, opening = false)
        '[' -> BracketRole(BracketKind.BRACKET, opening = true)
        ']' -> BracketRole(BracketKind.BRACKET, opening = false)
        '{' -> BracketRole(BracketKind.BRACE, opening = true)
        '}' -> BracketRole(BracketKind.BRACE, opening = false)
        else -> null
    }

/**
 * The quote character a string-literal rule is delimited by, inferred the same way
 * [com.halotukozak.alpaca.plugin.lexer.AlpacaSyntaxHighlighter] infers the string color: the first
 * `"` or `'` that appears literally in [pattern] (`"[^"]*"`, `'(\\.|[^'])*'`). Null if neither does.
 */
fun stringQuoteOf(pattern: String): Char? = pattern.firstOrNull { it == '"' || it == '\'' }

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
