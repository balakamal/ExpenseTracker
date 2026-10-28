package com.innovatex.expensetracker.ai

import android.content.Context
import androidx.hilt.work.HiltWorker
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
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

@HiltWorker
class ModelDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val modelStore: AiModelStore,
    private val preferencesDataStore: PreferencesDataStore
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (modelStore.isModelReady()) {
            preferencesDataStore.setAiClassificationEnabled(true)
            return@withContext Result.success(workDataOf(KEY_PROGRESS to 100))
        }

        val targetDir = modelStore.ensureDirectoryExists()
        val tempZipFile = File(targetDir, "model_bundle_temp.zip")

        try {
            setProgress(workDataOf(KEY_PROGRESS to 5, KEY_STATUS to "Connecting..."))

            val url = URL(DEFAULT_MODEL_BUNDLE_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                instanceFollowRedirects = true
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                return@withContext Result.retry()
            }

            val contentLength = connection.contentLengthLong
            var downloadedBytes = 0L

            BufferedInputStream(connection.inputStream).use { input ->
                FileOutputStream(tempZipFile).use { output ->
                    val buffer = ByteArray(8192)
                    var count: Int
                    var lastReportedPercent = 5

                    while (input.read(buffer).also { count = it } != -1) {
                        if (isStopped) {
                            tempZipFile.delete()
                            return@withContext Result.failure()
                        }
                        output.write(buffer, 0, count)
                        downloadedBytes += count

                        if (contentLength > 0) {
                            val percent = (5 + ((downloadedBytes * 85) / contentLength)).toInt()
                            if (percent - lastReportedPercent >= 3) {
                                lastReportedPercent = percent
                                setProgress(workDataOf(
                                    KEY_PROGRESS to percent,
                                    KEY_STATUS to "Downloading ($percent%)..."
                                ))
                            }
                        }
                    }
                }
            }

            setProgress(workDataOf(KEY_PROGRESS to 90, KEY_STATUS to "Extracting files..."))

            // Extract ZIP contents
            extractZip(tempZipFile, targetDir)
            tempZipFile.delete()

            if (modelStore.isModelReady()) {
                preferencesDataStore.setAiClassificationEnabled(true)
                setProgress(workDataOf(KEY_PROGRESS to 100, KEY_STATUS to "Complete"))
                Result.success(workDataOf(KEY_PROGRESS to 100))
            } else {
                modelStore.deleteModel()
                Result.failure(workDataOf(KEY_STATUS to "Downloaded bundle missing model or vocab files"))
            }
        } catch (e: Exception) {
            tempZipFile.delete()
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure(workDataOf(KEY_STATUS to (e.message ?: "Download failed")))
            }
        }
    }

    private fun extractZip(zipFile: File, outputDir: File) {
        ZipInputStream(BufferedInputStream(zipFile.inputStream())).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(outputDir, entry.name)
                // Security check against Zip Slip path traversal
                if (!outFile.canonicalPath.startsWith(outputDir.canonicalPath)) {
                    throw SecurityException("Invalid zip entry: ${entry.name}")
                }

                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        val buffer = ByteArray(8192)
                        var len: Int
                        while (zis.read(buffer).also { len = it } != -1) {
                            fos.write(buffer, 0, len)
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    companion object {
        const val WORK_NAME = "ai_model_download_worker"
        const val KEY_PROGRESS = "progress"
        const val KEY_STATUS = "status"

        // Canonical public hosted release / Hugging Face model bundle
        const val DEFAULT_MODEL_BUNDLE_URL =
            "https://huggingface.co/innovatex-expense/expense-bert-tiny-multihead/resolve/main/model_bundle.zip"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
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

