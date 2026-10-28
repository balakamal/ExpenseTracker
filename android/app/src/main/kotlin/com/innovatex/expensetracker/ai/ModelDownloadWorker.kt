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
        if (modelStore.isModelReady()) {
            preferencesDataStore.setAiClassificationEnabled(true)
            setProgress(workDataOf(KEY_PROGRESS to 100, KEY_STATUS to "Model already downloaded"))
            return@withContext Result.success(workDataOf(KEY_PROGRESS to 100, KEY_STATUS to "Model ready"))
        }

        val targetDir = modelStore.ensureDirectoryExists()
        val tempModelFile = File(targetDir, "model_temp.onnx")
        val tempVocabFile = File(targetDir, "vocab_temp.txt")

        try {
            // Step 1: Download INT8 Quantized Model (~4.5 MB)
            setProgress(workDataOf(KEY_PROGRESS to 5, KEY_STATUS to "Connecting to Hugging Face..."))

            downloadFileWithRedirects(
                urlString = MODEL_ONNX_URL,
                destination = tempModelFile,
                startPercent = 5,
                endPercent = 85,
                taskLabel = "Downloading AI model (4.5 MB)"
            )

            if (isStopped) {
                tempModelFile.delete()
                return@withContext Result.failure(workDataOf(KEY_STATUS to "Download cancelled"))
            }

            // Step 2: Download Vocabulary (~220 KB)
            setProgress(workDataOf(KEY_PROGRESS to 85, KEY_STATUS to "Downloading vocabulary (220 KB)..."))

            downloadFileWithRedirects(
                urlString = VOCAB_URL,
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

            setProgress(workDataOf(KEY_PROGRESS to 95, KEY_STATUS to "Verifying model files..."))

            // Step 3: Atomic install
            if (tempModelFile.exists() && tempModelFile.length() > 0 &&
                tempVocabFile.exists() && tempVocabFile.length() > 0
            ) {
                // Delete existing destination files if any
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

            if (modelStore.isModelReady()) {
                preferencesDataStore.setAiClassificationEnabled(true)
                setProgress(workDataOf(KEY_PROGRESS to 100, KEY_STATUS to "Ready"))
                Result.success(workDataOf(KEY_PROGRESS to 100, KEY_STATUS to "Success"))
            } else {
                modelStore.deleteModel()
                Result.failure(workDataOf(KEY_STATUS to "Model installation verification failed"))
            }
        } catch (e: Exception) {
            tempModelFile.delete()
            tempVocabFile.delete()
            val errorMsg = e.localizedMessage ?: e.message ?: "Network error during download"
            Result.failure(workDataOf(KEY_STATUS to errorMsg))
        }
    }

    private suspend fun downloadFileWithRedirects(
        urlString: String,
        destination: File,
        startPercent: Int,
        endPercent: Int,
        taskLabel: String
    ) {
        var currentUrl = urlString
        var redirects = 0
        val maxRedirects = 5
        var connection: HttpURLConnection? = null

        try {
            while (redirects < maxRedirects) {
                val url = URL(currentUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 20000
                    readTimeout = 45000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "ExpenseTracker-Android/1.0")
                }

                val code = connection.responseCode
                if (code == HttpURLConnection.HTTP_MOVED_PERM ||
                    code == HttpURLConnection.HTTP_MOVED_TEMP ||
                    code == HttpURLConnection.HTTP_SEE_OTHER ||
                    code == 307 || code == 308
                ) {
                    val location = connection.getHeaderField("Location")
                        ?: throw java.io.IOException("Redirect with no Location header from $currentUrl")
                    connection.disconnect()
                    currentUrl = if (location.startsWith("http")) location else URL(url, location).toString()
                    redirects++
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
                    val buffer = ByteArray(8192)
                    var count: Int
                    var lastPercent = startPercent

                    while (input.read(buffer).also { count = it } != -1) {
                        if (isStopped) {
                            return
                        }
                        output.write(buffer, 0, count)
                        downloadedBytes += count

                        if (totalBytes > 0) {
                            val range = endPercent - startPercent
                            val percent = startPercent + ((downloadedBytes * range) / totalBytes).toInt()
                            if (percent - lastPercent >= 3) {
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

        // Canonical Hugging Face public repository for quantized BERT-Tiny ONNX
        const val MODEL_ONNX_URL =
            "https://huggingface.co/onnx-community/bert-tiny-ONNX/resolve/main/onnx/model_quantized.onnx"
        const val VOCAB_URL =
            "https://huggingface.co/onnx-community/bert-tiny-ONNX/resolve/main/vocab.txt"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
                .setConstraints(constraints)
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
