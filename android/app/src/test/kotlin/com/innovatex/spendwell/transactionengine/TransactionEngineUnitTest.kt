package com.innovatex.spendwell.transactionengine

import com.innovatex.spendwell.domain.model.FinancialNature
import com.innovatex.spendwell.domain.model.SourceType
import com.innovatex.spendwell.domain.model.TransactionStatus
import com.innovatex.spendwell.domain.model.TransactionType
import com.innovatex.spendwell.transactionengine.model.DropReason
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

class TransactionEngineUnitTest {

    @Test
    fun `test HDFC UPI debit SMS extraction`() {
        val raw = "Sent Rs.450.00 from HDFC Bank A/C **1234 to SWIGGY on 24-OCT-26 via UPI Ref: 629811234567. Avl Bal: Rs. 14,200.50"
        val signal = RawSignal(SourceType.SMS, "VM-HDFCBK", raw)
        val normalized = Stage3MessageNormalization.normalize(signal)

        assertNull(Stage4SpamAndReminderFilter.filter(normalized))
        assertTrue(Stage5TransactionDetection.isTransaction(normalized.normalizedText))

        val extracted = Stage6StructuredExtraction.extract(normalized)
        assertNotNull(extracted)
        assertEquals("450.00", extracted!!.amount.formatInr(false))
        assertEquals(TransactionType.DEBIT, extracted.transactionType)
        assertEquals(TransactionStatus.SUCCESSFUL, extracted.status)
        assertEquals("Swiggy", extracted.normalizedMerchant)
        assertEquals("1234", extracted.accountLast4)
        assertEquals("629811234567", extracted.referenceId)
        assertEquals(FinancialNature.CONSUMPTION_EXPENSE, extracted.financialNature)

        val (catId, _) = Stage11CategoryClassification.classify(extracted)
        assertEquals("cat_food", catId)
    }

    @Test
    fun `test ICICI credit card transaction extraction`() {
        val raw = "Dear Customer, your ICICI Bank Credit Card XX9012 has been used for a transaction of INR 3,499.00 at AMAZON on 25-Oct-26. Info: TXN998811."
        val signal = RawSignal(SourceType.SMS, "AD-ICICIB", raw)
        val normalized = Stage3MessageNormalization.normalize(signal)

        assertNull(Stage4SpamAndReminderFilter.filter(normalized))
        val extracted = Stage6StructuredExtraction.extract(normalized)
        assertNotNull(extracted)
        assertEquals("3,499.00", extracted!!.amount.formatInr(false))
        assertEquals("Amazon", extracted.normalizedMerchant)
        assertEquals("9012", extracted.accountLast4)
        assertEquals(FinancialNature.CONSUMPTION_EXPENSE, extracted.financialNature)

        val (catId, _) = Stage11CategoryClassification.classify(extracted)
        assertEquals("cat_shopping", catId)
    }

    @Test
    fun `test salary credit transaction extraction`() {
        val raw = "INR 95,000.00 credited to your A/C **4321 on 31-Oct-26 by PAYROLL SALARY OCT. Info: UTR998822."
        val signal = RawSignal(SourceType.SMS, "VK-SBIINB", raw)
        val normalized = Stage3MessageNormalization.normalize(signal)

        assertNull(Stage4SpamAndReminderFilter.filter(normalized))
        val extracted = Stage6StructuredExtraction.extract(normalized)
        assertNotNull(extracted)
        assertEquals("95,000.00", extracted!!.amount.formatInr(false))
        assertEquals(TransactionType.CREDIT, extracted.transactionType)
        assertEquals(FinancialNature.INCOME, extracted.financialNature)

        val (catId, _) = Stage11CategoryClassification.classify(extracted)
        assertEquals("cat_salary", catId)
    }

    @Test
    fun `test refund transaction extraction`() {
        val raw = "Rs 1,200.00 refunded to your A/C **1234 on 22-Oct from Zomato. Ref: RFN8833."
        val signal = RawSignal(SourceType.SMS, "VM-HDFCBK", raw)
        val normalized = Stage3MessageNormalization.normalize(signal)

        val extracted = Stage6StructuredExtraction.extract(normalized)
        assertNotNull(extracted)
        assertEquals(TransactionStatus.REFUNDED, extracted!!.status)
        assertEquals(FinancialNature.REFUND, extracted.financialNature)
        assertEquals("Zomato", extracted.normalizedMerchant)
    }

    @Test
    fun `test investment transfer to Zerodha is not consumption expense`() {
        val raw = "Sent Rs. 15,000.00 to ZERODHA BROKING via UPI on 10-Oct. Ref: 778899."
        val signal = RawSignal(SourceType.SMS, "VM-HDFCBK", raw)
        val normalized = Stage3MessageNormalization.normalize(signal)

        val extracted = Stage6StructuredExtraction.extract(normalized)
        assertNotNull(extracted)
        assertEquals(FinancialNature.INVESTMENT_TRANSFER, extracted!!.financialNature)
        val (catId, _) = Stage11CategoryClassification.classify(extracted)
        assertEquals("cat_investment", catId)
    }

