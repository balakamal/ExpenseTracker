package com.innovatex.expensetracker.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "expense_tracker_settings")

enum class AiEnginePreference(val displayName: String, val sizeLabel: String) {
    GENERATIVE_LLM("Hybrid on-device classifier + SLM", "~475 MiB"),
    /** Kept only to read old preferences; the old generic BERT artifact was not fine-tuned for this task. */
    @Deprecated("Migrated to the hybrid on-device pipeline")
    FINANCIAL_TRANSFORMER("Legacy classifier (migrated)", "0 MB"),
    HEURISTIC_ONLY("Rules and Heuristics", "0 MB")
}

data class AppSettings(
    val smsTrackingEnabled: Boolean = true,
    val emailTrackingEnabled: Boolean = true,
    val defaultCurrency: String = "INR",
    val defaultMonthlyBudget: Double = 25000.0,
    val isHistoricalScanCompleted: Boolean = false,
    val deduplicationWindowMinutes: Int = 10,
    val autoCheckUpdates: Boolean = true,
    val aiClassificationEnabled: Boolean = true,
    val aiSpamDetectionEnabled: Boolean = true,
    val preferredAiEngine: AiEnginePreference = AiEnginePreference.GENERATIVE_LLM
)

@Singleton
class PreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val KEY_SMS_ENABLED = booleanPreferencesKey("key_sms_tracking_enabled")
        val KEY_EMAIL_ENABLED = booleanPreferencesKey("key_email_tracking_enabled")
        val KEY_DEFAULT_CURRENCY = stringPreferencesKey("key_default_currency")
        val KEY_DEFAULT_BUDGET = doublePreferencesKey("key_default_monthly_budget")
        val KEY_HISTORICAL_SCAN_DONE = booleanPreferencesKey("key_historical_scan_done")
        val KEY_DEDUP_WINDOW_MIN = intPreferencesKey("key_dedup_window_minutes")
        val KEY_AUTO_CHECK_UPDATES = booleanPreferencesKey("key_auto_check_updates")
        val KEY_SPAM_SENDERS = stringSetPreferencesKey("key_spam_senders")
        val KEY_TRUSTED_SENDERS = stringSetPreferencesKey("key_trusted_senders")
        val KEY_AI_CLASSIFICATION_ENABLED = booleanPreferencesKey("key_ai_classification_enabled")
        val KEY_AI_SPAM_DETECTION_ENABLED = booleanPreferencesKey("key_ai_spam_detection_enabled")
        val KEY_PREFERRED_AI_ENGINE = stringPreferencesKey("key_preferred_ai_engine")
    }

    val spamSendersFlow: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[KEY_SPAM_SENDERS] ?: emptySet()
    }

    val trustedSendersFlow: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[KEY_TRUSTED_SENDERS] ?: emptySet()
    }

    val historicalScanCompletedFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_HISTORICAL_SCAN_DONE] ?: false
    }

    suspend fun markSenderAsSpam(sender: String) {
        val normalized = sender.trim().uppercase()
        if (normalized.isBlank()) return
        dataStore.edit { preferences ->
            val currentSpam = preferences[KEY_SPAM_SENDERS] ?: emptySet()
            preferences[KEY_SPAM_SENDERS] = currentSpam + normalized
            val currentTrusted = preferences[KEY_TRUSTED_SENDERS] ?: emptySet()
            if (currentTrusted.contains(normalized)) {
                preferences[KEY_TRUSTED_SENDERS] = currentTrusted - normalized
            }
        }
    }

    suspend fun markSenderAsTrusted(sender: String) {
        val normalized = sender.trim().uppercase()
        if (normalized.isBlank()) return
        dataStore.edit { preferences ->
            val currentTrusted = preferences[KEY_TRUSTED_SENDERS] ?: emptySet()
            preferences[KEY_TRUSTED_SENDERS] = currentTrusted + normalized
            val currentSpam = preferences[KEY_SPAM_SENDERS] ?: emptySet()
            if (currentSpam.contains(normalized)) {
                preferences[KEY_SPAM_SENDERS] = currentSpam - normalized
            }
        }
    }

    suspend fun removeSenderFromSpamAndTrusted(sender: String) {
        val normalized = sender.trim().uppercase()
        if (normalized.isBlank()) return
        dataStore.edit { preferences ->
            val currentSpam = preferences[KEY_SPAM_SENDERS] ?: emptySet()
            preferences[KEY_SPAM_SENDERS] = currentSpam - normalized
            val currentTrusted = preferences[KEY_TRUSTED_SENDERS] ?: emptySet()
            preferences[KEY_TRUSTED_SENDERS] = currentTrusted - normalized
        }
    }

    suspend fun removeSenderFromSpam(sender: String) = removeSenderFromSpamAndTrusted(sender)
    suspend fun removeSenderFromTrusted(sender: String) = removeSenderFromSpamAndTrusted(sender)

    suspend fun isSpamSender(sender: String?): Boolean {
        if (sender.isNullOrBlank()) return false
        val normalized = sender.trim().uppercase()
        val spamSet = spamSendersFlow.first()
        return spamSet.contains(normalized)
    }

    suspend fun isTrustedSender(sender: String?): Boolean {
        if (sender.isNullOrBlank()) return false
        val normalized = sender.trim().uppercase()
        val trustedSet = trustedSendersFlow.first()
        return trustedSet.contains(normalized)
    }

    val appSettingsFlow: Flow<AppSettings> = dataStore.data.map { preferences ->
        AppSettings(
            smsTrackingEnabled = preferences[KEY_SMS_ENABLED] ?: true,
            emailTrackingEnabled = preferences[KEY_EMAIL_ENABLED] ?: true,
            defaultCurrency = preferences[KEY_DEFAULT_CURRENCY] ?: "INR",
            defaultMonthlyBudget = preferences[KEY_DEFAULT_BUDGET] ?: 25000.0,
            isHistoricalScanCompleted = preferences[KEY_HISTORICAL_SCAN_DONE] ?: false,
            deduplicationWindowMinutes = preferences[KEY_DEDUP_WINDOW_MIN] ?: 10,
            autoCheckUpdates = preferences[KEY_AUTO_CHECK_UPDATES] ?: true,
            aiClassificationEnabled = preferences[KEY_AI_CLASSIFICATION_ENABLED] ?: true,
            aiSpamDetectionEnabled = preferences[KEY_AI_SPAM_DETECTION_ENABLED] ?: true,
            preferredAiEngine = preferences[KEY_PREFERRED_AI_ENGINE]?.let { name ->
                runCatching { AiEnginePreference.valueOf(name) }.getOrNull()
            }?.takeUnless { it == AiEnginePreference.FINANCIAL_TRANSFORMER } ?: AiEnginePreference.GENERATIVE_LLM
        )
    }

    suspend fun setSmsTrackingEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_SMS_ENABLED] = enabled }
    }

    suspend fun setEmailTrackingEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_EMAIL_ENABLED] = enabled }
    }

    suspend fun setDefaultCurrency(currency: String) {
        dataStore.edit { it[KEY_DEFAULT_CURRENCY] = currency }
    }

    suspend fun setDefaultMonthlyBudget(budget: Double) {
        dataStore.edit { it[KEY_DEFAULT_BUDGET] = budget }
    }

    suspend fun setHistoricalScanCompleted(completed: Boolean) {
        dataStore.edit { it[KEY_HISTORICAL_SCAN_DONE] = completed }
    }

    suspend fun setDeduplicationWindowMinutes(minutes: Int) {
        dataStore.edit { it[KEY_DEDUP_WINDOW_MIN] = minutes }
    }

    suspend fun setAutoCheckUpdates(enabled: Boolean) {
        dataStore.edit { it[KEY_AUTO_CHECK_UPDATES] = enabled }
    }

    suspend fun setAiClassificationEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_AI_CLASSIFICATION_ENABLED] = enabled }
    }

    suspend fun setAiSpamDetectionEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_AI_SPAM_DETECTION_ENABLED] = enabled }
    }

    suspend fun setPreferredAiEngine(engine: AiEnginePreference) {
        dataStore.edit { it[KEY_PREFERRED_AI_ENGINE] = engine.name }
    }
}
