package com.halotukozak.alpaca.plugin.lexer

import com.intellij.lang.Language

/**
 * The single, shared [Language] for every language defined with Alpaca.
 *
 * IntelliJ's `Language` enforces "there should be exactly one instance of
 * each Language" (its own Javadoc) via a registry keyed by the concrete
 * *class*, not by ID -- confirmed empirically by hitting
 * `ImplementationConflictException: Language of 'class AlpacaLanguage' is
 * already registered` when constructing a second instance with a different
 * ID, and confirmed in source (`Language.java`, JetBrains/intellij-community):
 * `registeredLanguages: Map<Class<? extends Language>, Language>`. So a real
 * per-grammar `Language` instance isn't something the platform supports.
 *
 * Which grammar applies to a given file is instead resolved dynamically
 * per-file inside [AlpacaSyntaxHighlighterFactory] -- that's exactly what
 * its `(project, virtualFile)` parameters are for. [AlpacaFileType] has no
 * such one-instance restriction, so it still gets a real instance per
 * grammar (see [AlpacaFileTypes]), and token types are still kept distinct
 * per grammar by keying on the grammar id (see [AlpacaTokenTypes]).
 */
object AlpacaLanguage : Language("Alpaca")
