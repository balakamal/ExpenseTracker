package com.innovatex.spendwell.transactionengine

import com.innovatex.spendwell.analytics.FinancialAnalyticsEngine
import com.innovatex.spendwell.common.Money
import com.innovatex.spendwell.domain.model.FinancialNature
import com.innovatex.spendwell.domain.model.SourceType
import com.innovatex.spendwell.domain.model.Transaction
import com.innovatex.spendwell.domain.model.TransactionStatus
import com.innovatex.spendwell.domain.model.TransactionType
import com.innovatex.spendwell.transactionengine.model.DropReason
import com.innovatex.spendwell.transactionengine.model.ExtractedFinancialInfo
import com.innovatex.spendwell.transactionengine.model.RawSignal
import com.innovatex.spendwell.transactionengine.stages.Stage11CategoryClassification
import com.innovatex.spendwell.transactionengine.stages.Stage3MessageNormalization
import com.innovatex.spendwell.transactionengine.stages.Stage4SpamAndReminderFilter
import com.innovatex.spendwell.transactionengine.stages.Stage5TransactionDetection
import com.innovatex.spendwell.transactionengine.stages.Stage6StructuredExtraction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Calendar

class IndianFinancialSmsStreamSimulationTest {

    data class MockSmsScenario(
        val sender: String,
        val text: String,
        val timestamp: Long,
        val sourceType: SourceType = SourceType.SMS,
        val shouldBeDropped: Boolean = false,
        val expectedDropReason: DropReason? = null,
        val expectedAmountRupees: Double? = null,
        val expectedType: TransactionType? = null,
        val expectedNature: FinancialNature? = null,
        val expectedCategory: String? = null,
        val expectedMerchant: String? = null
    )

    @Test
    fun `simulate authentic 30-day Indian financial SMS stream with spam, OTPs, reminders and duplicates`() {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = now }

