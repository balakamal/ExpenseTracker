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
    fun `test unbundled flatbuffer without zip header is rejected by bundle validator`() {
        val dir = modelStore.ensureDirectoryExists()
        val badFile = File(dir, AiModelStore.GENERATIVE_LLM_FILE_NAME)
        // Write dummy flatbuffer data > 1MB without zip header
        val bytes = ByteArray(1_050_000)
        badFile.writeBytes(bytes)

        // Must reject raw non-zip flatbuffers to prevent native SIGABRT crashes
        assertFalse(modelStore.isValidMediaPipeBundle(badFile))
        assertFalse(modelStore.isGenerativeLlmReady())
    }

    @Test
    fun `test valid mediapipe task zip bundle is accepted by bundle validator`() {
        val dir = modelStore.ensureDirectoryExists()
        val taskFile = File(dir, AiModelStore.GENERATIVE_LLM_FILE_NAME)
        // Write dummy zip header PK\x03\x04 followed by data > 1MB
        val bytes = ByteArray(1_050_000)
        bytes[0] = 0x50.toByte() // P
        bytes[1] = 0x4B.toByte() // K
        bytes[2] = 0x03.toByte()
        bytes[3] = 0x04.toByte()
        taskFile.writeBytes(bytes)

        assertTrue(modelStore.isValidMediaPipeBundle(taskFile))
        assertTrue(modelStore.isGenerativeLlmReady())
    }

    @Test
    fun `test purgeIncompatibleModels deletes invalid model files`() {
        val dir = modelStore.ensureDirectoryExists()
        val badFile = File(dir, AiModelStore.GENERATIVE_LLM_FILE_NAME)
        badFile.writeBytes(ByteArray(1_050_000))

        val legacyTflite = File(dir, "qwen2.5_0.5b_instruct.tflite")
        legacyTflite.writeBytes(ByteArray(500))

        assertTrue(badFile.exists())
        assertTrue(legacyTflite.exists())

        val purged = modelStore.purgeIncompatibleModels()
        assertTrue(purged)
        assertFalse(badFile.exists())
        assertFalse(legacyTflite.exists())
        assertFalse(modelStore.isGenerativeLlmReady())
    }
}
