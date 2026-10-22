package com.innovatex.expensetracker.domain.usecase

import com.innovatex.expensetracker.data.local.LocalDataExportService
import com.innovatex.expensetracker.domain.model.SourceChannel
import com.innovatex.expensetracker.domain.model.Transaction
import com.innovatex.expensetracker.domain.model.TransactionType
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

class HistoricalArchiveTest {

    private val exportService = LocalDataExportService()

    @Test
    fun `test export transactions to CSV writes valid formatted lines`() {
        val txns = listOf(
            Transaction(
                id = "t1",
                amount = 45.50,
                currency = "USD",
                type = TransactionType.DEBIT,
                timestamp = 1729570000000L,
                monthKey = "2026-10",
                merchantRaw = "Whole Foods Market",
                merchantClean = "Whole Foods Market",
                categoryId = "groceries",
                sources = listOf(SourceChannel.SMS, SourceChannel.EMAIL),
                referenceId = "REF12345",
                isDeduplicated = true
            )
        )

        val output = ByteArrayOutputStream()
        exportService.exportTransactionsToCsv(txns, output)
        val csvResult = output.toString(Charsets.UTF_8.name())

        assertTrue(csvResult.contains("id,timestamp,formatted_date,amount,currency,type,merchant,category_id,sources,reference_id,is_deduplicated"))
        assertTrue(csvResult.contains("t1"))
        assertTrue(csvResult.contains("45.5"))
        assertTrue(csvResult.contains("DEBIT"))
        assertTrue(csvResult.contains("Whole Foods Market"))
        assertTrue(csvResult.contains("SMS;EMAIL"))
        assertTrue(csvResult.contains("true"))
    }
}
