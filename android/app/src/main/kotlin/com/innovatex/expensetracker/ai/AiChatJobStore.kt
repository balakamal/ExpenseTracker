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
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class AiChatSessionRecord(
    val id: String,
    val title: String,
    val mode: String,
    val createdAt: Long,
    val updatedAt: Long
)

data class AiChatJobRecord(
    val id: String,
    val sessionId: String = AiChatJobStore.DEFAULT_SESSION_ID,
    val mode: String,
    val prompt: String,
    val submittedAt: Long,
    val isWorking: Boolean,
    val answer: String? = null,
    val thinking: String? = null,
    val error: String? = null,
    val actionProposalJson: String? = null,
    val actionStatus: String? = null
)

/** Local durable chat harness state managing multiple conversation sessions and messages. */
@Singleton
class AiChatJobStore @Inject constructor(@ApplicationContext context: Context) {
    private val store = context.dataStore
    private val jobsKey = stringPreferencesKey("ai_chat_jobs_v1")
    private val sessionsKey = stringPreferencesKey("ai_chat_sessions_v1")
    private val activeSessionKey = stringPreferencesKey("ai_active_session_v1")

    val sessions: Flow<List<AiChatSessionRecord>> = store.data.map { preferences ->
        val decoded = decodeSessions(preferences[sessionsKey].orEmpty())
        if (decoded.isEmpty()) {
            listOf(defaultSession())
        } else {
            decoded.sortedByDescending { it.updatedAt }
        }
    }

    val activeSessionId: Flow<String> = store.data.map { preferences ->
        val active = preferences[activeSessionKey]
        if (!active.isNullOrBlank()) active else DEFAULT_SESSION_ID
    }

    val jobs: Flow<List<AiChatJobRecord>> = store.data.map { preferences ->
        decodeJobs(preferences[jobsKey].orEmpty())
    }

    suspend fun createSession(mode: String, initialTitle: String? = null): String {
        val newId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val session = AiChatSessionRecord(
            id = newId,
            title = initialTitle ?: if (mode == AiChatWorker.MODE_GENERAL) "General Inquiry" else "Financial Ledger Chat",
            mode = mode,
            createdAt = now,
            updatedAt = now
        )
        store.edit { preferences ->
            val existing = decodeSessions(preferences[sessionsKey].orEmpty())
                .filterNot { it.id == newId }
            val updated = existing + session
            preferences[sessionsKey] = encodeSessions(updated)
            preferences[activeSessionKey] = newId
        }
        return newId
    }

    suspend fun setActiveSession(sessionId: String) {
        store.edit { preferences ->
            preferences[activeSessionKey] = sessionId
        }
    }

    suspend fun upsert(job: AiChatJobRecord) {
        store.edit { preferences ->
            val jobsList = decodeJobs(preferences[jobsKey].orEmpty())
                .filterNot { it.id == job.id } + job
            preferences[jobsKey] = encodeJobs(jobsList.sortedBy { it.submittedAt }.takeLast(MAX_STORED_JOBS))

            // Update session timestamp & auto-name title if first prompt
            val sessionsList = decodeSessions(preferences[sessionsKey].orEmpty()).toMutableList()
            val targetIndex = sessionsList.indexOfFirst { it.id == job.sessionId }
            val now = System.currentTimeMillis()
            if (targetIndex >= 0) {
                val current = sessionsList[targetIndex]
                val newTitle = if (current.title == "Default Conversation" || current.title.startsWith("New ") || current.title == "Financial Ledger Chat" || current.title == "General Inquiry") {
                    job.prompt.take(38).trim().ifBlank { current.title }
                } else current.title
                sessionsList[targetIndex] = current.copy(
                    updatedAt = now,
                    title = newTitle,
                    mode = job.mode
                )
                preferences[sessionsKey] = encodeSessions(sessionsList)
            } else {
                val newSession = AiChatSessionRecord(
                    id = job.sessionId,
                    title = job.prompt.take(38).trim().ifBlank { "Conversation" },
                    mode = job.mode,
                    createdAt = now,
                    updatedAt = now
                )
                preferences[sessionsKey] = encodeSessions(sessionsList + newSession)
            }
        }
    }

