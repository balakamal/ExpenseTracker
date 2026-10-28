package com.innovatex.expensetracker.ai

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiModelStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val modelDir = File(context.filesDir, "ai_models")

    val modelFile: File
        get() = File(modelDir, MODEL_FILE_NAME)

    val vocabFile: File
        get() = File(modelDir, VOCAB_FILE_NAME)

    fun ensureDirectoryExists(): File {
        if (!modelDir.exists()) {
            modelDir.mkdirs()
        }
        return modelDir
    }

    fun isModelReady(): Boolean {
        return modelFile.exists() && modelFile.length() > 0 &&
            vocabFile.exists() && vocabFile.length() > 0
    }

    fun getModelSizeBytes(): Long {
        var size = 0L
        if (modelFile.exists()) size += modelFile.length()
        if (vocabFile.exists()) size += vocabFile.length()
        return size
    }

    fun deleteModel(): Boolean {
        var success = true
        if (modelFile.exists()) {
            success = success && modelFile.delete()
        }
        if (vocabFile.exists()) {
            success = success && vocabFile.delete()
        }
        return success
    }

    companion object {
        const val MODEL_FILE_NAME = "expense_classifier.onnx"
        const val VOCAB_FILE_NAME = "vocab.txt"
    }
}
