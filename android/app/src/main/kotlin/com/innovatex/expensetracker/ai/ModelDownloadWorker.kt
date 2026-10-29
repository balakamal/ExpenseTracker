package com.innovatex.expensetracker.ai

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.innovatex.expensetracker.data.local.AiEnginePreference
import com.innovatex.expensetracker.data.local.PreferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class ModelDownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val modelStore = AiModelStore(appContext)
    private val preferencesDataStore = PreferencesDataStore(appContext)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val targetEngineStr = inputData.getString(KEY_TARGET_ENGINE) ?: AiEnginePreference.GENERATIVE_LLM.name
        val targetEngine = runCatching { AiEnginePreference.valueOf(targetEngineStr) }.getOrDefault(AiEnginePreference.GENERATIVE_LLM)

        if (modelStore.isEngineReady(targetEngine)) {
            preferencesDataStore.setPreferredAiEngine(targetEngine)
            preferencesDataStore.setAiClassificationEnabled(true)
            setProgress(workDataOf(KEY_PROGRESS to 100, KEY_STATUS to "Model already downloaded"))
            return@withContext Result.success(workDataOf(KEY_PROGRESS to 100, KEY_STATUS to "Model ready"))
        }

        val targetDir = modelStore.ensureDirectoryExists()

        if (targetEngine == AiEnginePreference.GENERATIVE_LLM) {
            val tempLlmFile = File(targetDir, "llm_temp.tflite")
            try {
                setProgress(workDataOf(KEY_PROGRESS to 5, KEY_STATUS to "Connecting to LiteRT Community..."))

                downloadFileWithRedirects(
                    urlString = GENERATIVE_LLM_URL,
                    destination = tempLlmFile,
                    startPercent = 5,
                    endPercent = 95,
                    taskLabel = "Downloading Qwen 2.5 SLM (~520 MB)"
                )

                if (isStopped) {
                    tempLlmFile.delete()
                    return@withContext Result.failure(workDataOf(KEY_STATUS to "Download cancelled"))
                }

                setProgress(workDataOf(KEY_PROGRESS to 95, KEY_STATUS to "Verifying LLM binary..."))

                if (tempLlmFile.exists() && tempLlmFile.length() > 1_000_000L) {
                    if (modelStore.generativeLlmFile.exists()) modelStore.generativeLlmFile.delete()
                    val moved = tempLlmFile.renameTo(modelStore.generativeLlmFile)
                    if (!moved) {
                        tempLlmFile.delete()
                        return@withContext Result.failure(workDataOf(KEY_STATUS to "Failed to install LLM into app storage"))
                    }
                } else {
                    tempLlmFile.delete()
                    return@withContext Result.failure(workDataOf(KEY_STATUS to "Downloaded file was invalid or incomplete"))
                }

                if (modelStore.isGenerativeLlmReady()) {
                    preferencesDataStore.setPreferredAiEngine(AiEnginePreference.GENERATIVE_LLM)
                    preferencesDataStore.setAiClassificationEnabled(true)
                    setProgress(workDataOf(KEY_PROGRESS to 100, KEY_STATUS to "Ready"))
                    Result.success(workDataOf(KEY_PROGRESS to 100, KEY_STATUS to "Success"))
                } else {
                    modelStore.deleteGenerativeLlm()
                    Result.failure(workDataOf(KEY_STATUS to "Generative LLM verification failed"))
                }
            } catch (e: Exception) {
                tempLlmFile.delete()
                val errorMsg = e.localizedMessage ?: e.message ?: "Network error downloading LLM"
                Result.failure(workDataOf(KEY_STATUS to errorMsg))
            }
        } else {
            // Financial Transformer (ONNX)
            val tempModelFile = File(targetDir, "model_temp.onnx")
            val tempVocabFile = File(targetDir, "vocab_temp.txt")

            try {
                setProgress(workDataOf(KEY_PROGRESS to 5, KEY_STATUS to "Connecting to Hugging Face..."))

                downloadFileWithRedirects(
                    urlString = TRANSFORMER_MODEL_URL,
                    destination = tempModelFile,
                    startPercent = 5,
                    endPercent = 85,
                    taskLabel = "Downloading Financial Transformer (~35 MB)"
                )

                if (isStopped) {
                    tempModelFile.delete()
                    return@withContext Result.failure(workDataOf(KEY_STATUS to "Download cancelled"))
                }

                downloadFileWithRedirects(
                    urlString = TRANSFORMER_VOCAB_URL,
                    destination = tempVocabFile,
                    startPercent = 85,
                    endPercent = 95,
                    taskLabel = "Downloading vocabulary"
                )

                if (isStopped) {
                    tempModelFile.delete()
                    tempVocabFile.delete()
                    return@withContext Result.failure(workDataOf(KEY_STATUS to "Download cancelled"))
                }

                setProgress(workDataOf(KEY_PROGRESS to 95, KEY_STATUS to "Verifying transformer files..."))

                if (tempModelFile.exists() && tempModelFile.length() > 0 &&
                    tempVocabFile.exists() && tempVocabFile.length() > 0
                ) {
                    if (modelStore.modelFile.exists()) modelStore.modelFile.delete()
                    if (modelStore.vocabFile.exists()) modelStore.vocabFile.delete()

                    val modelMoved = tempModelFile.renameTo(modelStore.modelFile)
                    val vocabMoved = tempVocabFile.renameTo(modelStore.vocabFile)

                    if (!modelMoved || !vocabMoved) {
                        tempModelFile.delete()
                        tempVocabFile.delete()
                        return@withContext Result.failure(workDataOf(KEY_STATUS to "Failed to install model files into app storage"))
                    }
                } else {
                    tempModelFile.delete()
                    tempVocabFile.delete()
                    return@withContext Result.failure(workDataOf(KEY_STATUS to "Downloaded files were empty"))
                }

                if (modelStore.isTransformerReady()) {
                    preferencesDataStore.setPreferredAiEngine(AiEnginePreference.FINANCIAL_TRANSFORMER)
                    preferencesDataStore.setAiClassificationEnabled(true)
                    setProgress(workDataOf(KEY_PROGRESS to 100, KEY_STATUS to "Ready"))
                    Result.success(workDataOf(KEY_PROGRESS to 100, KEY_STATUS to "Success"))
                } else {
                    modelStore.deleteTransformer()
                    Result.failure(workDataOf(KEY_STATUS to "Model installation verification failed"))
                }
            } catch (e: Exception) {
                tempModelFile.delete()
                tempVocabFile.delete()
                val errorMsg = e.localizedMessage ?: e.message ?: "Network error downloading transformer"
                Result.failure(workDataOf(KEY_STATUS to errorMsg))
            }
        }
    }

    private suspend fun downloadFileWithRedirects(
        urlString: String,
        destination: File,
        startPercent: Int,
        endPercent: Int,
        taskLabel: String
    ) = withContext(Dispatchers.IO) {
        var currentUrl = urlString
        var connection: HttpURLConnection? = null
        val maxRedirects = 8
        var redirects = 0

        try {
            while (redirects < maxRedirects) {
                if (isStopped) return@withContext

                val url = URL(currentUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = 60_000
                    readTimeout = 120_000
                    setRequestProperty("User-Agent", "ExpenseTracker-Android/1.0 (Mobile AI)")
                }

                val code = connection.responseCode
                if (code == HttpURLConnection.HTTP_MOVED_PERM ||
                    code == HttpURLConnection.HTTP_MOVED_TEMP ||
                    code == HttpURLConnection.HTTP_SEE_OTHER ||
                    code == 307 || code == 308
                ) {
                    val newLocation = connection.getHeaderField("Location")
                        ?: throw java.io.IOException("Redirect response missing Location header from $currentUrl")
                    currentUrl = if (newLocation.startsWith("http://") || newLocation.startsWith("https://")) {
                        newLocation
                    } else {
                        URL(url, newLocation).toString()
                    }
                    connection.disconnect()
                    redirects++
                    continue
                } else if (code in 200..299) {
                    break
                } else {
                    throw java.io.IOException("HTTP $code ${connection.responseMessage} for $currentUrl")
                }
            }

            if (redirects >= maxRedirects) {
                throw java.io.IOException("Too many redirects ($redirects) for $urlString")
            }

            val conn = connection ?: throw java.io.IOException("Failed to connect to $urlString")
            val totalBytes = conn.contentLengthLong
            var downloadedBytes = 0L

            BufferedInputStream(conn.inputStream).use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(32768)
                    var count: Int
                    var lastPercent = startPercent

                    while (input.read(buffer).also { count = it } != -1) {
                        if (isStopped) return@withContext

                        output.write(buffer, 0, count)
                        downloadedBytes += count

                        if (totalBytes > 0) {
                            val range = endPercent - startPercent
                            val percent = startPercent + ((downloadedBytes * range) / totalBytes).toInt()
                            if (percent - lastPercent >= 2) {
                                lastPercent = percent
                                setProgress(workDataOf(
                                    KEY_PROGRESS to percent,
                                    KEY_STATUS to "$taskLabel ($percent%)..."
                                ))
                            }
                        }
                    }
                }
            }
        } finally {
            connection?.disconnect()
        }
    }

    companion object {
        const val WORK_NAME = "ai_model_download_worker"
        const val KEY_PROGRESS = "progress"
        const val KEY_STATUS = "status"
        const val KEY_TARGET_ENGINE = "target_engine"

        // Generative SLM (MediaPipe Tasks-GenAI / LiteRT - Qwen 2.5 0.5B INT8)
        const val GENERATIVE_LLM_URL =
            "https://huggingface.co/litert-community/Qwen2.5-0.5B-Instruct/resolve/main/Qwen2.5-0.5B-Instruct_seq128_q8_ekv1280.tflite"

        // Financial Sequence Transformer (ONNX)
        const val TRANSFORMER_MODEL_URL =
            "https://huggingface.co/onnx-community/bert-tiny-ONNX/resolve/main/onnx/model_quantized.onnx"
        const val TRANSFORMER_VOCAB_URL =
            "https://huggingface.co/onnx-community/bert-tiny-ONNX/resolve/main/vocab.txt"

        fun enqueue(context: Context, engine: AiEnginePreference = AiEnginePreference.GENERATIVE_LLM) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
                .setConstraints(constraints)
                .setInputData(workDataOf(KEY_TARGET_ENGINE to engine.name))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
