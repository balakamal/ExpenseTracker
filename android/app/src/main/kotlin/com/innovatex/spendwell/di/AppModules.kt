package com.innovatex.spendwell.di

import android.content.Context
import androidx.room.Room
import com.innovatex.spendwell.core.DefaultDispatcherProvider
import com.innovatex.spendwell.core.DispatcherProvider
import com.innovatex.spendwell.data.repository.AccountRepositoryImpl
import com.innovatex.spendwell.data.repository.BudgetRepositoryImpl
import com.innovatex.spendwell.data.repository.CategoryRepositoryImpl
import com.innovatex.spendwell.data.repository.DuplicateRepositoryImpl
import com.innovatex.spendwell.data.repository.InsightRepositoryImpl
import com.innovatex.spendwell.data.repository.RecurringPaymentRepositoryImpl
import com.innovatex.spendwell.data.repository.SavingsGoalRepositoryImpl
import com.innovatex.spendwell.data.repository.TransactionRepositoryImpl
import com.innovatex.spendwell.database.SpendwellDatabase
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
import com.innovatex.spendwell.domain.repository.AccountRepository
import com.innovatex.spendwell.domain.repository.BudgetRepository
import com.innovatex.spendwell.domain.repository.CategoryRepository
import com.innovatex.spendwell.domain.repository.DuplicateRepository
import com.innovatex.spendwell.domain.repository.InsightRepository
import com.innovatex.spendwell.domain.repository.RecurringPaymentRepository
import com.innovatex.spendwell.domain.repository.SavingsGoalRepository
import com.innovatex.spendwell.domain.repository.TransactionRepository
import com.innovatex.spendwell.security.KeyStoreManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingModule {

    @Binds
    @Singleton
    abstract fun bindDispatcherProvider(impl: DefaultDispatcherProvider): DispatcherProvider

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(impl: BudgetRepositoryImpl): BudgetRepository

    @Binds
    @Singleton
    abstract fun bindSavingsGoalRepository(impl: SavingsGoalRepositoryImpl): SavingsGoalRepository

    @Binds
    @Singleton
    abstract fun bindRecurringPaymentRepository(impl: RecurringPaymentRepositoryImpl): RecurringPaymentRepository

    @Binds
    @Singleton
    abstract fun bindInsightRepository(impl: InsightRepositoryImpl): InsightRepository

    @Binds
    @Singleton
    abstract fun bindDuplicateRepository(impl: DuplicateRepositoryImpl): DuplicateRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideSpendwellDatabase(
        @ApplicationContext context: Context,
        keyStoreManager: KeyStoreManager
    ): SpendwellDatabase {
        val passphrase = keyStoreManager.getOrCreateDatabasePassphrase()
        val factory = SupportFactory(passphrase)

        return Room.databaseBuilder(
            context,
            SpendwellDatabase::class.java,
            SpendwellDatabase.DATABASE_NAME
        )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideTransactionDao(db: SpendwellDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideAccountDao(db: SpendwellDatabase): AccountDao = db.accountDao()

    @Provides
    fun provideCategoryDao(db: SpendwellDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideBudgetDao(db: SpendwellDatabase): BudgetDao = db.budgetDao()

    @Provides
    fun provideSavingsGoalDao(db: SpendwellDatabase): SavingsGoalDao = db.savingsGoalDao()

    @Provides
    fun provideMerchantDao(db: SpendwellDatabase): MerchantDao = db.merchantDao()

    @Provides
    fun provideRecurringPaymentDao(db: SpendwellDatabase): RecurringPaymentDao = db.recurringPaymentDao()

    @Provides
    fun provideTransactionSourceEventDao(db: SpendwellDatabase): TransactionSourceEventDao = db.transactionSourceEventDao()

    @Provides
    fun provideDuplicateCandidateDao(db: SpendwellDatabase): DuplicateCandidateDao = db.duplicateCandidateDao()

    @Provides
    fun provideAiInsightDao(db: SpendwellDatabase): AiInsightDao = db.aiInsightDao()

    @Provides
    fun provideReconciliationDao(db: SpendwellDatabase): ReconciliationDao = db.reconciliationDao()

    @Provides
    fun provideUserProfileDao(db: SpendwellDatabase): UserProfileDao = db.userProfileDao()
}
