package com.halotukozak.alpaca.plugin.grammar

import com.halotukozak.alpaca.plugin.settings.AlpacaSettingsState
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.nio.file.Files
import java.nio.file.Path

class GrammarExportChangeListenerTest : BasePlatformTestCase() {
    private lateinit var exportDir: Path

    override fun setUp() {
        super.setUp()
        exportDir = Files.createTempDirectory("grammar-export-listener-test")
        AlpacaSettingsState.getInstance(project).exportDirectory = exportDir.toString()
    }

    override fun tearDown() {
        try {
            AlpacaSettingsState.getInstance(project).exportDirectory = ""
            Files.walk(exportDir).sorted(Comparator.reverseOrder()).forEach(Files::delete)
        } finally {
            super.tearDown()
        }
    }

    private fun writeLexer(
        id: String,
        tokensJson: String,
    ) = Files.writeString(exportDir.resolve("$id.tokens.json"), tokensJson)

    private fun GrammarService.singleLexerTokenNames(): List<String> =
        exportedGrammars()
            .lexers
            .single()
            .tokens
            .map { it.name }

    private fun contentChangeEventAt(path: Path): VFileEvent {
        val virtualFile =
            LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path)
                ?: error("expected $path to exist on disk")
        val requestor = this
        return VFileContentChangeEvent(requestor, virtualFile, 0, 1)
    }

    fun `test invalidates the cache when a changed file is under the export directory`() {
        writeLexer("L@L1", versionedJson("""[{"name":"A","pattern":"a","ignored":false}]"""))
        val service = GrammarService.getInstance(project)
        assertEquals(listOf("A"), service.singleLexerTokenNames())

        // Change the file on disk behind the cache, exactly like GrammarServiceTest does, then
        // let the listener notice.
        writeLexer("L@L1", versionedJson("""[{"name":"B","pattern":"b","ignored":false}]"""))
        GrammarExportChangeListener(project).after(listOf(contentChangeEventAt(exportDir.resolve("L@L1.tokens.json"))))

        assertEquals(listOf("B"), service.singleLexerTokenNames())
    }

    fun `test leaves the cache untouched when no event is under the export directory`() {
        writeLexer("L@L1", versionedJson("""[{"name":"A","pattern":"a","ignored":false}]"""))
        val service = GrammarService.getInstance(project)
        assertEquals(listOf("A"), service.singleLexerTokenNames())

        writeLexer("L@L1", versionedJson("""[{"name":"B","pattern":"b","ignored":false}]"""))
        val elsewhere = Files.createTempFile("grammar-export-listener-test-unrelated", ".json")
        try {
            GrammarExportChangeListener(project).after(listOf(contentChangeEventAt(elsewhere)))
            assertEquals(listOf("A"), service.singleLexerTokenNames())
        } finally {
            Files.deleteIfExists(elsewhere)
        }
    }
}
