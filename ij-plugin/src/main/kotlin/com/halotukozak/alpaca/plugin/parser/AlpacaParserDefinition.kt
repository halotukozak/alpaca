package com.halotukozak.alpaca.plugin.parser

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.EmptyLexer
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

/**
 * Registered once for the shared [com.halotukozak.alpaca.plugin.lexer.AlpacaLanguage].
 * [createLexer] and [createParser] are effectively unused stubs: real parsing happens in
 * [AlpacaFileElementType.parseContents], which can see the file being parsed and so knows which
 * grammar applies. `createLexer`/`createParser` only get a [Project]. They stay harmless no-ops
 * for any platform code that might invoke them directly.
 */
class AlpacaParserDefinition : ParserDefinition {
  override fun createLexer(project: Project?): Lexer = EmptyLexer()

  override fun createParser(project: Project?): PsiParser =
    PsiParser { root, builder ->
      val fileMarker = builder.mark()
      while (!builder.eof()) builder.advanceLexer()
      fileMarker.done(root)
      builder.treeBuilt
    }

  override fun getFileNodeType(): IFileElementType = AlpacaFileElementType

  // Alpaca's export doesn't distinguish "comment"/"string literal" rules from any other ignored
  // or ordinary token, so there is nothing meaningful to report here yet.
  override fun getCommentTokens(): TokenSet = TokenSet.EMPTY

  override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY

  override fun createElement(node: ASTNode): PsiElement = ASTWrapperPsiElement(node)

  override fun createFile(viewProvider: FileViewProvider): PsiFile = AlpacaPsiFile(viewProvider)
}