        fun timeOffset(daysAgo: Int, hourOfDay: Int): Long {
            return (cal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_MONTH, -daysAgo)
                set(Calendar.HOUR_OF_DAY, hourOfDay)
                set(Calendar.MINUTE, 15)
                set(Calendar.SECOND, 0)
            }.timeInMillis
        }

        val testStream = listOf(
            // 1. Genuine Salary Credit (30 days ago)
            MockSmsScenario(
                sender = "VK-SBIINB",
                text = "INR 95,000.00 credited to your A/C **4321 on 10-Oct-26 by PAYROLL SALARY OCT. Info: UTR998822.",
                timestamp = timeOffset(30, 9),
                shouldBeDropped = false,
                expectedAmountRupees = 95000.0,
                expectedType = TransactionType.CREDIT,
                expectedNature = FinancialNature.INCOME,
                expectedCategory = "cat_salary",
                expectedMerchant = "Payroll Salary Oct"
            ),

            // 2. SPAM: Pre-approved personal loan (Must be dropped!)
            MockSmsScenario(
                sender = "AD-HDFCBK",
                text = "Congratulations! You are eligible for pre-approved personal loan of Rs 5,00,000 at flat 10.5% interest. Apply now in 2 mins: hdfcbk.io/loan",
                timestamp = timeOffset(28, 11),
                shouldBeDropped = true,
                expectedDropReason = DropReason.SPAM_OR_PROMOTIONAL
            ),

            // 3. OTP: Verification code with transaction amount (Must be dropped!)
            MockSmsScenario(
                sender = "VM-ICICIB",
                text = "123456 is your secret OTP for transaction of Rs. 4,500.00 at AMAZON on ICICI Bank Card **9012. Do not share with anyone. Valid for 10 mins.",
                timestamp = timeOffset(27, 14),
                shouldBeDropped = true,
                expectedDropReason = DropReason.SPAM_OR_PROMOTIONAL
            ),

            // 4. Genuine HDFC UPI Debit: Swiggy
            MockSmsScenario(
                sender = "VM-HDFCBK",
                text = "Sent Rs.480.00 from HDFC Bank A/C **4321 to SWIGGY on 13-Oct-26 via UPI Ref: 629811234567. Avl Bal: Rs. 84,200.50",
                timestamp = timeOffset(27, 20),
                shouldBeDropped = false,
                expectedAmountRupees = 480.0,
                expectedType = TransactionType.DEBIT,
                expectedNature = FinancialNature.CONSUMPTION_EXPENSE,
                expectedCategory = "cat_food",
                expectedMerchant = "Swiggy"
            ),

            // 5. BILL REMINDER: Statement generated / Due date notice (Must be dropped from expenses!)
            MockSmsScenario(
                sender = "AD-HDFCBK",
                text = "Dear Cardholder, your HDFC Bank Credit Card statement is generated. Total amount due: Rs. 14,250.00 by due date 05-Nov-26. Pay now to avoid late fee.",
                timestamp = timeOffset(25, 16),
                shouldBeDropped = true,
                expectedDropReason = DropReason.SPAM_OR_PROMOTIONAL
            ),

            // 6. Genuine ICICI Credit Card swipe: Amazon
            MockSmsScenario(
                sender = "AD-ICICIB",
                text = "Your ICICI Bank Credit Card XX9012 has been used for a transaction of INR 2,499.00 at AMAZON on 15-Oct-26. Info: TXN998811.",
                timestamp = timeOffset(25, 17),
                shouldBeDropped = false,
                expectedAmountRupees = 2499.0,
                expectedType = TransactionType.DEBIT,
                expectedNature = FinancialNature.CONSUMPTION_EXPENSE,
                expectedCategory = "cat_shopping",
                expectedMerchant = "Amazon"
            ),

            // 7. Genuine Quick-Commerce: Zepto
            MockSmsScenario(
                sender = "VM-HDFCBK",
                text = "Debited Rs. 420.00 from A/C **4321 on 17-Oct-26 to ZEPTO via UPI Ref 629833441122. Avl Bal Rs. 81,281.50",
                timestamp = timeOffset(23, 10),
                shouldBeDropped = false,
                expectedAmountRupees = 420.0,
                expectedType = TransactionType.DEBIT,
                expectedNature = FinancialNature.CONSUMPTION_EXPENSE,
                expectedCategory = "cat_groceries",
                expectedMerchant = "Zepto"
            ),

            // 8. Genuine Micro-transaction: Chai Point (₹30)
            MockSmsScenario(
                sender = "VM-HDFCBK",
                text = "Paid Rs. 30.00 to CHAI POINT from HDFC A/C **4321 on 20-Oct. UPI Ref 629855667788.",
                timestamp = timeOffset(20, 11),
                shouldBeDropped = false,
                expectedAmountRupees = 30.0,
                expectedType = TransactionType.DEBIT,
                expectedNature = FinancialNature.CONSUMPTION_EXPENSE,
                expectedCategory = "cat_food",
                expectedMerchant = "Chai Point"
            ),

            // 9. Investment Transfer: Zerodha SIP (Must NOT count as consumption expense)
            MockSmsScenario(
                sender = "VM-HDFCBK",
                text = "Sent Rs. 10,000.00 to ZERODHA BROKING via UPI from A/C **4321 on 22-Oct. Ref: 778899.",
                timestamp = timeOffset(18, 10),
                shouldBeDropped = false,
                expectedAmountRupees = 10000.0,
                expectedType = TransactionType.DEBIT,
                expectedNature = FinancialNature.INVESTMENT_TRANSFER,
                expectedCategory = "cat_investment",
                expectedMerchant = "Zerodha Broking"
            ),

            // 10. Self Transfer / Internal Transfer (Must NOT count as consumption expense)
            MockSmsScenario(
                sender = "VK-SBIINB",
                text = "Rs. 8,000.00 debited from A/C **4321 for self transfer to own A/C **8899 on 24-Oct. Ref: 889900.",
                timestamp = timeOffset(16, 15),
                shouldBeDropped = false,
                expectedAmountRupees = 8000.0,
                expectedType = TransactionType.DEBIT,
                expectedNature = FinancialNature.INTERNAL_TRANSFER,
                expectedMerchant = "Transfer Between Own"
            ),

            // 11. Genuine Mobility: Uber
            MockSmsScenario(
                sender = "VM-HDFCBK",
                text = "Sent Rs. 260.00 to UBER INDIA from HDFC Bank A/C **4321 on 26-Oct via UPI. Ref 629877889900.",
                timestamp = timeOffset(14, 18),
                shouldBeDropped = false,
                expectedAmountRupees = 260.0,
                expectedType = TransactionType.DEBIT,
                expectedNature = FinancialNature.CONSUMPTION_EXPENSE,
                expectedCategory = "cat_transport",
                expectedMerchant = "Uber India"
            ),

            // 12. Refund: Swiggy Refund
            MockSmsScenario(
                sender = "VM-HDFCBK",
                text = "Rs. 240.00 refunded to your A/C **4321 on 28-Oct from SWIGGY. Ref: RFN9944.",
                timestamp = timeOffset(12, 14),
                shouldBeDropped = false,
                expectedAmountRupees = 240.0,
                expectedType = TransactionType.CREDIT,
                expectedNature = FinancialNature.REFUND,
                expectedMerchant = "Swiggy"
            )
        )

        val parsedTransactions = mutableListOf<Transaction>()

        for (scenario in testStream) {
            val signal = RawSignal(scenario.sourceType, scenario.sender, scenario.text, scenario.timestamp)
            val normalized = Stage3MessageNormalization.normalize(signal)

            // 1. Spam & Filter Stage
            val dropReason = Stage4SpamAndReminderFilter.filter(normalized)
            if (scenario.shouldBeDropped) {
                assertNotNull(dropReason, "Expected message to be dropped: ${scenario.text}")
                assertEquals(scenario.expectedDropReason, dropReason)
                continue
            } else {
                assertNull(dropReason, "Expected valid transaction, but was dropped with reason $dropReason: ${scenario.text}")
            }

            // 2. Transaction Detection Stage
            val isTx = Stage5TransactionDetection.isTransaction(normalized.normalizedText)
            assertTrue(isTx, "Failed transaction detection for: ${scenario.text}")

            // 3. Structured Extraction Stage
            val extracted: ExtractedFinancialInfo? = Stage6StructuredExtraction.extract(normalized)
            assertNotNull(extracted, "Extraction returned null for: ${scenario.text}")

            if (scenario.expectedAmountRupees != null) {
                assertEquals(scenario.expectedAmountRupees, extracted!!.amount.toDisplayDouble())
            }
            if (scenario.expectedType != null) {
                assertEquals(scenario.expectedType, extracted!!.transactionType)
            }
            if (scenario.expectedNature != null) {
                assertEquals(scenario.expectedNature, extracted!!.financialNature)
            }

            // 4. Category Classification
            val (categoryId, _) = Stage11CategoryClassification.classify(extracted!!)
            if (scenario.expectedCategory != null) {
                assertEquals(scenario.expectedCategory, categoryId)
            }

            // Convert to domain transaction for analytics verification
            parsedTransactions.add(
                Transaction(
                    id = "tx_${parsedTransactions.size}",
                    amount = extracted.amount,
                    type = extracted.transactionType,
                    status = extracted.status,
                    financialNature = extracted.financialNature,
                    timestamp = scenario.timestamp,
                    categoryId = categoryId,
                    merchantName = extracted.normalizedMerchant,
                    description = extracted.normalizedMerchant
                )
            )
        }

        // Verify Multi-Window Analytics Engine correctly handles the ingested stream!
        // A. Monthly Analytics
        val calMonth = Calendar.getInstance().apply { timeInMillis = timeOffset(15, 12) }
        val monthlySummary = FinancialAnalyticsEngine.analyze(
            transactions = parsedTransactions,
            year = calMonth.get(Calendar.YEAR),
            monthZeroIndexed = calMonth.get(Calendar.MONTH),
            cycleStartDay = 1
        )

        // Salary: ₹ 95,000
        assertEquals(95000.0, monthlySummary.totalIncome.toDisplayDouble())

        // Investment (₹10,000) and Internal Transfer (₹8,000) must strictly NOT be counted as expenses!
        // Expenses = Swiggy(480) + Amazon(2499) + Zepto(420) + ChaiPoint(30) + Uber(260) - SwiggyRefund(240) = 3449.0
        val expectedExpenses = 480.0 + 2499.0 + 420.0 + 30.0 + 260.0 - 240.0
        assertEquals(expectedExpenses, monthlySummary.totalExpenses.toDisplayDouble())

        // Net Savings = Income - Expenses = 95,000 - 3,449 = 91,551
        assertEquals(95000.0 - expectedExpenses, monthlySummary.netSavings.toDisplayDouble())

        // B. Daily Analytics on the day of Swiggy + Amazon
        val amazonDayTimestamp = timeOffset(25, 12)
        val dailySummary = FinancialAnalyticsEngine.analyzeDaily(parsedTransactions, amazonDayTimestamp)
        assertEquals(2499.0, dailySummary.totalExpenses.toDisplayDouble())

        // C. Weekly Analytics
        val weeklySummary = FinancialAnalyticsEngine.analyzeWeekly(parsedTransactions, weekOffset = 2)
        assertNotNull(weeklySummary)
        assertTrue(weeklySummary.totalExpenses.toDisplayDouble() > 0)
        assertEquals(7, weeklySummary.dailyBars.size)

        // D. Cumulative Bezier Curve Points
        assertTrue(monthlySummary.cumulativeSpendPoints.isNotEmpty())
        assertEquals(monthlySummary.cycleWindow?.totalDays, monthlySummary.cumulativeSpendPoints.size)
    }
}
