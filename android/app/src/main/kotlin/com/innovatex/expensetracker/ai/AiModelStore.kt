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

    // Financial Transformer (ONNX Runtime)
    val modelFile: File
        get() = File(modelDir, TRANSFORMER_MODEL_FILE_NAME)

    val vocabFile: File
        get() = File(modelDir, VOCAB_FILE_NAME)

    fun ensureDirectoryExists(): File {
        if (!modelDir.exists()) {
            modelDir.mkdirs()
        }
        return modelDir
    }

    fun isGenerativeLlmReady(): Boolean {
        if (generativeLlmFile.exists() && generativeLlmFile.length() > 1_000_000L) return true
        val legacy = File(modelDir, LEGACY_LLM_FILE_NAME)
        return legacy.exists() && legacy.length() > 1_000_000L
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
        return deleted
    }

    fun deleteTransformer(): Boolean {
        var success = true
        if (modelFile.exists()) success = success && modelFile.delete()
        if (vocabFile.exists()) success = success && vocabFile.delete()
        return success
    }

    fun deleteModel(): Boolean {
        val g = deleteGenerativeLlm()
        val t = deleteTransformer()
        return g && t
    }

    companion object {
        const val GENERATIVE_LLM_FILE_NAME = "qwen2.5_0.5b_instruct.tflite"
        const val LEGACY_LLM_FILE_NAME = "generative_llm.bin"
        const val TRANSFORMER_MODEL_FILE_NAME = "expense_classifier.onnx"
        const val VOCAB_FILE_NAME = "vocab.txt"
    }
}
