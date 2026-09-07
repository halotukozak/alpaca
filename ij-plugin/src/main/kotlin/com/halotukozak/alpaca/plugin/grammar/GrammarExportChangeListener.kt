package com.halotukozak.alpaca.plugin.grammar

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.FileContentUtil

/**
 * Drops [GrammarService]'s cache and reparses open files when a grammar export file changes on disk
 * (a rebuild with `ALPACA_GRAMMAR_EXPORT_DIR` set, typically), so an edited grammar takes effect
 * without reopening the project.
 *
 * Registered per project in `plugin.xml` (`<projectListeners>`). VFS events are delivered on the
 * EDT inside a write action, so the handler only flips the cache (cheap) and schedules the reparse
 * for later.
 */
class GrammarExportChangeListener(
    private val project: Project,
) : BulkFileListener {
    override fun after(events: List<VFileEvent>) {
        val service = GrammarService.getInstance(project)
        if (events.none { service.isUnderExportDirectory(it.path) }) return

        service.invalidate()
        ApplicationManager.getApplication().invokeLater({
            if (!project.isDisposed) FileContentUtil.reparseFiles(project, emptyList(), true)
        }, project.disposed)
    }
}
