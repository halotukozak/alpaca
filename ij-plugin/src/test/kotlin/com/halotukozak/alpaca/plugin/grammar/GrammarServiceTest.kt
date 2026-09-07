package com.halotukozak.alpaca.plugin.grammar

import com.halotukozak.alpaca.plugin.settings.AlpacaSettingsState
import com.halotukozak.alpaca.plugin.settings.GrammarAssociation
import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.nio.file.Files
import java.nio.file.Path

class GrammarServiceTest : BasePlatformTestCase() {
    private lateinit var exportDir: Path

    override fun setUp() {
        super.setUp()
        exportDir = Files.createTempDirectory("grammar-service-test")
        AlpacaSettingsState.getInstance(project).exportDirectory = exportDir.toString()
    }

    override fun tearDown() {
        try {
            AlpacaSettingsState.getInstance(project).exportDirectory = ""
            AlpacaSettingsState.getInstance(project).associations = mutableListOf()
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

    fun `test scans the export directory once and reuses the result until invalidated`() {
        writeLexer("L@L1", versionedJson("""[{"name":"A","pattern":"a","ignored":false}]"""))
        val service = GrammarService.getInstance(project)

        assertEquals(listOf("A"), service.singleLexerTokenNames())

        // Change the file on disk; the cached scan must not reflect it yet.
        writeLexer("L@L1", versionedJson("""[{"name":"B","pattern":"b","ignored":false}]"""))
        assertEquals(listOf("A"), service.singleLexerTokenNames())

        service.invalidate()
        assertEquals(listOf("B"), service.singleLexerTokenNames())
    }

    fun `test returns no grammars when no export directory is configured`() {
        AlpacaSettingsState.getInstance(project).exportDirectory = ""

        val grammars = GrammarService.getInstance(project).exportedGrammars()

        assertEmpty(grammars.lexers)
        assertEmpty(grammars.parsers)
    }

    fun `test resolveForFile maps a file's extension to the associated grammar`() {
        writeLexer("MyLexer@L7", versionedJson("""[{"name":"kw","pattern":"let","ignored":false}]"""))
        AlpacaSettingsState.getInstance(project).associations =
            mutableListOf(GrammarAssociation(extension = "demo", lexerGrammarId = "MyLexer@L7"))

        val service = GrammarService.getInstance(project)
        val demoFile = myFixture.configureByText("a.demo", "let").virtualFile
        val txtFile = myFixture.configureByText("a.txt", "let").virtualFile

        assertEquals("MyLexer@L7", service.resolveForFile(demoFile)?.lexerId)
        assertNull(service.resolveForFile(txtFile))
    }

    fun `test isUnderExportDirectory recognises files inside the export directory`() {
        val service = GrammarService.getInstance(project)

        assertTrue(service.isUnderExportDirectory(exportDir.resolve("Foo.tokens.json").toString()))
        assertFalse(service.isUnderExportDirectory(exportDir.parent.resolve("elsewhere.json").toString()))
    }

    fun `test shows a notification when a scan finds a version-incompatible export`() {
        // The pre-envelope shape: a bare array, no "version" key at all -> version 0.
        writeLexer("Old.L@L1", """[{"name":"kw","pattern":"let","ignored":false}]""")
        val notifications = mutableListOf<Notification>()
        project.messageBus.connect(testRootDisposable).subscribe(
            Notifications.TOPIC,
            object : Notifications {
                override fun notify(notification: Notification) {
                    notifications += notification
                }
            },
        )

        GrammarService.getInstance(project).exportedGrammars()

        assertEquals(1, notifications.size)
        assertTrue(notifications.single().content.contains("Old.L@L1.tokens.json"))
    }

    fun `test does not renotify while the scan stays cached`() {
        writeLexer("Old.L@L1", """[{"name":"kw","pattern":"let","ignored":false}]""")
        val notifications = mutableListOf<Notification>()
        project.messageBus.connect(testRootDisposable).subscribe(
            Notifications.TOPIC,
            object : Notifications {
                override fun notify(notification: Notification) {
                    notifications += notification
                }
            },
        )
        val service = GrammarService.getInstance(project)

        service.exportedGrammars()
        service.exportedGrammars()

        assertEquals(1, notifications.size)
    }
}
