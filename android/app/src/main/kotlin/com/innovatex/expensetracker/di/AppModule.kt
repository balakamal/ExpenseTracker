package com.innovatex.expensetracker.di

import android.content.Context
import androidx.room.Room
import com.innovatex.expensetracker.data.local.ExpenseDatabase
import com.innovatex.expensetracker.data.local.dao.CategoryDao
import com.innovatex.expensetracker.data.local.dao.MerchantRuleDao
import com.innovatex.expensetracker.data.local.dao.TransactionDao
import com.innovatex.expensetracker.data.remote.GithubReleaseChecker
import com.innovatex.expensetracker.data.repository.CategoryRepositoryImpl
import com.innovatex.expensetracker.data.repository.TransactionRepositoryImpl
import com.innovatex.expensetracker.domain.repository.AppUpdateRepository
import com.innovatex.expensetracker.domain.repository.CategoryRepository
import com.innovatex.expensetracker.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideExpenseDatabase(
        @ApplicationContext context: Context
    ): ExpenseDatabase = Room.databaseBuilder(
        context,
        ExpenseDatabase::class.java,
        ExpenseDatabase.DATABASE_NAME
    )
        .addMigrations(ExpenseDatabase.MIGRATION_1_2)
        .addCallback(ExpenseDatabase.createCallback())
        .build()

    @Provides
    fun provideTransactionDao(database: ExpenseDatabase): TransactionDao = database.transactionDao()

    @Provides
    fun provideCategoryDao(database: ExpenseDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideMerchantRuleDao(database: ExpenseDatabase): MerchantRuleDao = database.merchantRuleDao()

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        impl: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        impl: CategoryRepositoryImpl
    ): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindAppUpdateRepository(
        impl: GithubReleaseChecker
    ): AppUpdateRepository
}
