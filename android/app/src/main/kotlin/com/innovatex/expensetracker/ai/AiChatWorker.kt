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
    private val preferences: PreferencesDataStore,
    private val chatJobStore: AiChatJobStore
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prompt = inputData.getString(KEY_PROMPT).orEmpty()
        val mode = inputData.getString(KEY_MODE).orEmpty()
        val sessionId = inputData.getString(KEY_SESSION_ID).orEmpty().ifBlank { AiChatJobStore.DEFAULT_SESSION_ID }
        val submittedAt = inputData.getLong(KEY_SUBMITTED_AT, System.currentTimeMillis())
        val jobId = id.toString()
        if (prompt.isBlank()) return Result.failure(workDataOf(KEY_ERROR to "Enter a question to start."))

        val baseJob = AiChatJobRecord(
            id = jobId,
            sessionId = sessionId,
            mode = mode,
            prompt = inputData.getString(KEY_DISPLAY_PROMPT).orEmpty(),
            submittedAt = submittedAt,
            isWorking = true
        )
        return try {
            chatJobStore.upsert(baseJob)
            val ledger = inputData.getString(KEY_LEDGER)
            val response = if (mode == MODE_LEDGER) {
                llmEngine.answerLedgerQuestion(prompt, ledger.orEmpty())
            } else {
                llmEngine.answerGeneralChat(prompt)
            }
            if (response == null || response.answer.isBlank()) {
                val error = "The on-device AI model is not ready. Download it in Settings and try again."
                chatJobStore.upsert(baseJob.copy(isWorking = false, error = error))
                return Result.failure(workDataOf(KEY_ERROR to error))
            }
            val actionJson = response.action?.let { action ->
                org.json.JSONObject()
                    .put("type", action.type)
                    .apply {
                        action.merchant?.let { put("merchant", it) }
                        action.categoryId?.let { put("categoryId", it) }
                        action.transactionId?.let { put("transactionId", it) }
                        action.amount?.let { put("amount", it) }
                        action.notes?.let { put("notes", it) }
                        put("reason", action.reason)
                    }
                    .toString()
            }
            chatJobStore.upsert(
                baseJob.copy(
                    isWorking = false,
                    answer = response.answer,
                    thinking = response.thinking,
                    actionProposalJson = actionJson
                )
            )
            val output = workDataOf(
                KEY_PROMPT to inputData.getString(KEY_DISPLAY_PROMPT).orEmpty(),
                KEY_MODE to mode,
                KEY_SESSION_ID to sessionId,
                KEY_ANSWER to response.answer,
                KEY_THINKING to (response.thinking ?: ""),
                KEY_ACTION_PROPOSAL to (actionJson ?: ""),
                KEY_SUBMITTED_AT to submittedAt
            )
            notifyCompletionIfAllowed()
            Result.success(output)
        } catch (cancelled: CancellationException) {
            runCatching { chatJobStore.upsert(baseJob.copy(isWorking = false, error = "This AI request was interrupted. You can send it again.")) }
            throw cancelled
        } catch (error: Exception) {
            val message = error.message ?: "AI could not generate a response."
            runCatching { chatJobStore.upsert(baseJob.copy(isWorking = false, error = message)) }
            Result.failure(workDataOf(KEY_ERROR to message))
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
        private const val CHANNEL_ID = "ai_chat_response_channel"
        const val WORK_TAG = "ai_chat_response"
        const val KEY_PROMPT = "prompt"
        const val KEY_DISPLAY_PROMPT = "display_prompt"
        const val KEY_LEDGER = "ledger"
        const val KEY_MODE = "mode"
        const val KEY_SESSION_ID = "session_id"
        const val KEY_ANSWER = "answer"
        const val KEY_THINKING = "thinking"
        const val KEY_ACTION_PROPOSAL = "action_proposal"
        const val KEY_ERROR = "error"
        const val KEY_SUBMITTED_AT = "submitted_at"
        const val MODE_LEDGER = "financial_ledger"
        const val MODE_GENERAL = "general"
        const val MAX_PROMPT_CHARS = 1200
        const val MAX_CONTEXT_CHARS = 3500

        suspend fun enqueue(
            context: Context,
            prompt: String,
            displayPrompt: String,
            mode: String,
            ledger: String?,
            sessionId: String = AiChatJobStore.DEFAULT_SESSION_ID,
            chatJobStore: AiChatJobStore
        ) {
            val submittedAt = System.currentTimeMillis()
            val data = Data.Builder()
                .putString(KEY_PROMPT, prompt.take(MAX_PROMPT_CHARS))
                .putString(KEY_DISPLAY_PROMPT, displayPrompt.take(500))
                .putString(KEY_MODE, mode)
                .putString(KEY_SESSION_ID, sessionId)
                .putLong(KEY_SUBMITTED_AT, submittedAt)
                .apply { if (!ledger.isNullOrBlank()) putString(KEY_LEDGER, ledger.take(MAX_CONTEXT_CHARS)) }
                .build()
            val request = OneTimeWorkRequestBuilder<AiChatWorker>()
                .setInputData(data)
                .addTag(WORK_TAG)
                .build()
            val queuedJob = AiChatJobRecord(
                id = request.id.toString(),
                sessionId = sessionId,
                mode = mode,
                prompt = displayPrompt.take(500),
                submittedAt = submittedAt,
                isWorking = true
            )
            chatJobStore.upsert(queuedJob)
            try {
                WorkManager.getInstance(context).enqueue(request)
            } catch (error: Exception) {
                chatJobStore.upsert(queuedJob.copy(isWorking = false, error = error.message ?: "Could not queue the AI request."))
            }
        }
    }
}
