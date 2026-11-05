package com.innovatex.spendwell.integrations.importer

import com.innovatex.spendwell.common.Money
import com.innovatex.spendwell.domain.model.FinancialNature
import com.innovatex.spendwell.domain.model.Transaction
import com.innovatex.spendwell.domain.model.TransactionStatus
import com.innovatex.spendwell.domain.model.TransactionType
import com.innovatex.spendwell.transactionengine.stages.Stage8MerchantNormalization
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

object StatementImporter {

    private val DATE_FORMATS = listOf(
        SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH),
        SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH),
        SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
    )

    /**
     * Parses standard Indian bank CSV statements (Date, Narration/Description, Chq/Ref, Debit, Credit, Balance).
     */
    fun parseCsv(inputStream: InputStream, accountId: String? = null): List<Transaction> {
        val lines = inputStream.bufferedReader().readLines()
        if (lines.isEmpty()) return emptyList()

        val headerLine = lines.firstOrNull()?.lowercase(Locale.ROOT) ?: return emptyList()
        val headers = headerLine.split(",").map { it.trim().replace("\"", "") }

        val dateIdx = headers.indexOfFirst { it.contains("date") }
        val descIdx = headers.indexOfFirst { it.contains("narration") || it.contains("description") || it.contains("particular") }
        val debitIdx = headers.indexOfFirst { it.contains("debit") || it.contains("withdrawal") }
        val creditIdx = headers.indexOfFirst { it.contains("credit") || it.contains("deposit") }
        val refIdx = headers.indexOfFirst { it.contains("ref") || it.contains("chq") || it.contains("utr") }

        if (dateIdx == -1 || descIdx == -1) return emptyList()

        val parsedTransactions = mutableListOf<Transaction>()

        for (i in 1 until lines.size) {
            val line = lines[i].trim()
            if (line.isBlank()) continue
            val tokens = splitCsvLine(line)
            if (tokens.size <= descIdx || tokens.size <= dateIdx) continue

            val rawDate = tokens[dateIdx]
            val timestamp = parseDate(rawDate) ?: System.currentTimeMillis()
            val description = tokens[descIdx]
            val refNo = if (refIdx != -1 && tokens.size > refIdx) tokens[refIdx].ifBlank { null } else null

            var debitStr = if (debitIdx != -1 && tokens.size > debitIdx) tokens[debitIdx] else ""
            var creditStr = if (creditIdx != -1 && tokens.size > creditIdx) tokens[creditIdx] else ""

            val debitMoney = Money.fromString(debitStr)
            val creditMoney = Money.fromString(creditStr)

            val (amount, type) = when {
                !debitMoney.isZero -> Pair(debitMoney, TransactionType.DEBIT)
                !creditMoney.isZero -> Pair(creditMoney, TransactionType.CREDIT)
                else -> continue
            }

            val cleanMerchant = Stage8MerchantNormalization.cleanMerchantName(description)

            parsedTransactions.add(
                Transaction(
                    id = UUID.randomUUID().toString(),
                    amount = amount,
                    type = type,
                    status = TransactionStatus.SUCCESSFUL,
                    financialNature = if (type == TransactionType.CREDIT) FinancialNature.INCOME else FinancialNature.CONSUMPTION_EXPENSE,
                    timestamp = timestamp,
                    accountId = accountId,
                    merchantId = null,
                    merchantName = cleanMerchant,
                    categoryId = null,
                    categoryName = "Miscellaneous",
                    description = description,
                    referenceId = refNo,
                    utrNumber = null,
                    isReviewed = true,
                    createdAt = System.currentTimeMillis()
                )
            )
        }

        return parsedTransactions
    }

    private fun splitCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var inQuotes = false
        val sb = StringBuilder()

        for (ch in line) {
            when {
                ch == '\"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    result.add(sb.toString().trim())
                    sb.clear()
                }
                else -> sb.append(ch)
            }
        }
        result.add(sb.toString().trim())
        return result
    }

    private fun parseDate(dateStr: String): Long? {
        val clean = dateStr.trim().replace("\"", "")
        for (format in DATE_FORMATS) {
            try {
                return format.parse(clean)?.time
            } catch (e: Exception) {
                // Try next
            }
        }
        return null
    }
}
