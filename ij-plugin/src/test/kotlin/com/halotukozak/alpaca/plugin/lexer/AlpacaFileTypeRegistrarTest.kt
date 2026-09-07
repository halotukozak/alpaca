package com.halotukozak.alpaca.plugin.lexer

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AlpacaFileTypeRegistrarTest : BasePlatformTestCase() {
    fun `test associates the extension with an AlpacaFileType for the given grammar`() {
        runWriteAction { AlpacaFileTypeRegistrar.ensureRegistered("registrar-test-ext", "SomeGrammar@L1") }

        val fileType = FileTypeManager.getInstance().getFileTypeByExtension("registrar-test-ext")

        assertInstanceOf(fileType, AlpacaFileType::class.java)
        assertEquals("Alpaca:SomeGrammar@L1", fileType.name)
    }

    fun `test reuses the same file type instance for the same grammar id`() {
        runWriteAction {
            AlpacaFileTypeRegistrar.ensureRegistered("registrar-test-a", "SharedGrammar@L2")
            AlpacaFileTypeRegistrar.ensureRegistered("registrar-test-b", "SharedGrammar@L2")
        }

        val fileTypeManager = FileTypeManager.getInstance()
        assertSame(
            fileTypeManager.getFileTypeByExtension("registrar-test-a"),
            fileTypeManager.getFileTypeByExtension("registrar-test-b"),
        )
    }
}
