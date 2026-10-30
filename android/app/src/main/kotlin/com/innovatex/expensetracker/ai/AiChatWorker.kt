package com.innovatex.expensetracker.ai

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.innovatex.expensetracker.MainActivity
import com.innovatex.expensetracker.data.local.PreferencesDataStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/** Runs a user-requested local LLM response independently of the dashboard lifecycle. */
@HiltWorker
class AiChatWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val llmEngine: LlmClassifierEngine,
    private val preferences: PreferencesDataStore
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prompt = inputData.getString(KEY_PROMPT).orEmpty()
        val mode = inputData.getString(KEY_MODE).orEmpty()
        if (prompt.isBlank()) return Result.failure(workDataOf(KEY_ERROR to "Enter a question to start."))

        return try {
            val ledger = inputData.getString(KEY_LEDGER)
            val answer = llmEngine.answerChat(prompt, ledger, mode == MODE_LEDGER)
                ?: return Result.failure(workDataOf(KEY_ERROR to "The on-device AI model is not ready. Download it in Settings and try again."))
            val output = workDataOf(
                KEY_PROMPT to inputData.getString(KEY_DISPLAY_PROMPT).orEmpty(),
                KEY_MODE to mode,
                KEY_ANSWER to answer,
                KEY_SUBMITTED_AT to inputData.getLong(KEY_SUBMITTED_AT, System.currentTimeMillis())
            )
            notifyCompletionIfAllowed()
            Result.success(output)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(workDataOf(KEY_ERROR to (error.message ?: "AI could not generate a response.")))
        }
    }

    private suspend fun notifyCompletionIfAllowed() {
        if (!preferences.appSettingsFlow.first().backgroundAiNotificationsEnabled) return
        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "AI responses", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_OPEN_AI_TAB, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Your AI response is ready")
            .setContentText("Tap to continue your conversation.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        runCatching { NotificationManagerCompat.from(applicationContext).notify(id.hashCode(), notification) }
    }

    companion object {
        const val WORK_TAG = "ai_chat_response"
        const val KEY_PROMPT = "prompt"
        const val KEY_DISPLAY_PROMPT = "display_prompt"
        const val KEY_LEDGER = "ledger"
        const val KEY_MODE = "mode"
        const val KEY_ANSWER = "answer"
        const val KEY_ERROR = "error"
        const val KEY_SUBMITTED_AT = "submitted_at"
        const val MODE_LEDGER = "financial_ledger"
        const val MODE_GENERAL = "general"
        const val MAX_PROMPT_CHARS = 900
        const val MAX_CONTEXT_CHARS = 700

        fun enqueue(context: Context, prompt: String, displayPrompt: String, mode: String, ledger: String?) {
            val data = Data.Builder()
                .putString(KEY_PROMPT, prompt.take(MAX_PROMPT_CHARS))
                .putString(KEY_DISPLAY_PROMPT, displayPrompt.take(500))
                .putString(KEY_MODE, mode)
                .putLong(KEY_SUBMITTED_AT, System.currentTimeMillis())
                .apply { if (!ledger.isNullOrBlank()) putString(KEY_LEDGER, ledger.take(MAX_CONTEXT_CHARS)) }
                .build()
            val request = OneTimeWorkRequestBuilder<AiChatWorker>()
                .setInputData(data)
                .addTag(WORK_TAG)
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
