package com.innovatex.spendwell.data.seeder

import com.innovatex.spendwell.database.SpendwellDatabase
import com.innovatex.spendwell.database.entity.AccountEntity
import com.innovatex.spendwell.database.entity.AiInsightEntity
import com.innovatex.spendwell.database.entity.CreditCardEntity
import com.innovatex.spendwell.database.entity.MerchantEntity
import com.innovatex.spendwell.database.entity.TransactionEntity
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

object SampleFinancialDataSeeder {

    suspend fun seedIfEmpty(database: SpendwellDatabase) {
        val existingTx = database.transactionDao().observeAllTransactions().firstOrNull()
        if (existingTx.isNullOrEmpty()) {
            forceSeedDemoData(database)
        }
    }

    suspend fun forceSeedDemoData(database: SpendwellDatabase) {
        val now = System.currentTimeMillis()
        val hourMs = 3600_000L
        val dayMs = 86400_000L

        // 1. Seed Accounts matching Stitch
        val hdfcAccount = AccountEntity(
            id = "acc_hdfc",
            accountName = "HDFC Bank Instant Savings",
            accountType = "SAVINGS",
            accountNumberLast4 = "4821",
            bankName = "HDFC Bank",
            currentBalancePaisa = 14285000L, // ₹1,42,850.00
            isPrimary = true,
            isActive = true,
            createdAt = now - 30 * dayMs
        )
        val iciciAccount = AccountEntity(
            id = "acc_icici",
            accountName = "ICICI Bank Privilege",
            accountType = "SAVINGS",
            accountNumberLast4 = "9012",
            bankName = "ICICI Bank",
            currentBalancePaisa = 8500000L, // ₹85,000.00
            isPrimary = false,
            isActive = true,
            createdAt = now - 30 * dayMs
        )
        database.accountDao().insert(hdfcAccount)
        database.accountDao().insert(iciciAccount)

        // 2. Seed Credit Card
        val creditCard = CreditCardEntity(
            id = "cc_hdfc_regalia",
            cardName = "HDFC Regalia Credit Card",
            bankName = "HDFC Bank",
            last4 = "1044",
            creditLimitPaisa = 50000000L, // ₹5,00,000.00
            availableCreditPaisa = 38500000L, // ₹3,85,000.00
            billingCycleDay = 15,
            paymentDueDateDay = 5,
            linkedAccountId = "acc_hdfc",
            createdAt = now - 30 * dayMs
        )
        database.accountDao().insertCreditCard(creditCard)

        // 3. Seed Merchants
        val merchants = listOf(
            MerchantEntity("merch_swiggy", "Swiggy", defaultCategoryId = "cat_food", confidenceScore = 0.99f),
            MerchantEntity("merch_uber", "Uber Premier", defaultCategoryId = "cat_transport", confidenceScore = 0.99f),
            MerchantEntity("merch_chaipoint", "Chai Point", defaultCategoryId = "cat_food", confidenceScore = 0.95f),
            MerchantEntity("merch_razorpay", "Razorpay Software Ltd", defaultCategoryId = "cat_salary", confidenceScore = 0.99f),
            MerchantEntity("merch_blinkit", "Blinkit", defaultCategoryId = "cat_groceries", confidenceScore = 0.98f),
            MerchantEntity("merch_starbucks", "Starbucks Coffee", defaultCategoryId = "cat_food", confidenceScore = 0.98f),
            MerchantEntity("merch_amazon", "Amazon India", defaultCategoryId = "cat_shopping", confidenceScore = 0.99f),
            MerchantEntity("merch_zomato", "Zomato Ltd", defaultCategoryId = "cat_food", confidenceScore = 0.99f),
            MerchantEntity("merch_netflix", "Netflix Entertainment", defaultCategoryId = "cat_entertainment", confidenceScore = 0.99f),
            MerchantEntity("merch_cultfit", "Cult.fit Healthcare", defaultCategoryId = "cat_health", confidenceScore = 0.95f)
        )
        for (m in merchants) {
            database.merchantDao().insert(m)
        }

        // 4. Seed Realistic Transactions from Stitch Screens
        val sampleTransactions = listOf(
            TransactionEntity(
                id = "tx_swiggy_today",
                amountPaisa = 48900L, // ₹489.00
                currency = "INR",
                type = "DEBIT",
                status = "SUCCESSFUL",
                financialNature = "CONSUMPTION_EXPENSE",
                timestamp = now - 2 * hourMs,
                accountId = "acc_hdfc",
                merchantId = "merch_swiggy",
                categoryId = "cat_food",
                description = "Swiggy",
                referenceId = "SWG-882910",
                utrNumber = "426819284019",
                rawMessageSnippet = "Sent Rs.489.00 from HDFC Bank A/C **4821 to SWIGGY UPI ref 426819284019 on 24-09-24. Bal Rs.142850.00",
                isReviewed = true,
                createdAt = now - 2 * hourMs,
                updatedAt = now - 2 * hourMs
            ),
            TransactionEntity(
                id = "tx_uber_today",
                amountPaisa = 34200L, // ₹342.00
                currency = "INR",
                type = "DEBIT",
                status = "SUCCESSFUL",
                financialNature = "CONSUMPTION_EXPENSE",
                timestamp = now - 5 * hourMs,
                accountId = "acc_icici",
                merchantId = "merch_uber",
                categoryId = "cat_transport",
                description = "Uber Premier",
                referenceId = "UBR-902148",
                utrNumber = "426819284020",
                rawMessageSnippet = "Paid Rs.342.00 to Uber India via Google Pay from ICICI A/C **9012",
                isReviewed = true,
                createdAt = now - 5 * hourMs,
                updatedAt = now - 5 * hourMs
            ),
            TransactionEntity(
                id = "tx_chaipoint_today",
                amountPaisa = 6000L, // ₹60.00
                currency = "INR",
                type = "DEBIT",
                status = "SUCCESSFUL",
                financialNature = "CONSUMPTION_EXPENSE",
                timestamp = now - 6 * hourMs,
                accountId = "acc_hdfc",
                merchantId = "merch_chaipoint",
                categoryId = "cat_food",
                description = "Chai Point",
                referenceId = "CHP-11209",
                utrNumber = "426819284033",
                rawMessageSnippet = "Paid Rs.60.00 to Chai Point UPI from HDFC Bank A/C **4821",
                isReviewed = true,
                createdAt = now - 6 * hourMs,
                updatedAt = now - 6 * hourMs
            ),
            TransactionEntity(
                id = "tx_razorpay_salary",
                amountPaisa = 11500000L, // ₹1,15,000.00
                currency = "INR",
                type = "CREDIT",
                status = "SUCCESSFUL",
                financialNature = "INCOME",
                timestamp = now - dayMs,
                accountId = "acc_icici",
                merchantId = "merch_razorpay",
                categoryId = "cat_salary",
                description = "Razorpay Software Ltd",
                referenceId = "RZP-SAL-10294",
                utrNumber = "426819289901",
                rawMessageSnippet = "A/C **9012 credited with Rs.1,15,000.00 on 01-Oct-24 by Razorpay Software Ltd Salary",
                isReviewed = true,
                createdAt = now - dayMs,
                updatedAt = now - dayMs
            ),
            TransactionEntity(
                id = "tx_blinkit_yesterday",
                amountPaisa = 124000L, // ₹1,240.00
                currency = "INR",
                type = "DEBIT",
                status = "SUCCESSFUL",
                financialNature = "CONSUMPTION_EXPENSE",
                timestamp = now - dayMs - 3 * hourMs,
                accountId = "acc_hdfc",
                merchantId = "merch_blinkit",
                categoryId = "cat_groceries",
                description = "Blinkit",
                referenceId = "BLK-338291",
                utrNumber = "426819287711",
                rawMessageSnippet = "Debited Rs.1,240.00 via PhonePe to Blinkit from HDFC Bank A/C **4821",
                isReviewed = true,
                createdAt = now - dayMs - 3 * hourMs,
                updatedAt = now - dayMs - 3 * hourMs
            ),
            TransactionEntity(
                id = "tx_starbucks_pending",
                amountPaisa = 39000L, // ₹390.00
                currency = "INR",
                type = "DEBIT",
                status = "PENDING",
                financialNature = "CONSUMPTION_EXPENSE",
                timestamp = now - 2 * dayMs,
                accountId = "acc_hdfc",
                merchantId = "merch_starbucks",
                categoryId = "cat_food",
                description = "Starbucks Coffee",
                referenceId = "SBUX-44912",
                utrNumber = null,
                rawMessageSnippet = "Card **1044 swiped for Rs.390.00 at Starbucks Coffee. Pending bank settlement.",
                isReviewed = true,
                createdAt = now - 2 * dayMs,
                updatedAt = now - 2 * dayMs
            ),
            TransactionEntity(
                id = "tx_amazon_shopping",
                amountPaisa = 239900L, // ₹2,399.00
                currency = "INR",
                type = "DEBIT",
                status = "SUCCESSFUL",
                financialNature = "CONSUMPTION_EXPENSE",
                timestamp = now - 3 * dayMs,
                accountId = "acc_hdfc",
                merchantId = "merch_amazon",
                categoryId = "cat_shopping",
                description = "Amazon India",
                referenceId = "AMZ-993821",
                utrNumber = "426819285512",
                rawMessageSnippet = "Paid Rs.2,399.00 to Amazon India via Amazon Pay UPI from HDFC Bank A/C **4821",
                isReviewed = true,
                createdAt = now - 3 * dayMs,
                updatedAt = now - 3 * dayMs
            ),
            TransactionEntity(
                id = "tx_zomato_refund",
                amountPaisa = 85000L, // ₹850.00
                currency = "INR",
                type = "CREDIT",
                status = "REFUNDED",
                financialNature = "REFUND",
                timestamp = now - 4 * dayMs,
                accountId = "acc_hdfc",
                merchantId = "merch_zomato",
                categoryId = "cat_food",
                description = "Zomato Ltd",
                referenceId = "ZOM-REF-449",
                utrNumber = "426819281109",
                rawMessageSnippet = "Refund of Rs.850.00 processed for Zomato order #3910. Credited to HDFC Bank A/C **4821",
                isReviewed = true,
                createdAt = now - 4 * dayMs,
                updatedAt = now - 4 * dayMs
            ),
            TransactionEntity(
                id = "tx_netflix_recurring",
                amountPaisa = 64900L, // ₹649.00
                currency = "INR",
                type = "DEBIT",
                status = "SUCCESSFUL",
                financialNature = "CONSUMPTION_EXPENSE",
                timestamp = now - 5 * dayMs,
                accountId = "acc_hdfc",
                merchantId = "merch_netflix",
                categoryId = "cat_entertainment",
                description = "Netflix Entertainment",
                referenceId = "NTFLX-REC-881",
                utrNumber = null,
                rawMessageSnippet = "Recurring charge of Rs.649.00 on HDFC Card **1044 for Netflix Entertainment",
                isReviewed = true,
                createdAt = now - 5 * dayMs,
                updatedAt = now - 5 * dayMs
            ),
            TransactionEntity(
                id = "tx_cultfit_failed",
                amountPaisa = 150000L, // ₹1,500.00
                currency = "INR",
                type = "DEBIT",
                status = "FAILED",
                financialNature = "CONSUMPTION_EXPENSE",
                timestamp = now - 6 * dayMs,
                accountId = "acc_hdfc",
                merchantId = "merch_cultfit",
                categoryId = "cat_health",
                description = "Cult.fit Healthcare",
                referenceId = "CLT-FAIL-991",
                utrNumber = null,
                rawMessageSnippet = "Failed UPI mandate for Rs.1,500.00 at Cult.fit Healthcare",
                isReviewed = true,
                createdAt = now - 6 * dayMs,
                updatedAt = now - 6 * dayMs
            )
        )
        database.transactionDao().insertAll(sampleTransactions)

        // 5. Seed Proactive AI Insight
        val insight = AiInsightEntity(
            id = "insight_food_alert",
            title = "Food delivery is 22% higher",
            message = "You spent ₹6,450 across Swiggy and Zomato in the last 10 days, trending above your 3-month rolling average.",
            type = "SPENDING_ANOMALY",
            actionPayloadJson = "{\"action\": \"open_category\", \"categoryId\": \"cat_food\"}",
            isRead = false,
            createdAt = now - 30 * 60_000L
        )
        database.aiInsightDao().insert(insight)
    }
}
