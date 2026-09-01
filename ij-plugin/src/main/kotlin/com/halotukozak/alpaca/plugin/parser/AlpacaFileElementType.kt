package com.halotukozak.alpaca.plugin.parser

import com.halotukozak.alpaca.plugin.lexer.AlpacaLanguage
import com.halotukozak.alpaca.plugin.lexer.AlpacaLexer
import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.psi.tree.IFileElementType

/**
 * The single, shared root node type for every Alpaca-defined language's file (see [AlpacaLanguage]'s
 * doc comment for why there is only one). Overrides [parseContents] directly instead of the usual
 * [com.intellij.lang.ParserDefinition.createLexer]/`createParser` pair, because those are only ever
 * given a [com.intellij.openapi.project.Project] -- no file -- and resolving which grammar applies
 * needs the file. Here, the chameleon [ASTNode] gives access to its own containing file.
 */
object AlpacaFileElementType : IFileElementType(AlpacaLanguage) {
  override fun parseContents(chameleon: ASTNode): ASTNode {
    val psi = chameleon.psi ?: error("Bad chameleon: $chameleon has no PSI")
    val project = psi.project
    val virtualFile = psi.containingFile?.originalFile?.virtualFile

    val resolved = virtualFile?.let { resolveGrammarForFile(project, it) }
    val (lexerId, tokens, parserGrammar) =
      resolved ?: ResolvedGrammar("<unresolved>", emptyList(), null)

    val lexer = AlpacaLexer(lexerId, tokens)
    val builder = PsiBuilderFactory.getInstance().createBuilder(project, chameleon, lexer, AlpacaLanguage, chameleon.chars)

    val rootMarker = builder.mark()
    if (parserGrammar != null) {
      AlpacaLrDriver.forTable(parserGrammar.table).parse(AlpacaPsiTreeBuilder(builder, lexerId, tokens))
    }
    // Safety net: the driver always consumes up to EOF on both its exit paths (accept or an
    // error right at EOF), and there's nothing to drive at all when no parser grammar is
    // configured -- either way, make sure the whole chameleon ends up under rootMarker.
    while (!builder.eof()) builder.advanceLexer()
    rootMarker.done(this)

    return builder.treeBuilt.firstChildNode
  }
}
