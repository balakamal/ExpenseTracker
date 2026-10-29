package com.innovatex.expensetracker.ai

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.StatFs
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
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.innovatex.expensetracker.data.local.AiEnginePreference
import com.innovatex.expensetracker.data.local.PreferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/** Resumable, checksum-verified provisioning for the pinned LiteRT-LM model. */
class ModelDownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val modelStore = AiModelStore(context)
    private val preferences = PreferencesDataStore(context)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (modelStore.isGenerativeLlmReady()) return@withContext readyResult()
        try {
            modelStore.ensureDirectoryExists()
            val partial = modelStore.generativeLlmPartFile
            setProgress(workDataOf(KEY_PROGRESS to 1, KEY_STATUS to "Connecting to LiteRT model repository"))
            updateForeground(1, "Connecting to LiteRT model repository")
            downloadWithResume(partial)
            if (isStopped) return@withContext Result.failure(workDataOf(KEY_STATUS to "Download paused; retry to resume"))

            val sha256 = partialShaFile(partial).takeIf { it.isFile }?.readText()?.trim()
            if (sha256.isNullOrBlank() || !modelStore.isValidLiteRtModel(partial, sha256)) {
                partial.delete()
                partialShaFile(partial).delete()
                return@withContext Result.failure(workDataOf(KEY_STATUS to "Model checksum verification failed. Retry the download."))
            }

            setProgress(workDataOf(KEY_PROGRESS to 96, KEY_STATUS to "Checking model compatibility"))
            updateForeground(96, "Checking model compatibility")
            val compatibility = runCatching {
                Engine(EngineConfig(modelPath = partial.absolutePath, backend = Backend.CPU(), cacheDir = applicationContext.cacheDir.absolutePath))
                    .also { it.initialize() }
            }
            val validationEngine = compatibility.getOrElse { error ->
                return@withContext Result.failure(workDataOf(KEY_STATUS to "Model is incompatible or this device has insufficient memory: ${error.message ?: "initialization failed"}"))
            }
            validationEngine.close()

            val installed = runCatching {
                if (modelStore.generativeLlmFile.exists() && !modelStore.generativeLlmFile.delete()) error("Could not replace the existing model")
                if (!partial.renameTo(modelStore.generativeLlmFile)) partial.copyTo(modelStore.generativeLlmFile, overwrite = true).also { partial.delete() }
                val installedModel = modelStore.generativeLlmFile
                modelStore.generativeLlmMetadataFile.writeText("$sha256|${installedModel.length()}|${installedModel.lastModified()}")
                modelStore.isGenerativeLlmReady()
            }.getOrDefault(false)
            partialShaFile(partial).delete()
            if (!installed) {
                modelStore.deleteGenerativeLlm()
                return@withContext Result.failure(workDataOf(KEY_STATUS to "Verified model could not be installed in private storage"))
            }
            readyResult()
        } catch (e: Exception) {
            Result.failure(workDataOf(KEY_STATUS to (e.localizedMessage ?: "Download failed; retry to resume")))
        }
    }

    private suspend fun readyResult(): Result {
        preferences.setPreferredAiEngine(AiEnginePreference.GENERATIVE_LLM)
        preferences.setAiClassificationEnabled(true)
        setProgress(workDataOf(KEY_PROGRESS to 100, KEY_STATUS to "On-device model ready"))
        updateForeground(100, "On-device model ready")
        return Result.success(workDataOf(KEY_PROGRESS to 100, KEY_STATUS to "Model ready"))
    }

    private suspend fun downloadWithResume(destination: java.io.File) {
        var existing = destination.length()
        val savedHashFile = partialShaFile(destination)
        val partialMatchesPinnedModel = existing > 0 && savedHashFile.isFile &&
            runCatching {
                savedHashFile.readText().trim().equals(AiModelStore.PINNED_MODEL_SHA256, ignoreCase = true)
            }.getOrDefault(false)
        if (existing > 0 && !partialMatchesPinnedModel) {
            destination.delete()
            savedHashFile.delete()
            existing = 0L
        }
        val connection = (URL(MODEL_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            setRequestProperty("Accept-Encoding", "identity")
            if (existing > 0) setRequestProperty("Range", "bytes=$existing-")
        }
        try {
            val response = connection.responseCode
            if (response == 416) {
                val total = connection.getHeaderField("Content-Range")?.substringAfterLast('/')?.toLongOrNull()
                if (total == AiModelStore.PINNED_MODEL_SIZE_BYTES && total == existing) return
                destination.delete()
                partialShaFile(destination).delete()
                throw java.io.IOException("Repository rejected an invalid partial range; retry to restart")
            }
            if (response !in 200..299) throw java.io.IOException("Model repository returned HTTP $response")
            val append = response == HttpURLConnection.HTTP_PARTIAL && existing > 0
            if (!append) {
                existing = 0L
                destination.writeBytes(byteArrayOf())
            }

            val expectedHash = AiModelStore.PINNED_MODEL_SHA256
            savedHashFile.writeText(expectedHash)

            val contentRange = connection.getHeaderField("Content-Range")
            val rangeMatch = contentRange?.let { CONTENT_RANGE_REGEX.matchEntire(it.trim()) }
            val total = if (append) {
                val range = rangeMatch ?: throw java.io.IOException("Repository returned an invalid partial-content range")
                val rangeStart = range.groupValues[1].toLong()
                val rangeEnd = range.groupValues[2].toLong()
                val rangeTotal = range.groupValues[3].toLong()
                if (rangeStart != existing || rangeEnd < rangeStart || rangeTotal != AiModelStore.PINNED_MODEL_SIZE_BYTES) {
                    destination.delete()
                    savedHashFile.delete()
                    throw java.io.IOException("Repository returned a range for the wrong model bytes; retry to restart")
                }
                rangeTotal
            } else {
                connection.contentLengthLong.takeIf { it > 0 }
                    ?: throw java.io.IOException("Repository did not report the model size")
            }
            if (total != AiModelStore.PINNED_MODEL_SIZE_BYTES) {
                destination.delete()
                savedHashFile.delete()
                throw java.io.IOException("Downloaded artifact size does not match the pinned model revision")
            }
            val requiredSpace = total - existing + MIN_FREE_SPACE_MARGIN
            if (requiredSpace > StatFs(destination.parentFile!!.absolutePath).availableBytes) {
                throw java.io.IOException("Not enough free storage. Free at least ${requiredSpace / (1024 * 1024)} MB and retry.")
            }
            var bytes = existing
            var receivedThisResponse = 0L
            BufferedInputStream(connection.inputStream).use { input ->
                FileOutputStream(destination, append).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var lastProgress = 0
                    while (true) {
                        if (isStopped) return
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        bytes += count
                        receivedThisResponse += count
                        val progress = (bytes * 94 / total).toInt().coerceIn(1, 94)
                        if (progress > lastProgress) {
                            lastProgress = progress
                            setProgress(workDataOf(KEY_PROGRESS to progress, KEY_STATUS to "Downloading on-device model ($progress%)"))
                            updateForeground(progress, "Downloading on-device model ($progress%)")
                        }
                    }
                    output.fd.sync()
                }
            }
            if (append) {
                val range = requireNotNull(rangeMatch)
                val expectedResponseBytes = range.groupValues[2].toLong() - range.groupValues[1].toLong() + 1
                if (receivedThisResponse != expectedResponseBytes) {
                    throw java.io.IOException("Download range ended early; retry to resume")
                }
            }
            if (bytes != total) throw java.io.IOException("Download ended at $bytes of $total bytes; retry to resume")
            setProgress(workDataOf(KEY_PROGRESS to 95, KEY_STATUS to "Verifying model checksum"))
            updateForeground(95, "Verifying model checksum")
            if (!modelStore.isValidLiteRtModel(destination, expectedHash)) {
                destination.delete()
                savedHashFile.delete()
                throw java.io.IOException("Downloaded model checksum does not match the repository")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun partialShaFile(file: java.io.File) = java.io.File(file.parentFile, "${file.name}.sha256")

    private suspend fun updateForeground(progress: Int, status: String) {
        val channelId = "ai_model_download_channel"
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(channelId, "AI Model Download", NotificationManager.IMPORTANCE_LOW))
        }
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Apex Expense AI").setContentText(status)
            .setSmallIcon(android.R.drawable.stat_sys_download).setProgress(100, progress, false)
            .setOngoing(progress < 100).setSilent(true).build()
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else ForegroundInfo(NOTIFICATION_ID, notification)
        setForeground(info)
    }

    companion object {
        const val WORK_NAME = "ai_model_download_worker"
        const val KEY_PROGRESS = "progress"
        const val KEY_STATUS = "status"
        const val KEY_TARGET_ENGINE = "target_engine"
        private const val NOTIFICATION_ID = 2001
        private const val MIN_FREE_SPACE_MARGIN = 50L * 1024 * 1024
        private val CONTENT_RANGE_REGEX = Regex("bytes (\\d+)-(\\d+)/(\\d+)", RegexOption.IGNORE_CASE)
        const val MODEL_URL = "https://huggingface.co/litert-community/Qwen3-0.6B/resolve/154c5de0fc0fc5002a7da570ba8c67edffa32ca2/qwen3_0_6b_mixed_int4.litertlm"

        fun enqueue(context: Context, engine: AiEnginePreference = AiEnginePreference.GENERATIVE_LLM) {
            require(engine != AiEnginePreference.FINANCIAL_TRANSFORMER) { "The legacy generic BERT model is not supported" }
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
                .setConstraints(constraints)
                .setInputData(workDataOf(KEY_TARGET_ENGINE to engine.name))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .addTag(WORK_NAME).build()
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        fun cancel(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
