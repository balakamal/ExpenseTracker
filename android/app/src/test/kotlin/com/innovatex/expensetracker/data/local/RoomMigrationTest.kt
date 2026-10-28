package com.innovatex.expensetracker.data.local

import com.innovatex.expensetracker.data.local.entity.Converters
import com.innovatex.expensetracker.domain.model.SourceChannel
import com.innovatex.expensetracker.domain.model.TransactionType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import io.mockk.mockk
import io.mockk.verify

class RoomMigrationTest {

    private val converters = Converters()

    @Test
    fun `test TransactionType converter handles all types`() {
        val debitStr = converters.fromTransactionType(TransactionType.DEBIT)
        val creditStr = converters.fromTransactionType(TransactionType.CREDIT)

        assertEquals("DEBIT", debitStr)
        assertEquals("CREDIT", creditStr)

        assertEquals(TransactionType.DEBIT, converters.toTransactionType(debitStr))
        assertEquals(TransactionType.CREDIT, converters.toTransactionType(creditStr))
        assertEquals(TransactionType.DEBIT, converters.toTransactionType("UNKNOWN_FALLBACK"))
    }

    @Test
    fun `test SourceChannel list serialization preserves multi-channel sources`() {
        val sources = listOf(SourceChannel.SMS, SourceChannel.EMAIL)
        val serialized = converters.fromSourceChannelList(sources)

        assertEquals("SMS,EMAIL", serialized)

        val deserialized = converters.toSourceChannelList(serialized)
        assertEquals(2, deserialized.size)
        assertTrue(deserialized.contains(SourceChannel.SMS))
        assertTrue(deserialized.contains(SourceChannel.EMAIL))
    }

    @Test
    fun `test database default prepopulated categories integrity`() {
        val categories = ExpenseDatabase.PREPOPULATED_CATEGORIES
        assertEquals(9, categories.size)
        assertTrue(categories.any { it.id == "food_dining" })
        assertTrue(categories.any { it.id == "groceries" })
        assertTrue(categories.any { it.id == "income" })
    }

    @Test
    fun `test migration 1 to 2 executes alter table for ai_classification_json`() {
        val mockDb = mockk<androidx.sqlite.db.SupportSQLiteDatabase>(relaxed = true)
        ExpenseDatabase.MIGRATION_1_2.migrate(mockDb)
        verify {
            mockDb.execSQL("ALTER TABLE transactions ADD COLUMN ai_classification_json TEXT DEFAULT NULL")
        }
    }

    @Test
    fun `test migration 2 to 3 executes alter table and creates user_feedback_exemplars`() {
        val mockDb = mockk<androidx.sqlite.db.SupportSQLiteDatabase>(relaxed = true)
        ExpenseDatabase.MIGRATION_2_3.migrate(mockDb)
        verify {
            mockDb.execSQL("ALTER TABLE transactions ADD COLUMN user_override_valid INTEGER DEFAULT NULL")
            mockDb.execSQL(match { it.contains("CREATE TABLE IF NOT EXISTS user_feedback_exemplars") })
        }
    }
}
