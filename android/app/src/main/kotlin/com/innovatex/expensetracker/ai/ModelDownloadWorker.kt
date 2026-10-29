package com.innovatex.expensetracker.ai

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
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

        modelStore.ensureDirectoryExists()
        updateForeground(5, "Connecting to model repository...")

        if (targetEngine == AiEnginePreference.GENERATIVE_LLM) {
            val partFile = modelStore.generativeLlmPartFile
            val targetFile = modelStore.generativeLlmFile

            // Check if .part file is already fully downloaded and verified on disk
            if (partFile.exists() && modelStore.isValidMediaPipeBundle(partFile)) {
                installFileWithFallback(partFile, targetFile)
                if (modelStore.isGenerativeLlmReady()) {
                    preferencesDataStore.setPreferredAiEngine(AiEnginePreference.GENERATIVE_LLM)
                    preferencesDataStore.setAiClassificationEnabled(true)
                    setProgress(workDataOf(KEY_PROGRESS to 100, KEY_STATUS to "Ready"))
                    updateForeground(100, "Qwen 2.5 SLM is ready")
                    return@withContext Result.success(workDataOf(KEY_PROGRESS to 100, KEY_STATUS to "Success"))
                }
            }

            try {
                val resumePercent = calculateExistingPercent(partFile, 5, 95, 521_000_000L)
                val initialStatus = if (resumePercent > 5) "Resuming Qwen 2.5 SLM ($resumePercent%)..." else "Connecting to LiteRT Community..."
                setProgress(workDataOf(KEY_PROGRESS to resumePercent, KEY_STATUS to initialStatus))
                updateForeground(resumePercent, initialStatus)

                downloadWithResumeAndRetry(
                    urls = GENERATIVE_LLM_URLS,
                    destination = partFile,
                    startPercent = 5,
                    endPercent = 95,
                    taskLabel = "Downloading Qwen 2.5 SLM Task (~520 MB)",
                    isBundle = true
                )

                if (isStopped) {
                    // Do NOT delete the partial download! Keep it for resumption
                    return@withContext Result.failure(workDataOf(KEY_STATUS to "Download paused / cancelled"))
                }

                setProgress(workDataOf(KEY_PROGRESS to 95, KEY_STATUS to "Verifying LLM binary..."))
                updateForeground(95, "Verifying LLM binary...")

                if (partFile.exists() && modelStore.isValidMediaPipeBundle(partFile)) {
                    val moved = installFileWithFallback(partFile, targetFile)
                    if (!moved) {
                        return@withContext Result.failure(workDataOf(KEY_STATUS to "Failed to install LLM into app storage"))
                    }
                } else {
                    // If file is tiny (<1MB), it may be a corrupt error response; otherwise keep for resume
                    val msg = if (partFile.exists() && partFile.length() < 1_000_000L) {
                        partFile.delete()
                        "Downloaded file was invalid or corrupted"
                    } else {
                        "Download incomplete or verification failed. Tap Retry to resume."
                    }
                    return@withContext Result.failure(workDataOf(KEY_STATUS to msg))
                }

                if (modelStore.isGenerativeLlmReady()) {
                    preferencesDataStore.setPreferredAiEngine(AiEnginePreference.GENERATIVE_LLM)
                    preferencesDataStore.setAiClassificationEnabled(true)
                    setProgress(workDataOf(KEY_PROGRESS to 100, KEY_STATUS to "Ready"))
                    updateForeground(100, "Qwen 2.5 SLM is ready")
                    Result.success(workDataOf(KEY_PROGRESS to 100, KEY_STATUS to "Success"))
                } else {
                    modelStore.deleteGenerativeLlm()
                    Result.failure(workDataOf(KEY_STATUS to "Generative LLM verification failed"))
                }
            } catch (e: Exception) {
                // Keep partial file on disk so the next attempt resumes!
                val errorMsg = e.localizedMessage ?: e.message ?: "Network error downloading LLM"
                Result.failure(workDataOf(KEY_STATUS to errorMsg))
            }
        } else {
            // Financial Transformer (ONNX)
            val modelPart = modelStore.modelPartFile
            val vocabPart = modelStore.vocabPartFile

            try {
                setProgress(workDataOf(KEY_PROGRESS to 5, KEY_STATUS to "Connecting to Hugging Face..."))
                updateForeground(5, "Connecting to Hugging Face...")

                downloadWithResumeAndRetry(
                    urls = TRANSFORMER_MODEL_URLS,
                    destination = modelPart,
                    startPercent = 5,
                    endPercent = 85,
                    taskLabel = "Downloading Financial Transformer (~35 MB)",
                    isBundle = false
                )

                if (isStopped) {
                    return@withContext Result.failure(workDataOf(KEY_STATUS to "Download cancelled"))
                }

                downloadWithResumeAndRetry(
                    urls = TRANSFORMER_VOCAB_URLS,
                    destination = vocabPart,
                    startPercent = 85,
                    endPercent = 95,
                    taskLabel = "Downloading vocabulary",
                    isBundle = false
                )

                if (isStopped) {
                    return@withContext Result.failure(workDataOf(KEY_STATUS to "Download cancelled"))
                }

                setProgress(workDataOf(KEY_PROGRESS to 95, KEY_STATUS to "Verifying transformer files..."))
                updateForeground(95, "Verifying transformer files...")

                if (modelPart.exists() && modelPart.length() > 0 &&
                    vocabPart.exists() && vocabPart.length() > 0
                ) {
                    val modelMoved = installFileWithFallback(modelPart, modelStore.modelFile)
                    val vocabMoved = installFileWithFallback(vocabPart, modelStore.vocabFile)

                    if (!modelMoved || !vocabMoved) {
                        return@withContext Result.failure(workDataOf(KEY_STATUS to "Failed to install model files into app storage"))
                    }
                } else {
                    return@withContext Result.failure(workDataOf(KEY_STATUS to "Downloaded files were empty"))
                }

                if (modelStore.isTransformerReady()) {
                    preferencesDataStore.setPreferredAiEngine(AiEnginePreference.FINANCIAL_TRANSFORMER)
                    preferencesDataStore.setAiClassificationEnabled(true)
                    setProgress(workDataOf(KEY_PROGRESS to 100, KEY_STATUS to "Ready"))
                    updateForeground(100, "Financial Transformer ready")
                    Result.success(workDataOf(KEY_PROGRESS to 100, KEY_STATUS to "Success"))
                } else {
                    modelStore.deleteTransformer()
                    Result.failure(workDataOf(KEY_STATUS to "Model installation verification failed"))
                }
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: e.message ?: "Network error downloading transformer"
                Result.failure(workDataOf(KEY_STATUS to errorMsg))
            }
        }
    }

    private fun installFileWithFallback(source: File, target: File): Boolean {
        if (!source.exists()) return false
        if (target.exists()) target.delete()
        val moved = source.renameTo(target)
        if (moved) return true
        return try {
            source.copyTo(target, overwrite = true)
            source.delete()
            true
        } catch (e: Exception) {
            Log.e("ModelDownloadWorker", "Failed to move or copy ${source.name} to ${target.name}", e)
            false
        }
    }

    private fun calculateExistingPercent(file: File, start: Int, end: Int, expectedBytes: Long): Int {
        if (!file.exists() || expectedBytes <= 0) return start
        val bytes = file.length()
        val range = end - start
        return (start + ((bytes * range) / expectedBytes).toInt()).coerceIn(start, end)
    }

    private fun updateForeground(progress: Int, status: String) {
        runCatching {
            val channelId = "ai_model_download_channel"
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return@runCatching

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "AI Model Download",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shows progress of local AI model downloads"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(applicationContext, channelId)
                .setContentTitle("Apex Expense AI")
                .setContentText(status)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setProgress(100, progress, false)
                .setOngoing(progress < 100)
                .setSilent(true)
                .build()

            val foregroundInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                ForegroundInfo(NOTIFICATION_ID, notification)
            }
            setForegroundAsync(foregroundInfo)
        }
    }

    private suspend fun downloadWithResumeAndRetry(
        urls: List<String>,
        destination: File,
        startPercent: Int,
        endPercent: Int,
        taskLabel: String,
        isBundle: Boolean
    ) = withContext(Dispatchers.IO) {
        var lastException: Exception? = null
        for (url in urls) {
            var attempts = 0
            val maxAttempts = 3
            while (attempts < maxAttempts && !isStopped) {
                try {
                    downloadFileWithResume(
                        urlString = url,
                        destination = destination,
                        startPercent = startPercent,
                        endPercent = endPercent,
                        taskLabel = taskLabel,
                        isBundle = isBundle
                    )
                    return@withContext // Successfully downloaded
                } catch (e: Exception) {
                    lastException = e
                    attempts++
                    Log.w("ModelDownloadWorker", "Attempt $attempts failed for $url: ${e.message}")
                    if (isStopped) return@withContext
                    if (attempts < maxAttempts) {
                        kotlinx.coroutines.delay(attempts * 2000L)
                    }
                }
            }
        }
        if (lastException != null && !isStopped) {
            throw lastException
        }
    }

    private suspend fun downloadFileWithResume(
        urlString: String,
        destination: File,
        startPercent: Int,
        endPercent: Int,
        taskLabel: String,
        isBundle: Boolean
    ) = withContext(Dispatchers.IO) {
        var currentUrl = urlString
        var connection: HttpURLConnection? = null
        val maxRedirects = 8
        var redirects = 0
        var existingBytes = if (destination.exists()) destination.length() else 0L

        try {
            while (redirects < maxRedirects) {
                if (isStopped) return@withContext

                val url = URL(currentUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = 60_000
                    readTimeout = 120_000
                    setRequestProperty("User-Agent", "ExpenseTracker-Android/1.0 (Mobile AI)")
                    if (existingBytes > 0) {
                        setRequestProperty("Range", "bytes=$existingBytes-")
                    }
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
                } else if (code == 416) {
                    // HTTP 416 Range Not Satisfiable: file might already be complete
                    if (existingBytes > 0) {
                        val isGood = if (isBundle) modelStore.isValidMediaPipeBundle(destination) else (destination.length() > 0)
                        if (isGood) {
                            Log.i("ModelDownloadWorker", "File already fully downloaded on disk (HTTP 416 satisfied).")
                            return@withContext
                        } else {
                            Log.w("ModelDownloadWorker", "HTTP 416 but destination is invalid. Resetting partial file.")
                            destination.delete()
                            existingBytes = 0L
                            throw java.io.IOException("HTTP 416: Invalid partial file on disk. Resetting for clean download.")
                        }
                    }
                    break
                } else if (code == HttpURLConnection.HTTP_PARTIAL) {
                    // HTTP 206: Server accepted resume range
                    break
                } else if (code in 200..299) {
                    // HTTP 200: Server sent full file from 0, range was not used
                    if (existingBytes > 0) {
                        Log.w("ModelDownloadWorker", "Server returned HTTP 200 instead of 206 Partial. Starting fresh from 0.")
                        existingBytes = 0L
                    }
                    break
                } else {
                    throw java.io.IOException("HTTP $code ${connection.responseMessage} for $currentUrl")
                }
            }

            if (redirects >= maxRedirects) {
                throw java.io.IOException("Too many redirects ($redirects) for $urlString")
            }

            val conn = connection ?: throw java.io.IOException("Failed to connect to $urlString")
            val isPartial = conn.responseCode == HttpURLConnection.HTTP_PARTIAL

            // Accurately determine totalBytes from Content-Range or Content-Length
            val contentRange = conn.getHeaderField("Content-Range")
            val totalFromRange = contentRange?.substringAfterLast('/', "")?.toLongOrNull()
            val totalBytes = when {
                totalFromRange != null && totalFromRange > 0 -> totalFromRange
                conn.contentLengthLong > 0 -> if (isPartial) existingBytes + conn.contentLengthLong else conn.contentLengthLong
                else -> -1L
            }

            var downloadedBytes = existingBytes
            val append = existingBytes > 0 && isPartial

            // Immediately report real resume progress if resuming
            if (totalBytes > 0 && existingBytes > 0) {
                val range = endPercent - startPercent
                val resumePercent = (startPercent + ((existingBytes * range) / totalBytes).toInt())
                    .coerceIn(startPercent, endPercent)
                setProgress(workDataOf(
                    KEY_PROGRESS to resumePercent,
                    KEY_STATUS to "$taskLabel (Resuming at $resumePercent%)..."
                ))
                updateForeground(resumePercent, "$taskLabel (Resuming at $resumePercent%)...")
            }

            BufferedInputStream(conn.inputStream).use { input ->
                FileOutputStream(destination, append).use { output ->
                    val buffer = ByteArray(65536)
                    var count: Int
                    var lastPercent = startPercent

                    while (input.read(buffer).also { count = it } != -1) {
                        if (isStopped) return@withContext

                        output.write(buffer, 0, count)
                        downloadedBytes += count

                        if (totalBytes > 0) {
                            val range = endPercent - startPercent
                            val percent = (startPercent + ((downloadedBytes * range) / totalBytes).toInt())
                                .coerceIn(startPercent, endPercent)
                            if (percent - lastPercent >= 1) {
                                lastPercent = percent
                                setProgress(workDataOf(
                                    KEY_PROGRESS to percent,
                                    KEY_STATUS to "$taskLabel ($percent%)..."
                                ))
                                updateForeground(percent, "$taskLabel ($percent%)...")
                            }
                        }
                    }
                    output.flush()
                }
            }

            // Strictly verify that the stream was not truncated by an early socket close
            if (!isStopped && totalBytes > 0 && downloadedBytes < totalBytes) {
                throw java.io.IOException(
                    "Download stream terminated prematurely: received $downloadedBytes bytes of $totalBytes bytes"
                )
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
        private const val NOTIFICATION_ID = 2001

        // Generative SLM (MediaPipe Tasks-GenAI / LiteRT - Qwen 2.5 0.5B INT8 Task Bundle)
        val GENERATIVE_LLM_URLS = listOf(
            "https://huggingface.co/litert-community/Qwen2.5-0.5B-Instruct/resolve/main/Qwen2.5-0.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
            "https://hf-mirror.com/litert-community/Qwen2.5-0.5B-Instruct/resolve/main/Qwen2.5-0.5B-Instruct_multi-prefill-seq_q8_ekv1280.task"
        )
        const val GENERATIVE_LLM_URL =
            "https://huggingface.co/litert-community/Qwen2.5-0.5B-Instruct/resolve/main/Qwen2.5-0.5B-Instruct_multi-prefill-seq_q8_ekv1280.task"

        // Financial Sequence Transformer (ONNX)
        val TRANSFORMER_MODEL_URLS = listOf(
            "https://huggingface.co/onnx-community/bert-tiny-ONNX/resolve/main/onnx/model_quantized.onnx",
            "https://hf-mirror.com/onnx-community/bert-tiny-ONNX/resolve/main/onnx/model_quantized.onnx"
        )
        val TRANSFORMER_VOCAB_URLS = listOf(
            "https://huggingface.co/onnx-community/bert-tiny-ONNX/resolve/main/vocab.txt",
            "https://hf-mirror.com/onnx-community/bert-tiny-ONNX/resolve/main/vocab.txt"
        )
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