    suspend fun deleteSession(sessionId: String) {
        store.edit { preferences ->
            val sessionsList = decodeSessions(preferences[sessionsKey].orEmpty())
                .filterNot { it.id == sessionId }
            preferences[sessionsKey] = encodeSessions(sessionsList)

            // Remove associated jobs
            val jobsList = decodeJobs(preferences[jobsKey].orEmpty())
                .filterNot { it.sessionId == sessionId }
            preferences[jobsKey] = encodeJobs(jobsList)

            val currentActive = preferences[activeSessionKey]
            if (currentActive == sessionId) {
                val nextActive = sessionsList.maxByOrNull { it.updatedAt }?.id ?: DEFAULT_SESSION_ID
                preferences[activeSessionKey] = nextActive
            }
        }
    }

    suspend fun clearSession(sessionId: String) {
        store.edit { preferences ->
            val jobsList = decodeJobs(preferences[jobsKey].orEmpty())
                .filterNot { it.sessionId == sessionId }
            preferences[jobsKey] = encodeJobs(jobsList)
        }
    }

    private fun defaultSession() = AiChatSessionRecord(
        id = DEFAULT_SESSION_ID,
        title = "Default Conversation",
        mode = AiChatWorker.MODE_LEDGER,
        createdAt = 0L,
        updatedAt = 0L
    )

    private fun encodeSessions(sessions: List<AiChatSessionRecord>): String = JSONArray().apply {
        sessions.forEach { s ->
            put(
                JSONObject()
                    .put("id", s.id)
                    .put("title", s.title)
                    .put("mode", s.mode)
                    .put("createdAt", s.createdAt)
                    .put("updatedAt", s.updatedAt)
            )
        }
    }.toString()

    private fun decodeSessions(raw: String): List<AiChatSessionRecord> = runCatching {
        if (raw.isBlank()) return emptyList()
        val json = JSONArray(raw)
        (0 until json.length()).map { index ->
            val s = json.getJSONObject(index)
            AiChatSessionRecord(
                id = s.optString("id", DEFAULT_SESSION_ID),
                title = s.optString("title", "Conversation"),
                mode = s.optString("mode", AiChatWorker.MODE_LEDGER),
                createdAt = s.optLong("createdAt", 0L),
                updatedAt = s.optLong("updatedAt", 0L)
            )
        }
    }.getOrDefault(emptyList())

    private fun encodeJobs(jobs: List<AiChatJobRecord>): String = JSONArray().apply {
        jobs.forEach { job ->
            put(
                JSONObject()
                    .put("id", job.id)
                    .put("sessionId", job.sessionId)
                    .put("mode", job.mode)
                    .put("prompt", job.prompt)
                    .put("submittedAt", job.submittedAt)
                    .put("isWorking", job.isWorking)
                    .put("answer", job.answer)
                    .put("thinking", job.thinking)
                    .put("error", job.error)
                    .put("actionProposalJson", job.actionProposalJson)
                    .put("actionStatus", job.actionStatus)
            )
        }
    }.toString()

    private fun decodeJobs(raw: String): List<AiChatJobRecord> = runCatching {
        if (raw.isBlank()) return emptyList()
        val json = JSONArray(raw)
        (0 until json.length()).map { index ->
            val job = json.getJSONObject(index)
            AiChatJobRecord(
                id = job.optString("id"),
                sessionId = job.optString("sessionId").ifBlank { DEFAULT_SESSION_ID },
                mode = job.optString("mode"),
                prompt = job.optString("prompt"),
                submittedAt = job.optLong("submittedAt"),
                isWorking = job.optBoolean("isWorking"),
                answer = job.optString("answer").takeIf { it.isNotBlank() && it != "null" },
                thinking = job.optString("thinking").takeIf { it.isNotBlank() && it != "null" },
                error = job.optString("error").takeIf { it.isNotBlank() && it != "null" },
                actionProposalJson = job.optString("actionProposalJson").takeIf { it.isNotBlank() && it != "null" },
                actionStatus = job.optString("actionStatus").takeIf { it.isNotBlank() && it != "null" }
            )
        }
    }.getOrDefault(emptyList())

    companion object {
        const val DEFAULT_SESSION_ID = "default_session"
        private const val MAX_STORED_JOBS = 120
    }
}
