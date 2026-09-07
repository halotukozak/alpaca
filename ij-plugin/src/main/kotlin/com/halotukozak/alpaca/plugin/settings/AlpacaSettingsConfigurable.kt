package com.halotukozak.alpaca.plugin.settings

import com.halotukozak.alpaca.plugin.grammar.GrammarService
import com.halotukozak.alpaca.plugin.lexer.AlpacaFileTypeRegistrar
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.table.JBTable
import com.intellij.util.FileContentUtil
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import javax.swing.JComponent

class AlpacaSettingsConfigurable(
    private val project: Project,
) : Configurable {
    private val settings = AlpacaSettingsState.getInstance(project)
    private val exportDirectoryField = JBTextField()
    private val tableModel = ListTableModel<GrammarAssociation>(EXTENSION_COLUMN, LEXER_GRAMMAR_ID_COLUMN, PARSER_GRAMMAR_ID_COLUMN)
    private val table = JBTable(tableModel)

    override fun getDisplayName(): String = "Alpaca"

    override fun createComponent(): JComponent {
        table.columnModel.getColumn(0).preferredWidth = 100
        table.columnModel.getColumn(1).preferredWidth = 260
        table.columnModel.getColumn(2).preferredWidth = 260

        val tablePanel =
            ToolbarDecorator
                .createDecorator(table)
                .setAddAction { tableModel.addRow(GrammarAssociation()) }
                .createPanel()

        return panel {
            row("Grammar export directory:") {
                cell(exportDirectoryField)
                    .comment(
                        "Directory Alpaca's compile-time macros write exported grammars to. " +
                            "Falls back to the ALPACA_GRAMMAR_EXPORT_DIR environment variable when left empty.",
                    )
            }
            row("Language mappings:") {
                cell(tablePanel)
                    .align(Align.FILL)
                    .comment(
                        "Files with the given extension are highlighted using the lexer grammar's tokens. " +
                            "Leave the parser grammar id empty to skip real parsing and keep plain token highlighting.",
                    )
            }.resizableRow()
        }
    }

    override fun isModified(): Boolean = exportDirectoryField.text != settings.exportDirectory || tableModel.items != settings.associations

    override fun apply() {
        settings.exportDirectory = exportDirectoryField.text
        settings.associations = tableModel.items.filter { it.extension.isNotBlank() && it.lexerGrammarId.isNotBlank() }.toMutableList()

        runWriteAction {
            settings.associations.forEach { AlpacaFileTypeRegistrar.ensureRegistered(it.extension, it.lexerGrammarId) }
        }

        // The directory or the mappings may have changed: drop the cached scan, re-point the file
        // watcher, and reparse open files so the new grammar takes effect immediately.
        val grammarService = GrammarService.getInstance(project)
        grammarService.invalidate()
        grammarService.syncWatchedRoots()
        FileContentUtil.reparseFiles(project, emptyList(), true)
    }

    override fun reset() {
        exportDirectoryField.text = settings.exportDirectory
        tableModel.items = settings.associations.map { it.copy() }
    }

    companion object {
        private val EXTENSION_COLUMN =
            object : ColumnInfo<GrammarAssociation, String>("Extension") {
                override fun valueOf(item: GrammarAssociation): String = item.extension

                override fun setValue(
                    item: GrammarAssociation,
                    value: String,
                ) {
                    item.extension = value
                }

                override fun isCellEditable(item: GrammarAssociation): Boolean = true
            }

        private val LEXER_GRAMMAR_ID_COLUMN =
            object : ColumnInfo<GrammarAssociation, String>("Lexer grammar id") {
                override fun valueOf(item: GrammarAssociation): String = item.lexerGrammarId

                override fun setValue(
                    item: GrammarAssociation,
                    value: String,
                ) {
                    item.lexerGrammarId = value
                }

                override fun isCellEditable(item: GrammarAssociation): Boolean = true
            }

        private val PARSER_GRAMMAR_ID_COLUMN =
            object : ColumnInfo<GrammarAssociation, String>("Parser grammar id (optional)") {
                override fun valueOf(item: GrammarAssociation): String = item.parserGrammarId

                override fun setValue(
                    item: GrammarAssociation,
                    value: String,
                ) {
                    item.parserGrammarId = value
                }

                override fun isCellEditable(item: GrammarAssociation): Boolean = true
            }
    }
}
