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

    @TempDir
    lateinit var tempDir: File

    private lateinit var mockContext: Context
    private lateinit var modelStore: AiModelStore

    @BeforeEach
    fun setup() {
        mockContext = mockk(relaxed = true)
        every { mockContext.filesDir } returns tempDir
        modelStore = AiModelStore(mockContext)
    }

    @Test
    fun `test model not ready initially`() {
        assertFalse(modelStore.isModelReady())
        assertEquals(0L, modelStore.getModelSizeBytes())
    }

    @Test
    fun `test model ready when both model and vocab exist`() {
        val dir = modelStore.ensureDirectoryExists()
        val model = File(dir, AiModelStore.MODEL_FILE_NAME)
        val vocab = File(dir, AiModelStore.VOCAB_FILE_NAME)

        model.writeText("dummy onnx content")
        vocab.writeText("dummy vocab content")

        assertTrue(modelStore.isModelReady())
        assertTrue(modelStore.getModelSizeBytes() > 0)
    }

    @Test
    fun `test delete model removes files`() {
        val dir = modelStore.ensureDirectoryExists()
        val model = File(dir, AiModelStore.MODEL_FILE_NAME)
        val vocab = File(dir, AiModelStore.VOCAB_FILE_NAME)

        model.writeText("dummy onnx content")
        vocab.writeText("dummy vocab content")

        assertTrue(modelStore.isModelReady())

        modelStore.deleteModel()
        assertFalse(modelStore.isModelReady())
        assertFalse(model.exists())
        assertFalse(vocab.exists())
    }
}
