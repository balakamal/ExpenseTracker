package com.innovatex.expensetracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.innovatex.expensetracker.domain.model.AiClassificationResult
import com.innovatex.expensetracker.domain.model.AuthenticityLabel
import com.innovatex.expensetracker.domain.model.Category
import com.innovatex.expensetracker.domain.model.MerchantRule
import com.innovatex.expensetracker.domain.model.SourceChannel
import com.innovatex.expensetracker.domain.model.Transaction
import com.innovatex.expensetracker.domain.model.TransactionType

@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["name"], unique = true)
    ]
)
data class CategoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "icon_name")
    val iconName: String,

    @ColumnInfo(name = "color_hex")
    val colorHex: String,

    @ColumnInfo(name = "monthly_budget_limit")
    val monthlyBudgetLimit: Double? = null,

    @ColumnInfo(name = "is_system_default")
    val isSystemDefault: Boolean = false,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0
) {
    fun toDomain() = Category(
        id = id,
        name = name,
        iconName = iconName,
        colorHex = colorHex,
        monthlyBudgetLimit = monthlyBudgetLimit,
        isSystemDefault = isSystemDefault,
        sortOrder = sortOrder
    )

    companion object {
        fun fromDomain(domain: Category) = CategoryEntity(
            id = domain.id,
            name = domain.name,
            iconName = domain.iconName,
            colorHex = domain.colorHex,
            monthlyBudgetLimit = domain.monthlyBudgetLimit,
            isSystemDefault = domain.isSystemDefault,
            sortOrder = domain.sortOrder
        )
    }
}

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["category_id"]),
        Index(value = ["month_key"]),
        Index(value = ["amount", "timestamp"]),
        Index(value = ["reference_id"], unique = false)
    ],
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_DEFAULT
        )
    ]
)
data class TransactionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "currency")
    val currency: String,

    @ColumnInfo(name = "type")
    val type: TransactionType,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "month_key")
    val monthKey: String,

    @ColumnInfo(name = "merchant_raw")
    val merchantRaw: String,

    @ColumnInfo(name = "merchant_clean")
    val merchantClean: String,

    @ColumnInfo(name = "category_id", defaultValue = "general")
    val categoryId: String,

    @ColumnInfo(name = "sources")
    val sources: List<SourceChannel>,

    @ColumnInfo(name = "is_deduplicated")
    val isDeduplicated: Boolean = false,

    @ColumnInfo(name = "reference_id")
    val referenceId: String? = null,

    @ColumnInfo(name = "account_last4")
    val accountLast4: String? = null,

    @ColumnInfo(name = "raw_snippet")
    val rawSnippet: String? = null,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "ai_classification_json")
    val aiClassificationJson: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = Transaction(
        id = id,
        amount = amount,
        currency = currency,
        type = type,
        timestamp = timestamp,
        monthKey = monthKey,
        merchantRaw = merchantRaw,
        merchantClean = merchantClean,
        categoryId = categoryId,
        sources = sources,
        isDeduplicated = isDeduplicated,
        referenceId = referenceId,
        accountLast4 = accountLast4,
        rawSnippet = rawSnippet,
        notes = notes,
        aiClassification = parseAiClassificationJson(aiClassificationJson),
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(domain: Transaction) = TransactionEntity(
            id = domain.id,
            amount = domain.amount,
            currency = domain.currency,
            type = domain.type,
            timestamp = domain.timestamp,
            monthKey = domain.monthKey,
            merchantRaw = domain.merchantRaw,
            merchantClean = domain.merchantClean,
            categoryId = domain.categoryId,
            sources = domain.sources,
            isDeduplicated = domain.isDeduplicated,
            referenceId = domain.referenceId,
            accountLast4 = domain.accountLast4,
            rawSnippet = domain.rawSnippet,
            notes = domain.notes,
            aiClassificationJson = domain.aiClassification?.toJson(),
            createdAt = domain.createdAt
        )

        private fun parseAiClassificationJson(raw: String?): AiClassificationResult? {
            val parts = raw?.split('|') ?: return null
            if (parts.size < 5) return null
            return AiClassificationResult(
                authenticityLabel = runCatching { AuthenticityLabel.valueOf(parts[0]) }.getOrDefault(AuthenticityLabel.REAL_TRANSACTION),
                authenticityConfidence = parts[1].toFloatOrNull() ?: 0f,
                categoryId = parts[2].ifBlank { null },
                categoryConfidence = parts[3].toFloatOrNull(),
                impliedTransactionType = parts[4].ifBlank { null }?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() },
                ranByAi = true
            )
        }

        private fun AiClassificationResult.toJson(): String =
            "${authenticityLabel.name}|$authenticityConfidence|${categoryId ?: ""}|$categoryConfidence|${impliedTransactionType?.name ?: ""}"
    }
}

@Entity(
    tableName = "merchant_rules",
    indices = [
        Index(value = ["pattern"], unique = true),
        Index(value = ["category_id"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MerchantRuleEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "pattern")
    val pattern: String,

    @ColumnInfo(name = "category_id")
    val categoryId: String,

    @ColumnInfo(name = "is_regex")
    val isRegex: Boolean = false,

    @ColumnInfo(name = "priority")
    val priority: Int = 100,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = MerchantRule(
        id = id,
        pattern = pattern,
        categoryId = categoryId,
        isRegex = isRegex,
        priority = priority,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(domain: MerchantRule) = MerchantRuleEntity(
            id = domain.id,
            pattern = domain.pattern,
            categoryId = domain.categoryId,
            isRegex = domain.isRegex,
            priority = domain.priority,
            createdAt = domain.createdAt
        )
    }
}

class Converters {
    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = runCatching {
        TransactionType.valueOf(value)
    }.getOrDefault(TransactionType.DEBIT)

    @TypeConverter
    fun fromSourceChannelList(channels: List<SourceChannel>): String =
        channels.joinToString(",") { it.name }

    @TypeConverter
    fun toSourceChannelList(value: String): List<SourceChannel> =
        if (value.isBlank()) listOf(SourceChannel.SMS)
        else value.split(",").mapNotNull { name ->
            runCatching { SourceChannel.valueOf(name.trim()) }.getOrNull()
        }
}
