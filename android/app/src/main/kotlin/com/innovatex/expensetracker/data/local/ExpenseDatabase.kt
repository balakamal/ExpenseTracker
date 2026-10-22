package com.innovatex.expensetracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.innovatex.expensetracker.data.local.dao.CategoryDao
import com.innovatex.expensetracker.data.local.dao.MerchantRuleDao
import com.innovatex.expensetracker.data.local.dao.TransactionDao
import com.innovatex.expensetracker.data.local.entity.CategoryEntity
import com.innovatex.expensetracker.data.local.entity.Converters
import com.innovatex.expensetracker.data.local.entity.MerchantRuleEntity
import com.innovatex.expensetracker.data.local.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        MerchantRuleEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun merchantRuleDao(): MerchantRuleDao

    companion object {
        const val DATABASE_NAME = "expense_tracker.db"

        val PREPOPULATED_CATEGORIES = listOf(
            CategoryEntity("food_dining", "Food & Dining", "utensils", "#F59E0B", isSystemDefault = true, sortOrder = 1),
            CategoryEntity("groceries", "Groceries", "shopping_cart", "#10B981", isSystemDefault = true, sortOrder = 2),
            CategoryEntity("shopping", "Shopping", "shopping_bag", "#8B5CF6", isSystemDefault = true, sortOrder = 3),
            CategoryEntity("transport", "Transport", "car", "#3B82F6", isSystemDefault = true, sortOrder = 4),
            CategoryEntity("utilities", "Bills & Utilities", "zap", "#EC4899", isSystemDefault = true, sortOrder = 5),
            CategoryEntity("entertainment", "Entertainment", "film", "#06B6D4", isSystemDefault = true, sortOrder = 6),
            CategoryEntity("health", "Health & Wellness", "heart_pulse", "#14B8A6", isSystemDefault = true, sortOrder = 7),
            CategoryEntity("income", "Income & Salary", "trending_up", "#22C55E", isSystemDefault = true, sortOrder = 8),
            CategoryEntity("general", "General", "more_horizontal", "#64748B", isSystemDefault = true, sortOrder = 9)
        )

        fun createCallback(): Callback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Seed initial categories
                PREPOPULATED_CATEGORIES.forEach { category ->
                    db.execSQL(
                        "INSERT OR IGNORE INTO categories (id, name, icon_name, color_hex, is_system_default, sort_order) " +
                            "VALUES ('${category.id}', '${category.name}', '${category.iconName}', '${category.colorHex}', 1, ${category.sortOrder})"
                    )
                }
            }
        }
    }
}
