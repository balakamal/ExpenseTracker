package com.innovatex.expensetracker.ai

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiModelStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val modelDir = File(context.filesDir, "ai_models")

    val generativeLlmFile: File get() = File(modelDir, GENERATIVE_LLM_FILE_NAME)
    val generativeLlmPartFile: File get() = File(modelDir, GENERATIVE_LLM_PART_FILE_NAME)
    val generativeLlmMetadataFile: File get() = File(modelDir, GENERATIVE_LLM_METADATA_FILE_NAME)

    fun ensureDirectoryExists(): File {
        if (!modelDir.exists() && !modelDir.mkdirs()) {
            throw IllegalStateException("Unable to create private model storage")
        }
        return modelDir
    }

    /** Validates the pinned LiteRT-LM artifact using its repository LFS SHA-256. */
    fun isValidLiteRtModel(file: File, expectedSha256: String? = null): Boolean {
        if (!file.isFile || file.length() < MIN_MODEL_BYTES) return false
        return try {
            val checksum = sha256(file)
            val metadataHash = generativeLlmMetadataFile.takeIf(File::isFile)?.readText()?.substringBefore('|')?.trim()
            val expected = expectedSha256?.takeIf { it.matches(SHA256_REGEX) } ?: metadataHash
            expected != null && checksum.equals(expected, ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }

    /** Fast readiness check: full hashing happens once at install, never on the SMS ingestion path. */
    fun isGenerativeLlmReady(): Boolean {
        val file = generativeLlmFile
        if (!file.isFile || file.length() < MIN_MODEL_BYTES || !generativeLlmMetadataFile.isFile) return false
        val metadata = runCatching { generativeLlmMetadataFile.readText().trim().split('|') }.getOrNull() ?: return false
        return metadata.size == 3 && metadata[0].equals(PINNED_MODEL_SHA256, ignoreCase = true) &&
            file.length() == PINNED_MODEL_SIZE_BYTES && metadata[1].toLongOrNull() == file.length() &&
            metadata[2].toLongOrNull() == file.lastModified()
    }
    fun isModelReady(): Boolean = isGenerativeLlmReady()
    fun getGenerativeLlmSizeBytes(): Long = generativeLlmFile.takeIf(File::exists)?.length() ?: 0L
    fun getModelSizeBytes(): Long = getGenerativeLlmSizeBytes()

    fun deleteGenerativeLlm(): Boolean = listOf(
        generativeLlmFile, generativeLlmPartFile, generativeLlmMetadataFile,
        File(modelDir, LEGACY_LLM_FILE_NAME), File(modelDir, "$LEGACY_LLM_FILE_NAME.part"),
        File(modelDir, LEGACY_TASK_FILE_NAME), File(modelDir, "$LEGACY_TASK_FILE_NAME.part")
    ).all { !it.exists() || it.delete() }

    /** Removes artifacts created by the incompatible MediaPipe bundle and untrained BERT path. */
    fun purgeIncompatibleModels(): Boolean {
        val staleFiles = listOf(
            File(modelDir, LEGACY_LLM_FILE_NAME),
            File(modelDir, "$LEGACY_LLM_FILE_NAME.part"),
            File(modelDir, LEGACY_TASK_FILE_NAME),
            File(modelDir, "$LEGACY_TASK_FILE_NAME.part"),
            File(modelDir, "qwen2.5_0.5b_instruct.tflite"),
            File(modelDir, LEGACY_ONNX_FILE_NAME),
            File(modelDir, "$LEGACY_ONNX_FILE_NAME.part"),
            File(modelDir, LEGACY_VOCAB_FILE_NAME),
            File(modelDir, "$LEGACY_VOCAB_FILE_NAME.part")
        )
        return staleFiles.fold(false) { removed, file -> (file.exists() && file.delete()) || removed }
    }
    fun deleteModel(): Boolean = deleteGenerativeLlm()

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val GENERATIVE_LLM_FILE_NAME = "qwen3_0_6b_mixed_int4.litertlm"
        const val GENERATIVE_LLM_PART_FILE_NAME = "$GENERATIVE_LLM_FILE_NAME.part"
        const val GENERATIVE_LLM_METADATA_FILE_NAME = "$GENERATIVE_LLM_FILE_NAME.sha256"
        const val PINNED_MODEL_SHA256 = "7900eb4e7362d88c58782c6f9999bb7a129e03544aa98b8f338ea0cc5d8c22c1"
        const val PINNED_MODEL_SIZE_BYTES = 497_516_544L
        const val LEGACY_LLM_FILE_NAME = "generative_llm.bin"
        const val LEGACY_TASK_FILE_NAME = "qwen2.5_0.5b_instruct.task"
        private const val LEGACY_ONNX_FILE_NAME = "expense_classifier.onnx"
        private const val LEGACY_VOCAB_FILE_NAME = "vocab.txt"
        private const val MIN_MODEL_BYTES = 10_000_000L
        private val SHA256_REGEX = Regex("[a-fA-F0-9]{64}")
    }
}
