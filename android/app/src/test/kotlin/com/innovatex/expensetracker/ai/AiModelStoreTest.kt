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

    @Test
    fun `test qwen flatbuffer is rejected by bundle validator`() {
        val dir = modelStore.ensureDirectoryExists()
        val qwenFile = File(dir, AiModelStore.GENERATIVE_LLM_FILE_NAME)
        // Write dummy data > 1MB
        val bytes = ByteArray(1_050_000)
        qwenFile.writeBytes(bytes)

        // Must reject qwen files to prevent native SIGABRT crashes
        assertFalse(modelStore.isValidMediaPipeBundle(qwenFile))
        assertFalse(modelStore.isGenerativeLlmReady())
    }

    @Test
    fun `test purgeIncompatibleModels deletes invalid model files`() {
        val dir = modelStore.ensureDirectoryExists()
        val qwenFile = File(dir, AiModelStore.GENERATIVE_LLM_FILE_NAME)
        qwenFile.writeBytes(ByteArray(1_050_000))

        assertTrue(qwenFile.exists())
        val purged = modelStore.purgeIncompatibleModels()
        assertTrue(purged)
        assertFalse(qwenFile.exists())
        assertFalse(modelStore.isGenerativeLlmReady())
    }
}
