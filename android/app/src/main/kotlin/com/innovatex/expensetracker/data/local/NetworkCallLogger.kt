package com.innovatex.expensetracker.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

data class NetworkCallLog(val timestamp: Long, val method: String, val endpoint: String, val status: String, val durationMs: Long)

/** Stores request metadata only. URL query strings, headers, bodies and response contents are excluded. */
@Singleton
class NetworkCallLogger @Inject constructor(@ApplicationContext private val context: Context) {
    private val key = stringPreferencesKey("network_call_log_v1")

    val logs: Flow<List<NetworkCallLog>> = context.dataStore.data.map { prefs -> decode(prefs[key].orEmpty()) }

    suspend fun record(method: String, url: String, status: String, durationMs: Long) {
        val uri = runCatching { URI(url) }.getOrNull()
        val endpoint = listOfNotNull(uri?.host, uri?.path?.takeIf { it.isNotBlank() }).joinToString("").ifBlank { "unknown endpoint" }
        val item = NetworkCallLog(System.currentTimeMillis(), method.take(10), endpoint.take(180), status.take(24), durationMs.coerceAtLeast(0))
        context.dataStore.edit { prefs ->
            val recent = decode(prefs[key].orEmpty()).take(99)
            prefs[key] = encode(listOf(item) + recent)
        }
    }

    suspend fun clear() { context.dataStore.edit { it.remove(key) } }

    private fun encode(items: List<NetworkCallLog>) = JSONArray().apply {
        items.forEach { item -> put(JSONObject().put("time", item.timestamp).put("method", item.method).put("endpoint", item.endpoint).put("status", item.status).put("duration", item.durationMs)) }
    }.toString()

    private fun decode(raw: String): List<NetworkCallLog> = runCatching {
        val json = JSONArray(raw)
        (0 until json.length()).map { index ->
            val item = json.getJSONObject(index)
            NetworkCallLog(item.optLong("time"), item.optString("method"), item.optString("endpoint"), item.optString("status"), item.optLong("duration"))
        }
    }.getOrDefault(emptyList())
}
