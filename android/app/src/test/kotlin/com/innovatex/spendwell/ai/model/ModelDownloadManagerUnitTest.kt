package com.innovatex.spendwell.ai.model

import android.content.Context
import com.innovatex.spendwell.core.DispatcherProvider
import com.innovatex.spendwell.data.preferences.UserPreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ModelDownloadManagerUnitTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var context: Context
    private lateinit var preferencesRepository: UserPreferencesRepository
    private lateinit var testDispatchers: DispatcherProvider
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        context = mockk(relaxed = true)
        preferencesRepository = mockk(relaxed = true)

        every { context.filesDir } returns tempDir
        every { preferencesRepository.isWifiOnlyDownload } returns flowOf(false)
        every { preferencesRepository.hfApiToken } returns flowOf(null)
        every { preferencesRepository.customModelUrl } returns flowOf(null)

        testDispatchers = object : DispatcherProvider {
            override val main: CoroutineDispatcher = testDispatcher
            override val io: CoroutineDispatcher = testDispatcher
            override val default: CoroutineDispatcher = testDispatcher
            override val unconfined: CoroutineDispatcher = testDispatcher
        }
    }

    @Test
    fun `initial state is NotDownloaded when model file does not exist`() {
        val manager = ModelDownloadManager(context, preferencesRepository, testDispatchers)
        assertEquals(ModelDownloadState.NotDownloaded, manager.downloadState.value)
    }

    @Test
    fun `scanForLocalModel detects existing binary and transitions to Ready`() {
        val modelsDir = File(tempDir, "models")
        modelsDir.mkdirs()
        val dummyModel = File(modelsDir, ModelDownloadManager.MODEL_FILE_NAME)
        // Create dummy file > 10MB
        val bytes = ByteArray(1024 * 1024) // 1MB chunk
        dummyModel.outputStream().use { fos ->
            repeat(11) {
                fos.write(bytes)
            }
        }

        val manager = ModelDownloadManager(context, preferencesRepository, testDispatchers)
        val detected = manager.scanForLocalModel()

        assertTrue(detected)
        val state = manager.downloadState.value
        assertTrue(state is ModelDownloadState.Ready)
        assertEquals(dummyModel.length(), (state as ModelDownloadState.Ready).sizeBytes)
    }

    @Test
    fun `deleteModel removes model file and resets state to NotDownloaded`() = runTest(testDispatcher) {
        val modelsDir = File(tempDir, "models")
        modelsDir.mkdirs()
        val dummyModel = File(modelsDir, ModelDownloadManager.MODEL_FILE_NAME)
        dummyModel.writeText("sample model content exceeding min size threshold")

        val manager = ModelDownloadManager(context, preferencesRepository, testDispatchers)
        manager.deleteModel()

        assertFalse(dummyModel.exists())
        assertEquals(ModelDownloadState.NotDownloaded, manager.downloadState.value)
        coVerify { preferencesRepository.setModelDownloaded(false, null) }
    }

    @Test
    fun `401 and 403 error messages clearly mention Hugging Face and gated access requirements`() {
        val msg401 = "Hugging Face authentication required (HTTP 401). Gemma 2B is a gated model. Please provide your Hugging Face Access Token with accepted Gemma terms, or configure a custom download URL / import file."
        val msg403 = "Access forbidden (HTTP 403). Ensure you have accepted the Gemma license terms at huggingface.co/google/gemma-2b-it-tflite and your token has Read permissions."

        assertTrue(msg401.contains("401"))
        assertTrue(msg401.contains("Hugging Face"))
        assertTrue(msg401.contains("gated model"))

        assertTrue(msg403.contains("403"))
        assertTrue(msg403.contains("huggingface.co/google/gemma-2b-it-tflite"))
    }
}

