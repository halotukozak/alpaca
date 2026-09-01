package com.halotukozak.alpaca.plugin.structure

import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile

/** Registered once for the shared [com.halotukozak.alpaca.plugin.lexer.AlpacaLanguage]; the model
 *  it builds reads whichever grammar the file actually resolves to, so nothing here is per-grammar. */
class AlpacaStructureViewFactory : PsiStructureViewFactory {
  override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder =
    object : TreeBasedStructureViewBuilder() {
      override fun createStructureViewModel(editor: Editor?) = AlpacaStructureViewModel(psiFile, editor)
    }
}
