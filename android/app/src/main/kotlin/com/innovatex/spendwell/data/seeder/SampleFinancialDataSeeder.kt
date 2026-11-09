package com.innovatex.spendwell.data.seeder

import android.content.Context
import com.innovatex.spendwell.database.SpendwellDatabase
import com.innovatex.spendwell.database.entity.AccountEntity
import com.innovatex.spendwell.database.entity.CategoryEntity
import com.innovatex.spendwell.database.entity.TransactionEntity
import com.innovatex.spendwell.domain.model.FinancialNature
import com.innovatex.spendwell.domain.model.TransactionStatus
import com.innovatex.spendwell.domain.model.TransactionType
import com.innovatex.spendwell.integrations.widget.SpendwellWidgetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID

/**
 * Provides an authentic, comprehensive 30-day financial dataset tailored to the Indian ecosystem.
 * Populates realistic bank accounts, default categories, and a timeline of realistic transactions
 * (salary credits, rent, quick-commerce, dining, micro-UPI payments, subscriptions, and investments).
 */
object SampleFinancialDataSeeder {

    suspend fun seedDemoLedger(
        database: SpendwellDatabase,
        context: Context? = null
    ) = withContext(Dispatchers.IO) {
        val txDao = database.transactionDao()
        val accountDao = database.accountDao()
        val catDao = database.categoryDao()

        // 1. Seed Accounts
        val hdfcAcc = AccountEntity(
            id = "acc_hdfc_salary",
            accountName = "HDFC Salary Account",
            accountType = "SAVINGS",
            accountNumberLast4 = "4321",
            bankName = "HDFC",
            currentBalancePaisa = 142850_00L,
            isPrimary = true,
            isActive = true
        )
        val sbiAcc = AccountEntity(
            id = "acc_sbi_savings",
            accountName = "SBI Personal Savings",
            accountType = "SAVINGS",
            accountNumberLast4 = "8899",
            bankName = "SBI",
            currentBalancePaisa = 48500_00L,
            isPrimary = false,
            isActive = true
        )
        val iciciCard = AccountEntity(
            id = "acc_icici_rubyx",
            accountName = "ICICI Rubyx Card",
            accountType = "CREDIT_CARD",
            accountNumberLast4 = "9012",
            bankName = "ICICI",
            currentBalancePaisa = 14200_00L,
            isPrimary = false,
            isActive = true
        )
        accountDao.insert(hdfcAcc)
        accountDao.insert(sbiAcc)
        accountDao.insert(iciciCard)

        // 2. Ensure Default Categories exist
        val defaultCategories = listOf(
            CategoryEntity("cat_food", "Food & Dining", "Restaurant", "#FF7A30"),
            CategoryEntity("cat_groceries", "Groceries & Essentials", "ShoppingCart", "#159B70"),
            CategoryEntity("cat_shopping", "Shopping", "ShoppingBag", "#8B5CF6"),
            CategoryEntity("cat_transport", "Transport & Commute", "DirectionsCar", "#3B82F6"),
            CategoryEntity("cat_utilities", "Bills & Utilities", "Bolt", "#EAB308"),
            CategoryEntity("cat_housing", "Housing & Rent", "Home", "#6366F1"),
            CategoryEntity("cat_entertainment", "Entertainment & OTT", "SmartDisplay", "#EC4899"),
            CategoryEntity("cat_health", "Health & Wellness", "FitnessCenter", "#EF4444"),
            CategoryEntity("cat_salary", "Salary & Income", "Payments", "#10B981"),
            CategoryEntity("cat_investment", "Investments", "TrendingUp", "#06B6D4")
        )
        catDao.insertAll(defaultCategories)

        // 3. Generate 30 Days of Realistic Transactions
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = now }

        val demoTxList = mutableListOf<TransactionEntity>()

