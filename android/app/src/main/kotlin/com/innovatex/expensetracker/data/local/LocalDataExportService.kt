package com.innovatex.expensetracker.data.local

import com.innovatex.expensetracker.domain.model.Transaction
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalDataExportService @Inject constructor() {

    fun exportTransactionsToCsv(transactions: List<Transaction>, outputStream: OutputStream) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        outputStream.bufferedWriter().use { writer ->
            // CSV Header
            writer.write("id,timestamp,formatted_date,amount,currency,type,merchant,category_id,sources,reference_id,is_deduplicated\n")

            for (t in transactions) {
                val dateStr = dateFormat.format(Date(t.timestamp))
                val sourcesStr = t.sources.joinToString(";") { it.name }
                val line = buildString {
                    append(t.id).append(",")
                    append(t.timestamp).append(",")
                    append("\"$dateStr\"").append(",")
                    append(t.amount).append(",")
                    append(t.currency).append(",")
                    append(t.type.name).append(",")
                    append("\"${t.merchantClean.replace("\"", "\"\"")}\"").append(",")
                    append(t.categoryId).append(",")
                    append("\"$sourcesStr\"").append(",")
                    append("\"${t.referenceId ?: ""}\"").append(",")
                    append(t.isDeduplicated)
                    append("\n")
                }
                writer.write(line)
            }
        }
    }
}
