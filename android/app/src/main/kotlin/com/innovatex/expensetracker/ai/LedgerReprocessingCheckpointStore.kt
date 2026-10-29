package com.innovatex.expensetracker.ai

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.innovatex.expensetracker.domain.model.TransactionCursor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.ledgerReprocessingDataStore by preferencesDataStore(name = "ledger_reprocessing_checkpoint")

data class LedgerReprocessingCheckpoint(
    val workId: String,
    val total: Int,
    val processed: Int,
    val updatedCount: Int,
    val lastCursor: TransactionCursor?,
    val throughCursor: TransactionCursor,
    val deduplicating: Boolean
)

@Singleton
class LedgerReprocessingCheckpointStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore get() = context.ledgerReprocessingDataStore

    suspend fun read(workId: String): LedgerReprocessingCheckpoint? {
        val values = dataStore.data.first()
        if (values[KEY_WORK_ID] != workId) return null
        val throughTimestamp = values[KEY_THROUGH_TIMESTAMP] ?: return null
        val throughId = values[KEY_THROUGH_ID] ?: return null
        val lastTimestamp = values[KEY_LAST_TIMESTAMP]
        val lastId = values[KEY_LAST_ID]
        return LedgerReprocessingCheckpoint(
            workId = workId,
            total = values[KEY_TOTAL] ?: 0,
            processed = values[KEY_PROCESSED] ?: 0,
            updatedCount = values[KEY_UPDATED] ?: 0,
            lastCursor = if (lastTimestamp != null && lastId != null) TransactionCursor(lastTimestamp, lastId) else null,
            throughCursor = TransactionCursor(throughTimestamp, throughId),
            deduplicating = values[KEY_DEDUPLICATING] ?: false
        )
    }

    suspend fun save(checkpoint: LedgerReprocessingCheckpoint) {
        dataStore.edit { values ->
            values[KEY_WORK_ID] = checkpoint.workId
            values[KEY_TOTAL] = checkpoint.total
            values[KEY_PROCESSED] = checkpoint.processed
            values[KEY_UPDATED] = checkpoint.updatedCount
            values[KEY_THROUGH_TIMESTAMP] = checkpoint.throughCursor.timestamp
            values[KEY_THROUGH_ID] = checkpoint.throughCursor.id
            checkpoint.lastCursor?.let {
                values[KEY_LAST_TIMESTAMP] = it.timestamp
                values[KEY_LAST_ID] = it.id
            } ?: run {
                values.remove(KEY_LAST_TIMESTAMP)
                values.remove(KEY_LAST_ID)
            }
            values[KEY_DEDUPLICATING] = checkpoint.deduplicating
        }
    }

    suspend fun clear(workId: String) {
        dataStore.edit { values ->
            if (values[KEY_WORK_ID] == workId) {
                values.remove(KEY_WORK_ID)
                values.remove(KEY_TOTAL)
                values.remove(KEY_PROCESSED)
                values.remove(KEY_UPDATED)
                values.remove(KEY_LAST_TIMESTAMP)
                values.remove(KEY_LAST_ID)
                values.remove(KEY_THROUGH_TIMESTAMP)
                values.remove(KEY_THROUGH_ID)
                values.remove(KEY_DEDUPLICATING)
            }
        }
    }

    private companion object {
        val KEY_WORK_ID = stringPreferencesKey("work_id")
        val KEY_TOTAL = intPreferencesKey("total")
        val KEY_PROCESSED = intPreferencesKey("processed")
        val KEY_UPDATED = intPreferencesKey("updated")
        val KEY_LAST_TIMESTAMP = longPreferencesKey("last_timestamp")
        val KEY_LAST_ID = stringPreferencesKey("last_id")
        val KEY_THROUGH_TIMESTAMP = longPreferencesKey("through_timestamp")
        val KEY_THROUGH_ID = stringPreferencesKey("through_id")
        val KEY_DEDUPLICATING = booleanPreferencesKey("deduplicating")
    }
}
