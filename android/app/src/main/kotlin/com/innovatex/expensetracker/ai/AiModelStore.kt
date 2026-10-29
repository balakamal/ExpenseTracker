package com.innovatex.expensetracker.ai

import android.content.Context
import com.innovatex.expensetracker.data.local.AiEnginePreference
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiModelStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val modelDir = File(context.filesDir, "ai_models")

    // Generative LLM (MediaPipe Tasks-GenAI)
    val generativeLlmFile: File
        get() {
            val file = File(modelDir, GENERATIVE_LLM_FILE_NAME)
            if (file.exists()) return file
            val legacy = File(modelDir, LEGACY_LLM_FILE_NAME)
            return if (legacy.exists()) legacy else file
        }

    val generativeLlmPartFile: File
        get() = File(modelDir, GENERATIVE_LLM_PART_FILE_NAME)

    // Financial Transformer (ONNX Runtime)
    val modelFile: File
        get() = File(modelDir, TRANSFORMER_MODEL_FILE_NAME)

    val modelPartFile: File
        get() = File(modelDir, TRANSFORMER_MODEL_PART_FILE_NAME)

    val vocabFile: File
        get() = File(modelDir, VOCAB_FILE_NAME)

    val vocabPartFile: File
        get() = File(modelDir, VOCAB_PART_FILE_NAME)

    fun ensureDirectoryExists(): File {
        if (!modelDir.exists()) {
            modelDir.mkdirs()
        }
        return modelDir
    }

    fun isValidMediaPipeBundle(file: File): Boolean {
        if (!file.exists() || file.length() < 1_000_000L) return false
        return try {
            // Step 1: Fast header check for ZIP container magic bytes (PK\x03\x04)
            val hasZipMagic = java.io.RandomAccessFile(file, "r").use { raf ->
                val magic = ByteArray(4)
                raf.readFully(magic)
                magic[0] == 0x50.toByte() && magic[1] == 0x4B.toByte() &&
                    magic[2] == 0x03.toByte() && magic[3] == 0x04.toByte()
            }
            if (!hasZipMagic) return false

            // Step 2: Validate complete ZIP archive structure (Central Directory at end of file)
            // If the download was truncated before completion, ZipFile constructor will throw ZipException
            java.util.zip.ZipFile(file).use { zip ->
                zip.entries().hasMoreElements()
            }
        } catch (_: Exception) {
            false
        }
    }

    fun purgeIncompatibleModels(): Boolean {
        var purged = false
        // Purge bare unbundled tflite flatbuffers that lack MediaPipe task metadata
        val badTflite = File(modelDir, "qwen2.5_0.5b_instruct.tflite")
        if (badTflite.exists()) {
            badTflite.delete()
            purged = true
        }
        if (generativeLlmFile.exists() && !isValidMediaPipeBundle(generativeLlmFile)) {
            deleteGenerativeLlm()
            purged = true
        }
        val legacy = File(modelDir, LEGACY_LLM_FILE_NAME)
        if (legacy.exists() && !isValidMediaPipeBundle(legacy)) {
            legacy.delete()
            purged = true
        }
        return purged
    }

    fun isGenerativeLlmReady(): Boolean {
        if (generativeLlmFile.exists() && isValidMediaPipeBundle(generativeLlmFile)) return true
        val legacy = File(modelDir, LEGACY_LLM_FILE_NAME)
        return legacy.exists() && isValidMediaPipeBundle(legacy)
    }

    fun isTransformerReady(): Boolean {
        return modelFile.exists() && modelFile.length() > 0 &&
            vocabFile.exists() && vocabFile.length() > 0
    }

    fun isEngineReady(engine: AiEnginePreference): Boolean {
        return when (engine) {
            AiEnginePreference.GENERATIVE_LLM -> isGenerativeLlmReady()
            AiEnginePreference.FINANCIAL_TRANSFORMER -> isTransformerReady()
            AiEnginePreference.HEURISTIC_ONLY -> true
        }
    }

    fun isModelReady(): Boolean {
        return isGenerativeLlmReady() || isTransformerReady()
    }

    fun getGenerativeLlmSizeBytes(): Long {
        return if (generativeLlmFile.exists()) generativeLlmFile.length() else 0L
    }

    fun getTransformerSizeBytes(): Long {
        var size = 0L
        if (modelFile.exists()) size += modelFile.length()
        if (vocabFile.exists()) size += vocabFile.length()
        return size
    }

    fun getModelSizeBytes(engine: AiEnginePreference): Long {
        return when (engine) {
            AiEnginePreference.GENERATIVE_LLM -> getGenerativeLlmSizeBytes()
            AiEnginePreference.FINANCIAL_TRANSFORMER -> getTransformerSizeBytes()
            AiEnginePreference.HEURISTIC_ONLY -> 0L
        }
    }

    fun getModelSizeBytes(): Long {
        return getGenerativeLlmSizeBytes() + getTransformerSizeBytes()
    }

    fun deleteGenerativeLlm(): Boolean {
        var deleted = if (generativeLlmFile.exists()) generativeLlmFile.delete() else true
        val legacy = File(modelDir, LEGACY_LLM_FILE_NAME)
        if (legacy.exists()) deleted = deleted && legacy.delete()
        val tflite = File(modelDir, "qwen2.5_0.5b_instruct.tflite")
        if (tflite.exists()) deleted = deleted && tflite.delete()
        if (generativeLlmPartFile.exists()) deleted = deleted && generativeLlmPartFile.delete()
        return deleted
    }

    fun deleteTransformer(): Boolean {
        var success = true
        if (modelFile.exists()) success = success && modelFile.delete()
        if (vocabFile.exists()) success = success && vocabFile.delete()
        if (modelPartFile.exists()) success = success && modelPartFile.delete()
        if (vocabPartFile.exists()) success = success && vocabPartFile.delete()
        return success
    }

    fun deleteModel(): Boolean {
        val g = deleteGenerativeLlm()
        val t = deleteTransformer()
        return g && t
    }

    companion object {
        const val GENERATIVE_LLM_FILE_NAME = "qwen2.5_0.5b_instruct.task"
        const val GENERATIVE_LLM_PART_FILE_NAME = "qwen2.5_0.5b_instruct.task.part"
        const val LEGACY_LLM_FILE_NAME = "generative_llm.bin"
        const val TRANSFORMER_MODEL_FILE_NAME = "expense_classifier.onnx"
        const val TRANSFORMER_MODEL_PART_FILE_NAME = "expense_classifier.onnx.part"
        const val MODEL_FILE_NAME = TRANSFORMER_MODEL_FILE_NAME
        const val VOCAB_FILE_NAME = "vocab.txt"
        const val VOCAB_PART_FILE_NAME = "vocab.txt.part"
    }
}
