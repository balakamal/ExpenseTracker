package com.innovatex.expensetracker.ai

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class AiModelStoreTest {
    @TempDir lateinit var tempDir: File
    private lateinit var modelStore: AiModelStore

    @BeforeEach
    fun setup() {
        val context = mockk<Context>(relaxed = true)
        every { context.filesDir } returns tempDir
        modelStore = AiModelStore(context)
    }

    @Test
    fun `model is not ready without pinned checksum`() {
        val file = File(modelStore.ensureDirectoryExists(), AiModelStore.GENERATIVE_LLM_FILE_NAME)
        file.writeBytes(ByteArray(11_000_000))
        assertFalse(modelStore.isGenerativeLlmReady())
        assertEquals(0L, modelStore.getModelSizeBytes())
    }

    @Test
    fun `verified LiteRT model is ready only when checksum matches`() {
        val file = File(modelStore.ensureDirectoryExists(), AiModelStore.GENERATIVE_LLM_FILE_NAME)
        file.writeBytes(ByteArray(11_000_000) { (it % 251).toByte() })
        val checksum = modelStore.sha256(file)
        modelStore.generativeLlmMetadataFile.writeText("$checksum|${file.length()}|${file.lastModified()}")

        assertTrue(modelStore.isValidLiteRtModel(file, checksum))
        assertFalse(modelStore.isValidLiteRtModel(file, "0".repeat(64)))
        assertTrue(modelStore.isGenerativeLlmReady())
        assertTrue(modelStore.getModelSizeBytes() > 0)
        modelStore.generativeLlmMetadataFile.writeText("${"0".repeat(64)}|${file.length()}|${file.lastModified()}")
        assertFalse(modelStore.isGenerativeLlmReady())
    }

    @Test
    fun `delete model removes active partial and checksum files`() {
        modelStore.ensureDirectoryExists()
        modelStore.generativeLlmFile.writeText("active")
        modelStore.generativeLlmPartFile.writeText("partial")
        modelStore.generativeLlmMetadataFile.writeText("checksum")
        assertTrue(modelStore.deleteModel())
        assertFalse(modelStore.generativeLlmFile.exists())
        assertFalse(modelStore.generativeLlmPartFile.exists())
        assertFalse(modelStore.generativeLlmMetadataFile.exists())
    }

}
