package com.innovatex.spendwell.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.innovatex.spendwell.database.converter.Converters
import com.innovatex.spendwell.database.dao.AccountDao
import com.innovatex.spendwell.database.dao.AiInsightDao
import com.innovatex.spendwell.database.dao.BudgetDao
import com.innovatex.spendwell.database.dao.CategoryDao
import com.innovatex.spendwell.database.dao.DuplicateCandidateDao
import com.innovatex.spendwell.database.dao.MerchantDao
import com.innovatex.spendwell.database.dao.ReconciliationDao
import com.innovatex.spendwell.database.dao.RecurringPaymentDao
import com.innovatex.spendwell.database.dao.SavingsGoalDao
import com.innovatex.spendwell.database.dao.TransactionDao
import com.innovatex.spendwell.database.dao.TransactionSourceEventDao
import com.innovatex.spendwell.database.dao.UserProfileDao
import com.innovatex.spendwell.database.entity.AccountEntity
import com.innovatex.spendwell.database.entity.AiInsightEntity
import com.innovatex.spendwell.database.entity.BudgetEntity
import com.innovatex.spendwell.database.entity.CategoryEntity
import com.innovatex.spendwell.database.entity.CreditCardEntity
import com.innovatex.spendwell.database.entity.DuplicateCandidateEntity
import com.innovatex.spendwell.database.entity.FinancialRuleEntity
import com.innovatex.spendwell.database.entity.ImportedStatementEntity
import com.innovatex.spendwell.database.entity.MerchantEntity
import com.innovatex.spendwell.database.entity.ReconciliationSessionEntity
import com.innovatex.spendwell.database.entity.RecurringPaymentEntity
import com.innovatex.spendwell.database.entity.SavingsGoalEntity
import com.innovatex.spendwell.database.entity.TransactionClassificationEntity
import com.innovatex.spendwell.database.entity.TransactionEntity
import com.innovatex.spendwell.database.entity.TransactionSourceEventEntity
import com.innovatex.spendwell.database.entity.UserProfileEntity

@Database(
    entities = [
        UserProfileEntity::class,
        AccountEntity::class,
        CreditCardEntity::class,
        TransactionEntity::class,
        TransactionSourceEventEntity::class,
        MerchantEntity::class,
        CategoryEntity::class,
        TransactionClassificationEntity::class,
        DuplicateCandidateEntity::class,
        RecurringPaymentEntity::class,
        BudgetEntity::class,
        SavingsGoalEntity::class,
        FinancialRuleEntity::class,
        AiInsightEntity::class,
        ReconciliationSessionEntity::class,
        ImportedStatementEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class SpendwellDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun merchantDao(): MerchantDao
    abstract fun recurringPaymentDao(): RecurringPaymentDao
    abstract fun transactionSourceEventDao(): TransactionSourceEventDao
    abstract fun duplicateCandidateDao(): DuplicateCandidateDao
    abstract fun aiInsightDao(): AiInsightDao
    abstract fun reconciliationDao(): ReconciliationDao
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        const val DATABASE_NAME = "spendwell_ledger.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Future migration blueprint
            }
        }
    }
}