        fun addTx(
            daysAgo: Int,
            hourOfDay: Int,
            amountRupees: Double,
            type: TransactionType,
            nature: FinancialNature,
            category: String,
            merchant: String,
            accountId: String = "acc_hdfc_salary",
            accountLast4: String = "4321",
            refId: String? = null
        ) {
            val txCal = (cal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_MONTH, -daysAgo)
                set(Calendar.HOUR_OF_DAY, hourOfDay)
                set(Calendar.MINUTE, (10..55).random())
                set(Calendar.SECOND, 0)
            }
            val paisa = (amountRupees * 100).toLong()

            demoTxList.add(
                TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    amountPaisa = paisa,
                    currency = "INR",
                    type = type.name,
                    status = TransactionStatus.SUCCESSFUL.name,
                    financialNature = nature.name,
                    timestamp = txCal.timeInMillis,
                    accountId = accountId,
                    toAccountId = null,
                    merchantId = null,
                    categoryId = category,
                    description = merchant,
                    referenceId = refId ?: UUID.randomUUID().toString().take(12).uppercase(),
                    utrNumber = (600000000000L + (10000000000L..99999999999L).random()).toString(),
                    rawMessageSnippet = "Transaction of ₹$amountRupees at $merchant",
                    isReviewed = true,
                    originalTransactionId = null,
                    recurringPaymentId = null,
                    createdAt = txCal.timeInMillis,
                    updatedAt = txCal.timeInMillis
                )
            )
        }

        // --- SALARY INFLOW (Month Start & Mid) ---
        addTx(daysAgo = 28, hourOfDay = 9, amountRupees = 95000.0, type = TransactionType.CREDIT, nature = FinancialNature.INCOME, category = "cat_salary", merchant = "Company Payroll Pvt Ltd")

        // --- HOUSING & RENT ---
        addTx(daysAgo = 27, hourOfDay = 11, amountRupees = 24000.0, type = TransactionType.DEBIT, nature = FinancialNature.CONSUMPTION_EXPENSE, category = "cat_housing", merchant = "NoBroker Rent Pay")

        // --- INVESTMENTS (Excluded from consumption expenses) ---
        addTx(daysAgo = 25, hourOfDay = 10, amountRupees = 10000.0, type = TransactionType.DEBIT, nature = FinancialNature.INVESTMENT_TRANSFER, category = "cat_investment", merchant = "Zerodha Broking SIP")
        addTx(daysAgo = 15, hourOfDay = 14, amountRupees = 5000.0, type = TransactionType.DEBIT, nature = FinancialNature.INVESTMENT_TRANSFER, category = "cat_investment", merchant = "Groww Mutual Fund")

        // --- UTILITIES & BILLS ---
        addTx(daysAgo = 24, hourOfDay = 16, amountRupees = 1850.0, type = TransactionType.DEBIT, nature = FinancialNature.CONSUMPTION_EXPENSE, category = "cat_utilities", merchant = "BESCOM Electricity")
        addTx(daysAgo = 22, hourOfDay = 12, amountRupees = 1179.0, type = TransactionType.DEBIT, nature = FinancialNature.CONSUMPTION_EXPENSE, category = "cat_utilities", merchant = "Airtel Broadband Fiber")
        addTx(daysAgo = 18, hourOfDay = 21, amountRupees = 649.0, type = TransactionType.DEBIT, nature = FinancialNature.CONSUMPTION_EXPENSE, category = "cat_entertainment", merchant = "Netflix Entertainment")
        addTx(daysAgo = 12, hourOfDay = 20, amountRupees = 119.0, type = TransactionType.DEBIT, nature = FinancialNature.CONSUMPTION_EXPENSE, category = "cat_entertainment", merchant = "Spotify India")

        // --- GROCERIES & QUICK COMMERCE ---
        addTx(daysAgo = 26, hourOfDay = 8, amountRupees = 680.0, type = TransactionType.DEBIT, nature = FinancialNature.CONSUMPTION_EXPENSE, category = "cat_groceries", merchant = "Blinkit Commerce")
        addTx(daysAgo = 20, hourOfDay = 19, amountRupees = 540.0, type = TransactionType.DEBIT, nature = FinancialNature.CONSUMPTION_EXPENSE, category = "cat_groceries", merchant = "Zepto Delivery")
        addTx(daysAgo = 14, hourOfDay = 9, amountRupees = 890.0, type = TransactionType.DEBIT, nature = FinancialNature.CONSUMPTION_EXPENSE, category = "cat_groceries", merchant = "Instamart Groceries")
        addTx(daysAgo = 8, hourOfDay = 18, amountRupees = 1150.0, type = TransactionType.DEBIT, nature = FinancialNature.CONSUMPTION_EXPENSE, category = "cat_groceries", merchant = "BigBasket Supermarket")
        addTx(daysAgo = 3, hourOfDay = 10, amountRupees = 480.0, type = TransactionType.DEBIT, nature = FinancialNature.CONSUMPTION_EXPENSE, category = "cat_groceries", merchant = "Zepto Quick")

        // --- FOOD & DINING ---
        addTx(daysAgo = 25, hourOfDay = 13, amountRupees = 480.0, type = TransactionType.DEBIT, nature = FinancialNature.CONSUMPTION_EXPENSE, category = "cat_food", merchant = "Swiggy Restaurant")
        addTx(daysAgo = 23, hourOfDay = 20, amountRupees = 720.0, type = TransactionType.DEBIT, nature = FinancialNature.CONSUMPTION_EXPENSE, category = "cat_food", merchant = "Zomato Orders")
        addTx(daysAgo = 19, hourOfDay = 17, amountRupees = 390.0, type = TransactionType.DEBIT, nature = FinancialNature.CONSUMPTION_EXPENSE, category = "cat_food", merchant = "Starbucks Coffee")
        addTx(daysAgo = 16, hourOfDay = 13, amountRupees = 310.0, type = TransactionType.DEBIT, nature = FinancialNature.CONSUMPTION_EXPENSE, category = "cat_food", merchant = "Subway Sandwich")
        addTx(daysAgo = 11, hourOfDay = 21, amountRupees = 1420.0, type = TransactionType.DEBIT, nature = FinancialNature.CONSUMPTION_EXPENSE, category = "cat_food", merchant = "Toit Brewpub")
        addTx(daysAgo = 7, hourOfDay = 20, amountRupees = 650.0, type = TransactionType.DEBIT, nature = FinancialNature.CONSUMPTION_EXPENSE, category = "cat_food", merchant = "Dominos Pizza")
        addTx(daysAgo = 2, hourOfDay = 13, amountRupees = 340.0, type = TransactionType.DEBIT, nature = FinancialNature.CONSUMPTION_EXPENSE, category = "cat_food", merchant = "Swiggy Daily")

        // --- SHOPPING & COMMERCE ---
        addTx(daysAgo = 21, hourOfDay = 15, amountRupees = 2499.0, type = TransactionType.DEBIT, nature = FinancialNature.CONSUMPTION_EXPENSE, category = "cat_shopping", merchant = "Amazon India", accountId = "acc_icici_rubyx")
        addTx(daysAgo = 13, hourOfDay = 18, amountRupees = 1890.0, type = TransactionType.DEBIT, nature = FinancialNature.CONSUMPTION_EXPENSE, category = "cat_shopping", merchant = "Myntra Fashion", accountId = "acc_icici_rubyx")
        addTx(daysAgo = 5, hourOfDay = 16, amountRupees = 999.0, type = TransactionType.DEBIT, nature = FinancialNature.CONSUMPTION_EXPENSE, category = "cat_shopping", merchant = "Nykaa Retail")

        // --- TRANSPORT & COMMUTE ---
        addTx(daysAgo = 26, hourOfDay = 9, amountRupees = 240.0, type = TransactionType.DEBIT, nature = FinancialNature.CONSUMPTION_EXPENSE, category = "cat_transport", merchant = "Uber India")
        addTx(daysAgo = 22, hourOfDay = 18, amountRupees = 190.0, type = TransactionType.DEBIT, nature = FinancialNature.CONSUMPTION_EXPENSE, category = "cat_transport", merchant = "Ola Cabs")
        addTx(daysAgo = 17, hourOfDay = 9, amountRupees = 220.0, type = TransactionType.DEBIT, nature = FinancialNature.CONSUMPTION_EXPENSE, category = "cat_transport", merchant = "Uber Ride")
        addTx(daysAgo = 10, hourOfDay = 19, amountRupees = 180.0, type = TransactionType.DEBIT, nature = FinancialNature.CONSUMPTION_EXPENSE, category = "cat_transport", merchant = "Rapido Bike")
        addTx(daysAgo = 4, hourOfDay = 8, amountRupees = 260.0, type = TransactionType.DEBIT, nature = FinancialNature.CONSUMPTION_EXPENSE, category = "cat_transport", merchant = "Uber India")

        // --- MICRO-TRANSACTIONS (≤ ₹100, Frequent UPI) ---
        addTx(daysAgo = 24, hourOfDay = 11, amountRupees = 30.0, type = TransactionType.DEBIT, nature = FinancialNature.CONSUMPTION_EXPENSE, category = "cat_food", merchant = "Chai Point Quick")
        addTx(daysAgo = 21, hourOfDay = 17, amountRupees = 40.0, type = TransactionType.DEBIT, nature = FinancialNature.CONSUMPTION_EXPENSE, category = "cat_food", merchant = "Tender Coconut")
        addTx(daysAgo = 18, hourOfDay = 11, amountRupees = 50.0, type = TransactionType.DEBIT, nature = FinancialNature.CONSUMPTION_EXPENSE, category = "cat_food", merchant = "Corner Bakery")
        addTx(daysAgo = 15, hourOfDay = 16, amountRupees = 30.0, type = TransactionType.DEBIT, nature = FinancialNature.CONSUMPTION_EXPENSE, category = "cat_food", merchant = "Tea Post")
        addTx(daysAgo = 9, hourOfDay = 11, amountRupees = 20.0, type = TransactionType.DEBIT, nature = FinancialNature.CONSUMPTION_EXPENSE, category = "cat_food", merchant = "Chai Vendor")
        addTx(daysAgo = 6, hourOfDay = 17, amountRupees = 60.0, type = TransactionType.DEBIT, nature = FinancialNature.CONSUMPTION_EXPENSE, category = "cat_food", merchant = "Fresh Juice Stall")

        // --- TODAY'S TRANSACTIONS ---
        addTx(daysAgo = 0, hourOfDay = 8, amountRupees = 30.0, type = TransactionType.DEBIT, nature = FinancialNature.CONSUMPTION_EXPENSE, category = "cat_food", merchant = "Morning Chai")
        addTx(daysAgo = 0, hourOfDay = 10, amountRupees = 420.0, type = TransactionType.DEBIT, nature = FinancialNature.CONSUMPTION_EXPENSE, category = "cat_groceries", merchant = "Blinkit Quick Delivery")
        addTx(daysAgo = 0, hourOfDay = 13, amountRupees = 360.0, type = TransactionType.DEBIT, nature = FinancialNature.CONSUMPTION_EXPENSE, category = "cat_food", merchant = "Swiggy Lunch Box")
        addTx(daysAgo = 0, hourOfDay = 18, amountRupees = 190.0, type = TransactionType.DEBIT, nature = FinancialNature.CONSUMPTION_EXPENSE, category = "cat_transport", merchant = "Uber Evening Ride")

        // Insert all into Room
        txDao.insertAll(demoTxList)

        // Trigger live home screen widgets refresh
        if (context != null) {
            SpendwellWidgetManager.updateAllWidgets(context)
        }
    }

    suspend fun clearAllData(
        database: SpendwellDatabase,
        context: Context? = null
    ) = withContext(Dispatchers.IO) {
        database.transactionDao().clearAll()
        if (context != null) {
            SpendwellWidgetManager.updateAllWidgets(context)
        }
    }
}
