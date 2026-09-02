package com.halotukozak.alpaca.plugin.parser

import com.halotukozak.alpaca.plugin.lexer.AlpacaLanguage
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider

/**
 * The PSI file for every Alpaca-defined language. Generic, since which grammar applies is resolved
 * dynamically inside [AlpacaFileElementType], not baked into a per-grammar subclass; its
 * [getFileType][com.intellij.psi.PsiFile.getFileType] still correctly reports the per-grammar
 * [com.halotukozak.alpaca.plugin.lexer.AlpacaFileType], inherited from the view provider.
 */
class AlpacaPsiFile(
  viewProvider: FileViewProvider,
) : PsiFileBase(viewProvider, AlpacaLanguage) {
  override fun getFileType(): FileType = viewProvider.fileType

  override fun toString(): String = "Alpaca File"
}
