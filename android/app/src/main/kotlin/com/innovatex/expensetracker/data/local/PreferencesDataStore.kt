package com.innovatex.expensetracker.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "expense_tracker_settings")

data class AppSettings(
    val smsTrackingEnabled: Boolean = true,
    val emailTrackingEnabled: Boolean = true,
    val defaultCurrency: String = "INR",
    val defaultMonthlyBudget: Double = 25000.0,
    val isHistoricalScanCompleted: Boolean = false,
    val deduplicationWindowMinutes: Int = 10,
    val autoCheckUpdates: Boolean = true
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
    }

    val appSettingsFlow: Flow<AppSettings> = dataStore.data.map { preferences ->
        AppSettings(
            smsTrackingEnabled = preferences[KEY_SMS_ENABLED] ?: true,
            emailTrackingEnabled = preferences[KEY_EMAIL_ENABLED] ?: true,
            defaultCurrency = preferences[KEY_DEFAULT_CURRENCY] ?: "INR",
            defaultMonthlyBudget = preferences[KEY_DEFAULT_BUDGET] ?: 25000.0,
            isHistoricalScanCompleted = preferences[KEY_HISTORICAL_SCAN_DONE] ?: false,
            deduplicationWindowMinutes = preferences[KEY_DEDUP_WINDOW_MIN] ?: 10,
            autoCheckUpdates = preferences[KEY_AUTO_CHECK_UPDATES] ?: true
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
}
