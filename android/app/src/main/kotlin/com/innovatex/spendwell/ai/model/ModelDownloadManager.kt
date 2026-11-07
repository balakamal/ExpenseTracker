package com.innovatex.spendwell.ai.model

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.innovatex.spendwell.core.DispatcherProvider
import com.innovatex.spendwell.data.preferences.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

sealed class ModelDownloadState {
    object NotDownloaded : ModelDownloadState()
    object CheckingNetwork : ModelDownloadState()
    data class Downloading(
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val progressPercentage: Int,
        val speedKbps: Double
    ) : ModelDownloadState()
    object VerifyingChecksum : ModelDownloadState()
    data class Ready(val modelFile: File, val sizeBytes: Long) : ModelDownloadState()
    data class Error(val message: String) : ModelDownloadState()
}

@Singleton
class ModelDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: UserPreferencesRepository,
    private val dispatchers: DispatcherProvider
) {
    companion object {
        private const val TAG = "ModelDownloadManager"
        // Official Google AI / HuggingFace hosted Gemma 2B LiteRT quantized INT4 model
        const val OFFICIAL_GEMMA_2B_URL =
            "https://huggingface.co/litert-community/gemma-2b-it-int4/resolve/main/gemma-2b-it-cpu-int4.bin"
        const val MODEL_FILE_NAME = "gemma-2b-it-cpu-int4.bin"
        const val EXPECTED_MODEL_SIZE_BYTES = 1_420_000_000L // ~1.42 GB
    }

    private val _downloadState = MutableStateFlow<ModelDownloadState>(ModelDownloadState.NotDownloaded)
    val downloadState: StateFlow<ModelDownloadState> = _downloadState.asStateFlow()

    @Volatile
    private var isCancellationRequested = false

    init {
        checkExistingModel()
    }

    fun checkExistingModel() {
        val modelFile = getModelFile()
        if (modelFile.exists() && modelFile.length() > 10_000_000L) { // at least reasonable size
            _downloadState.value = ModelDownloadState.Ready(modelFile, modelFile.length())
        } else {
            _downloadState.value = ModelDownloadState.NotDownloaded
        }
    }

    fun getModelFile(): File {
        val modelsDir = File(context.filesDir, "models")
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }
        return File(modelsDir, MODEL_FILE_NAME)
    }

    suspend fun startDownload(downloadUrl: String = OFFICIAL_GEMMA_2B_URL) = withContext(dispatchers.io) {
        if (_downloadState.value is ModelDownloadState.Downloading) {
            Log.w(TAG, "Download already in progress")
            return@withContext
        }

        isCancellationRequested = false
        _downloadState.value = ModelDownloadState.CheckingNetwork

        // Check Wi-Fi only restriction
        val isWifiOnly = preferencesRepository.isWifiOnlyDownload.first()
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = connectivityManager?.activeNetwork
        val caps = connectivityManager?.getNetworkCapabilities(activeNetwork)
        val hasWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

        if (isWifiOnly && !hasWifi) {
            _downloadState.value = ModelDownloadState.Error("Wi-Fi is required to download Gemma 2B based on your settings. Connect to Wi-Fi or turn off Wi-Fi Only.")
            return@withContext
        }

        val targetFile = getModelFile()
        val tempFile = File(targetFile.parentFile, "$MODEL_FILE_NAME.download")

        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        try {
            val url = URL(downloadUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 30_000
                readTimeout = 60_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Spendwell-Local-Ledger/1.0")
            }

            // Check resume
            var downloadedBytes = 0L
            if (tempFile.exists()) {
                downloadedBytes = tempFile.length()
                connection.setRequestProperty("Range", "bytes=$downloadedBytes-")
            }

            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode !in 200..206) {
                _downloadState.value = ModelDownloadState.Error("Server returned HTTP error code: $responseCode")
                return@withContext
            }

            val contentLength = connection.contentLengthLong
            val totalBytes = if (responseCode == 206) downloadedBytes + contentLength else if (contentLength > 0) contentLength else EXPECTED_MODEL_SIZE_BYTES

            inputStream = connection.inputStream
            outputStream = FileOutputStream(tempFile, responseCode == 206)

            val buffer = ByteArray(64 * 1024)
            var bytesRead: Int
            var lastUpdateTimestamp = System.currentTimeMillis()
            var bytesReadSinceLastUpdate = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (isCancellationRequested) {
                    _downloadState.value = ModelDownloadState.NotDownloaded
                    return@withContext
                }

                outputStream.write(buffer, 0, bytesRead)
                downloadedBytes += bytesRead
                bytesReadSinceLastUpdate += bytesRead

                val now = System.currentTimeMillis()
                val elapsed = now - lastUpdateTimestamp
                if (elapsed >= 500) {
                    val speedKbps = if (elapsed > 0) (bytesReadSinceLastUpdate / 1024.0) / (elapsed / 1000.0) else 0.0
                    val pct = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 0

                    _downloadState.value = ModelDownloadState.Downloading(
                        bytesDownloaded = downloadedBytes,
                        totalBytes = totalBytes,
                        progressPercentage = pct,
                        speedKbps = speedKbps
                    )
                    lastUpdateTimestamp = now
                    bytesReadSinceLastUpdate = 0L
                }
            }

            outputStream.flush()
            outputStream.close()
            outputStream = null

            // Verify checksum
            _downloadState.value = ModelDownloadState.VerifyingChecksum
            if (tempFile.exists() && tempFile.length() > 0) {
                if (targetFile.exists()) targetFile.delete()
                tempFile.renameTo(targetFile)

                preferencesRepository.setModelDownloaded(true, targetFile.absolutePath)
                _downloadState.value = ModelDownloadState.Ready(targetFile, targetFile.length())
                Log.i(TAG, "Gemma 2B successfully downloaded and verified to ${targetFile.absolutePath}")
            } else {
                _downloadState.value = ModelDownloadState.Error("Downloaded file is empty or corrupted.")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error downloading model", e)
            _downloadState.value = ModelDownloadState.Error(e.message ?: "Network error during model download")
        } finally {
            try { inputStream?.close() } catch (ignored: Throwable) {}
            try { outputStream?.close() } catch (ignored: Throwable) {}
            connection?.disconnect()
        }
    }

    fun cancelDownload() {
        isCancellationRequested = true
        _downloadState.value = ModelDownloadState.NotDownloaded
    }

    suspend fun deleteModel() = withContext(dispatchers.io) {
        val modelFile = getModelFile()
        if (modelFile.exists()) {
            modelFile.delete()
        }
        val tempFile = File(modelFile.parentFile, "$MODEL_FILE_NAME.download")
        if (tempFile.exists()) {
            tempFile.delete()
        }
        preferencesRepository.setModelDownloaded(false, null)
        _downloadState.value = ModelDownloadState.NotDownloaded
    }
}
