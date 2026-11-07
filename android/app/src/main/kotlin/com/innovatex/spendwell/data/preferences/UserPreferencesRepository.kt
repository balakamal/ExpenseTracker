package com.innovatex.spendwell.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "spendwell_preferences")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_IS_PRIVATE_MODE = booleanPreferencesKey("is_private_mode")
        val KEY_CYCLE_START_DAY = intPreferencesKey("cycle_start_day") // 1 to 31, -1 for last day, -2 for last working day
        val KEY_MODEL_DOWNLOADED = booleanPreferencesKey("model_downloaded")
        val KEY_MODEL_FILE_PATH = stringPreferencesKey("model_file_path")
        val KEY_WIFI_ONLY_DOWNLOAD = booleanPreferencesKey("wifi_only_download")
        val KEY_SYNCED_MONTHS = stringSetPreferencesKey("synced_months")
        val KEY_HF_API_TOKEN = stringPreferencesKey("hf_api_token")
        val KEY_CUSTOM_MODEL_URL = stringPreferencesKey("custom_model_url")
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_ONBOARDING_COMPLETED] ?: false
    }

    val isPrivateMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_IS_PRIVATE_MODE] ?: true
    }

    val cycleStartDay: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_CYCLE_START_DAY] ?: 1
    }

    val isModelDownloaded: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_MODEL_DOWNLOADED] ?: false
    }

    val modelFilePath: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_MODEL_FILE_PATH]
    }

    val isWifiOnlyDownload: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_WIFI_ONLY_DOWNLOAD] ?: true
    }

    val syncedMonths: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[KEY_SYNCED_MONTHS] ?: emptySet()
    }

    val hfApiToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_HF_API_TOKEN]
    }

    val customModelUrl: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_CUSTOM_MODEL_URL]
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setPrivateMode(isPrivate: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_PRIVATE_MODE] = isPrivate
        }
    }

    suspend fun setCycleStartDay(day: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CYCLE_START_DAY] = day.coerceIn(-2, 31)
        }
    }

    suspend fun setModelDownloaded(downloaded: Boolean, path: String? = null) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MODEL_DOWNLOADED] = downloaded
            if (path != null) {
                preferences[KEY_MODEL_FILE_PATH] = path
            }
        }
    }

    suspend fun setWifiOnlyDownload(wifiOnly: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_WIFI_ONLY_DOWNLOAD] = wifiOnly
        }
    }

    suspend fun addSyncedMonth(monthKey: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_SYNCED_MONTHS] ?: emptySet()
            preferences[KEY_SYNCED_MONTHS] = current + monthKey
        }
    }

    suspend fun setHfApiToken(token: String?) {
        context.dataStore.edit { preferences ->
            if (token.isNullOrBlank()) {
                preferences.remove(KEY_HF_API_TOKEN)
            } else {
                preferences[KEY_HF_API_TOKEN] = token.trim()
            }
        }
    }

    suspend fun setCustomModelUrl(url: String?) {
        context.dataStore.edit { preferences ->
            if (url.isNullOrBlank()) {
                preferences.remove(KEY_CUSTOM_MODEL_URL)
            } else {
                preferences[KEY_CUSTOM_MODEL_URL] = url.trim()
            }
        }
    }
}
