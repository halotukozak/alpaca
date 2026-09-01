package com.halotukozak.alpaca.plugin.structure

import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.PsiNavigateUtil
import javax.swing.Icon

/** The Alpaca logo (see `docs/_assets/images/logo.png`), used for every node since the tree is
 *  grammar-agnostic -- there is no per-nonterminal semantic to pick a more specific icon from. */
private val NODE_ICON: Icon = IconLoader.getIcon("/icons/alpacaNode.png", AlpacaStructureViewElement::class.java.classLoader)

/**
 * One node in the structure view, wrapping a composite [PsiElement] (the file itself, at the
 * root). Entirely grammar-agnostic: every non-leaf node the parser produced -- named after its
 * own nonterminal -- is shown, since [PsiElement.getChildren] already only returns composite
 * children (leaf tokens are filtered out by the platform itself).
 */
class AlpacaStructureViewElement(
  private val element: PsiElement,
) : StructureViewTreeElement, SortableTreeElement {
  override fun getValue(): Any = element

  override fun getAlphaSortKey(): String = presentableName()

  override fun getPresentation(): ItemPresentation =
    object : ItemPresentation {
      override fun getPresentableText(): String = presentableName()

      override fun getIcon(unused: Boolean): Icon = NODE_ICON
    }

  override fun getChildren(): Array<TreeElement> = element.children.map { AlpacaStructureViewElement(it) }.toTypedArray()

  override fun navigate(requestFocus: Boolean) = PsiNavigateUtil.navigate(element, requestFocus)

  override fun canNavigate(): Boolean = element.isValid && element.containingFile?.virtualFile != null

  override fun canNavigateToSource(): Boolean = canNavigate()

  /** e.g. `Expr: 1 + 2 * 3` -- the nonterminal's name plus a snippet of its own source text, so
   *  nodes of the same grammar rule (there are often many, e.g. every wrapped sub-expression)
   *  stay distinguishable in the tree. */
  private fun presentableName(): String {
    if (element is PsiFile) return element.name

    val typeName = element.node.elementType.toString()
    val snippet = element.text.replace('\n', ' ').trim().let { if (it.length > 40) it.take(40) + "…" else it }
    return "$typeName: $snippet"
  }
}
