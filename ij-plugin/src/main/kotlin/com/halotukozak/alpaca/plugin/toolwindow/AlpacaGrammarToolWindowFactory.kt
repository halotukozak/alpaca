package com.halotukozak.alpaca.plugin.toolwindow

import com.halotukozak.alpaca.plugin.grammar.resolveGrammarForFile
import com.halotukozak.alpaca.plugin.icons.AlpacaIcons
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.treeStructure.Tree
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.SwingConstants
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * "Alpaca Grammar": the whole of the current file's grammar (every token, every production
 * alternative), browsable in one place -- unlike Structure View (this file's own parse tree) or
 * Quick Documentation (one node's rule at a time). Refreshes on every editor selection change to
 * always show the grammar of whichever file is now on top.
 */
class AlpacaGrammarToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow,
    ) {
        val panel = GrammarPanel(project)
        val content = ContentFactory.getInstance().createContent(panel, null, false)
        toolWindow.contentManager.addContent(content)

        project.messageBus.connect(toolWindow.disposable).subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    panel.showGrammarFor(event.newFile)
                }
            },
        )
        panel.showGrammarFor(FileEditorManager.getInstance(project).selectedFiles.firstOrNull())
    }
}

private class GrammarPanel(
    private val project: Project,
) : JPanel(BorderLayout()) {
    private val placeholder = JBLabel("Open an Alpaca-defined file to see its grammar.", SwingConstants.CENTER)
    private val tree =
        Tree().apply {
            isRootVisible = true
            cellRenderer = GrammarTreeCellRenderer()
        }

    init {
        add(placeholder, BorderLayout.CENTER)
    }

    fun showGrammarFor(file: VirtualFile?) {
        val resolved = file?.let { resolveGrammarForFile(project, it) }
        removeAll()
        if (resolved == null) {
            add(placeholder, BorderLayout.CENTER)
        } else {
            tree.model = DefaultTreeModel(toSwingTree(buildGrammarTree(resolved)))
            // Only the root: a grammar with hundreds of productions (e.g. the Scala one this
            // project's own parser targets) would otherwise dump every alternative open at once.
            tree.expandRow(0)
            add(JBScrollPane(tree), BorderLayout.CENTER)
        }
        revalidate()
        repaint()
    }

    private fun toSwingTree(node: GrammarTreeNode): DefaultMutableTreeNode =
        DefaultMutableTreeNode(node).apply {
            for (child in node.children) add(toSwingTree(child))
        }
}

/** Renders a [GrammarTreeNode]: bold for a category/nonterminal heading, the pattern or
 *  right-hand side dimmed as a secondary segment -- the same role Structure View's location
 *  string plays -- instead of one flat string. */
private class GrammarTreeCellRenderer : ColoredTreeCellRenderer() {
    override fun customizeCellRenderer(
        tree: JTree,
        value: Any?,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean,
    ) {
        val node = (value as? DefaultMutableTreeNode)?.userObject as? GrammarTreeNode ?: return
        icon = AlpacaIcons.FILE
        append(node.primaryText, if (node.bold) SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES else SimpleTextAttributes.REGULAR_ATTRIBUTES)
        node.secondaryText?.let {
            append("  $it", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        }
    }
}
