package com.innovatex.expensetracker.ai

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.innovatex.expensetracker.data.local.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class AiChatJobRecord(
    val id: String,
    val mode: String,
    val prompt: String,
    val submittedAt: Long,
    val isWorking: Boolean,
    val answer: String? = null,
    val error: String? = null,
    val actionProposalJson: String? = null,
    val actionStatus: String? = null
)

/** Local durable chat state so pending prompts and completed replies survive process recreation. */
@Singleton
class AiChatJobStore @Inject constructor(@ApplicationContext context: Context) {
    private val store = context.dataStore
    private val jobsKey = stringPreferencesKey("ai_chat_jobs_v1")

    val jobs: Flow<List<AiChatJobRecord>> = store.data.map { preferences -> decode(preferences[jobsKey].orEmpty()) }

    suspend fun upsert(job: AiChatJobRecord) {
        store.edit { preferences ->
            val updated = decode(preferences[jobsKey].orEmpty())
                .filterNot { it.id == job.id } + job
            preferences[jobsKey] = encode(updated.sortedBy { it.submittedAt }.takeLast(MAX_STORED_JOBS))
        }
    }

    private fun encode(jobs: List<AiChatJobRecord>): String = JSONArray().apply {
        jobs.forEach { job ->
            put(
                JSONObject()
                    .put("id", job.id)
                    .put("mode", job.mode)
                    .put("prompt", job.prompt)
                    .put("submittedAt", job.submittedAt)
                    .put("isWorking", job.isWorking)
                    .put("answer", job.answer)
                    .put("error", job.error)
                    .put("actionProposalJson", job.actionProposalJson)
                    .put("actionStatus", job.actionStatus)
            )
        }
    }.toString()

    private fun decode(raw: String): List<AiChatJobRecord> = runCatching {
        val json = JSONArray(raw)
        (0 until json.length()).map { index ->
            val job = json.getJSONObject(index)
            AiChatJobRecord(
                id = job.optString("id"),
                mode = job.optString("mode"),
                prompt = job.optString("prompt"),
                submittedAt = job.optLong("submittedAt"),
                isWorking = job.optBoolean("isWorking"),
                answer = job.optString("answer").takeIf { it.isNotBlank() && it != "null" },
                error = job.optString("error").takeIf { it.isNotBlank() && it != "null" },
                actionProposalJson = job.optString("actionProposalJson").takeIf { it.isNotBlank() && it != "null" },
                actionStatus = job.optString("actionStatus").takeIf { it.isNotBlank() && it != "null" }
            )
        }
    }.getOrDefault(emptyList())

    private companion object {
        const val MAX_STORED_JOBS = 80
    }
}