    @Test
    fun `test credit card bill payment is classified correctly`() {
        val raw = "Paid Rs 12,450.00 towards HDFC Credit Card Bill payment on 18-Oct. Ref: 445566."
        val signal = RawSignal(SourceType.SMS, "VM-HDFCBK", raw)
        val normalized = Stage3MessageNormalization.normalize(signal)

        val extracted = Stage6StructuredExtraction.extract(normalized)
        assertNotNull(extracted)
        assertEquals(FinancialNature.CREDIT_CARD_BILL_PAYMENT, extracted!!.financialNature)
    }

    @Test
    fun `test payment reminder is dropped at stage 4`() {
        val raw = "Dear Customer, your total amount due on card ending 4321 is Rs. 18,920.00. Payment due date is 12-Nov. Please pay to avoid late fee."
        val signal = RawSignal(SourceType.SMS, "AD-ICICIB", raw)
        val normalized = Stage3MessageNormalization.normalize(signal)

        val dropReason = Stage4SpamAndReminderFilter.filter(normalized)
        assertEquals(DropReason.PAYMENT_REMINDER_NOT_TRANSACTION, dropReason)
    }

    @Test
    fun `test e-statement generated notice is dropped at stage 4`() {
        val raw = "Your e-statement for HDFC Bank Credit Card ending 9876 is generated. Total Due: Rs 42,500.00. Min Due: Rs 2,500.00. Pay by 20-Nov."
        val signal = RawSignal(SourceType.SMS, "VM-HDFCBK", raw)
        val normalized = Stage3MessageNormalization.normalize(signal)

        val dropReason = Stage4SpamAndReminderFilter.filter(normalized)
        assertEquals(DropReason.PAYMENT_REMINDER_NOT_TRANSACTION, dropReason)
    }

    @Test
    fun `test upcoming auto debit mandate reminder is dropped at stage 4`() {
        val raw = "Reminder: Your SIP mandate of INR 5,000 will be debited from A/c 5432 on 15-Nov-26 towards NIPPON INDIA MF."
        val signal = RawSignal(SourceType.SMS, "AX-UTIBNK", raw)
        val normalized = Stage3MessageNormalization.normalize(signal)

        val dropReason = Stage4SpamAndReminderFilter.filter(normalized)
        assertEquals(DropReason.PAYMENT_REMINDER_NOT_TRANSACTION, dropReason)
    }

    @Test
    fun `test utility bill due notice is dropped at stage 4`() {
        val raw = "Dear Consumer, your electricity bill of Rs 1,480.00 is due on 10-Nov. Pay immediately to avoid disconnection."
        val signal = RawSignal(SourceType.SMS, "TD-BESCOM", raw)
        val normalized = Stage3MessageNormalization.normalize(signal)

        val dropReason = Stage4SpamAndReminderFilter.filter(normalized)
        assertEquals(DropReason.PAYMENT_REMINDER_NOT_TRANSACTION, dropReason)
    }

    @Test
    fun `test promotional loan spam is dropped at stage 4`() {
        val raw = "Congratulations! You are pre-approved for personal loan up to Rs. 5,00,000 at lowest interest rates. Click here to avail: bit.ly/loan"
        val signal = RawSignal(SourceType.SMS, "BZ-LOAN", raw)
        val normalized = Stage3MessageNormalization.normalize(signal)

        val dropReason = Stage4SpamAndReminderFilter.filter(normalized)
        assertEquals(DropReason.SPAM_OR_PROMOTIONAL, dropReason)
    }

    @Test
    fun `test personal phone number SMS is dropped as non-financial`() {
        val raw = "Hey, can you please send me Rs 500 when you get a chance?"
        val signal = RawSignal(SourceType.SMS, "+919876543210", raw)
        val normalized = Stage3MessageNormalization.normalize(signal)

        val dropReason = Stage4SpamAndReminderFilter.filter(normalized)
        assertEquals(DropReason.NON_FINANCIAL_MESSAGE, dropReason)
    }

    @Test
    fun `test reward points received is rejected in stage 5`() {
        val raw = "Congratulations, 500 reward points credited to your credit card rewards account."
        assertFalse(Stage5TransactionDetection.isTransaction(raw))
    }

    @Test
    fun `test otp message is dropped at stage 4`() {
        val raw = "Your secret OTP is 482910 for NetBanking login. Do not share this with anyone including bank officials."
        val signal = RawSignal(SourceType.SMS, "VM-HDFCBK", raw)
        val normalized = Stage3MessageNormalization.normalize(signal)

        val dropReason = Stage4SpamAndReminderFilter.filter(normalized)
        assertEquals(DropReason.AUTHENTICATION_OTP, dropReason)
    }
}
