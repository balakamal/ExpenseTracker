package com.innovatex.expensetracker.ingestion.parser

import com.innovatex.expensetracker.domain.model.TransactionType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SmsParserTest {

    private lateinit var parser: SmsParserEngine

    @BeforeEach
    fun setup() {
        parser = SmsParserEngine()
    }

    @Test
    fun `test standard debit SMS extraction`() {
        val sms = "Your Acct XX4321 debited for USD 58.20 at Whole Foods on 22-Oct. Avl bal: USD 1,420.00. Ref: TXN998811"
        assertTrue(parser.isFinancialAlert(sms))

        val result = parser.parse(sms)
        assertNotNull(result)
        assertEquals(58.20, result!!.amount)
        assertEquals("USD", result.currency)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("Whole Foods", result.cleanMerchant)
        assertEquals("4321", result.accountLast4)
        assertEquals("TXN998811", result.referenceId)
    }

    @Test
    fun `test credit refund SMS extraction`() {
        val sms = "INR 1,250.00 credited to a/c **8899 on 21-Oct from Amazon Refund. UTR 77665544."
        assertTrue(parser.isFinancialAlert(sms))

        val result = parser.parse(sms)
        assertNotNull(result)
        assertEquals(1250.00, result!!.amount)
        assertEquals("INR", result.currency)
        assertEquals(TransactionType.CREDIT, result.type)
        assertEquals("Amazon Refund", result.cleanMerchant)
        assertEquals("8899", result.accountLast4)
        assertEquals("77665544", result.referenceId)
    }

    @Test
    fun `test paid for service SMS extraction`() {
        val sms = "Paid USD 24.50 for Uber on 10-Oct using card ending 5544. Ref: UBR998822"
        assertTrue(parser.isFinancialAlert(sms))

        val result = parser.parse(sms)
        assertNotNull(result)
        assertEquals(24.50, result!!.amount)
        assertEquals("USD", result.currency)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("Uber", result.cleanMerchant)
        assertEquals("5544", result.accountLast4)
        assertEquals("UBR998822", result.referenceId)
    }

    @Test
    fun `test non-financial OTP message is ignored`() {
        val otpSms = "Your one time password (OTP) for banking login is 482910. Do not share this with anyone."
        assertFalse(parser.isFinancialAlert(otpSms))
        assertNull(parser.parse(otpSms))
    }

    @Test
    fun `test promotional spam SMS is ignored`() {
        val promoSms = "Congratulations! You have an exclusive offer for pre-approved personal loan of $10,000. Apply now!"
        assertFalse(parser.isFinancialAlert(promoSms))
        assertNull(parser.parse(promoSms))
    }
}
