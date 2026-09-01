package com.halotukozak.alpaca.plugin.lexer

import com.intellij.lang.Language

/**
 * The single, shared [Language] for every language defined with Alpaca.
 *
 * IntelliJ allows only one [Language] instance per concrete class (registry keyed by class, not
 * ID); a second instance under a different ID throws `ImplementationConflictException`. A real
 * per-grammar `Language` isn't possible, so [AlpacaFileType] carries the per-grammar identity
 * instead, and token types stay distinct per grammar by keying on the grammar id ([AlpacaTokenTypes]).
 */
object AlpacaLanguage : Language("Alpaca")
