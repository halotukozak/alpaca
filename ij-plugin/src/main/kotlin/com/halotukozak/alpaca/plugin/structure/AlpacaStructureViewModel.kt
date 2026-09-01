package com.halotukozak.alpaca.plugin.structure

import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.Sorter
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile

/**
 * Wraps the whole file's PSI tree as the structure view model. [getSorters] only wires up the
 * platform's alphabetical-sort toggle; the tree's default order still mirrors source order, which
 * matters here since sibling nodes of the same nonterminal are otherwise indistinguishable.
 */
class AlpacaStructureViewModel(
    psiFile: PsiFile,
    editor: Editor?,
) : StructureViewModelBase(psiFile, editor, AlpacaStructureViewElement(psiFile)),
    StructureViewModel.ElementInfoProvider {
    override fun getSorters(): Array<Sorter> = arrayOf(Sorter.ALPHA_SORTER)

    override fun isAlwaysShowsPlus(element: StructureViewTreeElement): Boolean = false

    override fun isAlwaysLeaf(element: StructureViewTreeElement): Boolean = element.children.isEmpty()
}
