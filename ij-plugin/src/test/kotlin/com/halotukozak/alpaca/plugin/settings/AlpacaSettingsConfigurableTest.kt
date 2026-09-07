package com.halotukozak.alpaca.plugin.settings

import com.halotukozak.alpaca.plugin.grammar.GrammarService
import com.halotukozak.alpaca.plugin.grammar.versionedJson
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.nio.file.Files
import java.nio.file.Path

class AlpacaSettingsConfigurableTest : BasePlatformTestCase() {
    private lateinit var exportDir: Path
    private lateinit var settings: AlpacaSettingsState

    override fun setUp() {
        super.setUp()
        exportDir = Files.createTempDirectory("settings-configurable-test")
        settings = AlpacaSettingsState.getInstance(project)
        settings.exportDirectory = exportDir.toString()
        settings.associations = mutableListOf()
    }

    override fun tearDown() {
        try {
            settings.exportDirectory = ""
            settings.associations = mutableListOf()
            Files.walk(exportDir).sorted(Comparator.reverseOrder()).forEach(Files::delete)
        } finally {
            super.tearDown()
        }
    }

    fun `test apply registers a file type for each association and refreshes the grammar cache`() {
        // Warm the cache while the export directory is still empty, exactly like a real session
        // that opened the project before the grammar was exported.
        assertEmpty(GrammarService.getInstance(project).exportedGrammars().lexers)

        Files.writeString(
            exportDir.resolve("CfgTest.L@L1.tokens.json"),
            versionedJson("""[{"name":"kw","pattern":"let","ignored":false}]"""),
        )
        settings.associations = mutableListOf(GrammarAssociation(extension = "cfgtest-ext", lexerGrammarId = "CfgTest.L@L1"))

        val configurable = AlpacaSettingsConfigurable(project)
        configurable.reset()
        configurable.apply()

        val fileType = FileTypeManager.getInstance().getFileTypeByExtension("cfgtest-ext")
        assertEquals("Alpaca:CfgTest.L@L1", fileType.name)

        // Only passes if apply() actually dropped the cache warmed above, not just re-read settings.
        val lexerIds =
            GrammarService
                .getInstance(project)
                .exportedGrammars()
                .lexers
                .map { it.id }
        assertEquals(listOf("CfgTest.L@L1"), lexerIds)
    }

    fun `test isModified compares the loaded fields against the current settings, not their initial values`() {
        settings.associations = mutableListOf(GrammarAssociation(extension = "a", lexerGrammarId = "A@L1"))
        val configurable = AlpacaSettingsConfigurable(project)
        configurable.reset()

        assertFalse(configurable.isModified())

        // Settings changed elsewhere (e.g. GrammarExportChangeListener) while the dialog was open.
        settings.associations = mutableListOf(GrammarAssociation(extension = "b", lexerGrammarId = "B@L1"))

        assertTrue(configurable.isModified())
    }

    fun `test isModified reacts to the export directory as well as the mappings`() {
        val configurable = AlpacaSettingsConfigurable(project)
        configurable.reset()

        assertFalse(configurable.isModified())

        settings.exportDirectory = "$exportDir/elsewhere"

        assertTrue(configurable.isModified())
    }
}
