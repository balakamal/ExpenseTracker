package com.innovatex.expensetracker.ingestion.parser

import com.innovatex.expensetracker.domain.model.PaymentMedium
import com.innovatex.expensetracker.domain.model.Transaction
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
    fun `test promotional spam SMS without debit or credit is ignored`() {
        val promoSms = "Congratulations! You have an exclusive offer for pre-approved personal loan of $10,000. Apply now!"
        assertFalse(parser.isFinancialAlert(promoSms))
        assertNull(parser.parse(promoSms))
    }

    @Test
    fun `test pre-approved loan spam with amount and apply is strictly rejected`() {
        val loanSms = "Congratulations! You are eligible for pre-approved personal loan of Rs 5,00,000 with instant credit to your account. Apply now: http://bit.ly/loan"
        assertFalse(parser.isFinancialAlert(loanSms))
        assertNull(parser.parse(loanSms))
    }

    @Test
    fun `test discount coupon promo is rejected`() {
        val discountSms = "Dear customer, flat Rs 200 discount on your purchase at Dominos! Order now on app. Use code FEAST."
        assertFalse(parser.isFinancialAlert(discountSms))
        assertNull(parser.parse(discountSms))
    }

    @Test
    fun `test cashback offer promo is rejected`() {
        val cashbackSms = "Get Rs. 500 cashback on your next purchase of Rs. 2000 using your HDFC Credit Card. Hurry! Offer valid till midnight."
        assertFalse(parser.isFinancialAlert(cashbackSms))
        assertNull(parser.parse(cashbackSms))
    }

    @Test
    fun `test bill due reminder without debit is rejected`() {
        val billSms = "Your electricity bill of Rs. 1,450 is due on 28-Oct. Pay now to avoid late fee and disconnection."
        assertFalse(parser.isFinancialAlert(billSms))
        assertNull(parser.parse(billSms))
    }

    @Test
    fun `test payment collect request is rejected`() {
        val collectSms = "Request received to pay Rs. 300 to XYZ on PhonePe. Click here to approve collect request."
        assertFalse(parser.isFinancialAlert(collectSms))
        assertNull(parser.parse(collectSms))
    }

    @Test
    fun `test telecom recharge marketing is rejected`() {
        val rechargeSms = "Recharge with Rs. 299 and get 1.5GB/day daily data and unlimited calls for 28 days. Recharge now on Airtel Thanks."
        assertFalse(parser.isFinancialAlert(rechargeSms))
        assertNull(parser.parse(rechargeSms))
    }

    @Test
    fun `test gaming and rummy spam is rejected`() {
        val gamingSms = "Win cash up to Rs. 10,000 daily! Play Rummy now. Deposit Rs. 100 and claim your bonus points."
        assertFalse(parser.isFinancialAlert(gamingSms))
        assertNull(parser.parse(gamingSms))
    }

    @Test
    fun `test personal 10-digit mobile number sender is rejected`() {
        val sms = "I sent you Rs 500 for dinner yesterday"
        val sender = "+919876543210"
        assertFalse(parser.isFinancialAlert(sms, sender))
        assertNull(parser.parse(sms, sender = sender))
    }

    @Test
    fun `test credit card limit increase notification is rejected`() {
        val limitSms = "Good news! Your credit card limit has been increased to Rs. 2,50,000. Enjoy enhanced spending power."
        assertFalse(parser.isFinancialAlert(limitSms))
        assertNull(parser.parse(limitSms))
    }

    @Test
    fun `test Indian bank SMS with promo footer is correctly parsed`() {
        val sms = "Sent Rs.500.00 from HDFC Bank A/C **1234 to Zomato on 22-10-24. UPI Ref 429618291029. Congratulations! Get 10% discount on flights."
        assertTrue(parser.isFinancialAlert(sms, sender = "AD-HDFCBK"))

        val result = parser.parse(sms, sender = "AD-HDFCBK")
        assertNotNull(result)
        assertEquals(500.00, result!!.amount)
        assertEquals("INR", result.currency)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("Zomato", result.cleanMerchant)
        assertEquals("1234", result.accountLast4)
        assertEquals("429618291029", result.referenceId)
    }

    @Test
    fun `test Indian SBI UPI debit SMS extraction`() {
        val sms = "Dear SBI UPI User, your A/c ...4321 debited by Rs.150.00 on 22Oct24 transfer to Swiggy. UPI Ref 429618291029."
        assertTrue(parser.isFinancialAlert(sms, sender = "VK-SBIINB"))

        val result = parser.parse(sms, sender = "VK-SBIINB")
        assertNotNull(result)
        assertEquals(150.00, result!!.amount)
        assertEquals("INR", result.currency)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("4321", result.accountLast4)
        assertEquals("429618291029", result.referenceId)
    }

    @Test
    fun `test Indian ICICI Dr transaction extraction`() {
        val sms = "Acct XX9876 Dr with Rs. 350.00 on 20-Oct-24 towards Blinkit. Ref: 99887766."
        assertTrue(parser.isFinancialAlert(sms, sender = "BP-ICICIB"))

        val result = parser.parse(sms, sender = "BP-ICICIB")
        assertNotNull(result)
        assertEquals(350.00, result!!.amount)
        assertEquals("INR", result.currency)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("Blinkit", result.cleanMerchant)
        assertEquals("9876", result.accountLast4)
        assertEquals("99887766", result.referenceId)
    }

    @Test
    fun `test loan EMI debit is not falsely ignored as loan marketing`() {
        val sms = "Home Loan EMI of Rs. 25,000.00 debited from A/C XX1234 on 05-Oct-24. Avl Bal: INR 80,000. Ref: 88776655"
        assertTrue(parser.isFinancialAlert(sms, sender = "AX-HDFCBK"))

        val result = parser.parse(sms, sender = "AX-HDFCBK")
        assertNotNull(result)
        assertEquals(25000.00, result!!.amount)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("1234", result.accountLast4)
        assertEquals("88776655", result.referenceId)

        val medium = PaymentMedium.detect(sms, result.merchantRaw, result.referenceId)
        assertEquals(PaymentMedium.BANK_ACCOUNT, medium)
    }

    @Test
    fun `test utility bill paid transaction is not falsely ignored as bill due reminder`() {
        val sms = "Paid Rs 1,450.00 towards electricity bill from A/c XX5678 on 22-Oct. Ref: TXN12345"
        assertTrue(parser.isFinancialAlert(sms, sender = "VM-SBIBNK"))

        val result = parser.parse(sms, sender = "VM-SBIBNK")
        assertNotNull(result)
        assertEquals(1450.00, result!!.amount)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("5678", result.accountLast4)
        assertEquals("TXN12345", result.referenceId)
    }

    @Test
    fun `test credit card purchase alert extraction and medium detection`() {
        val sms = "Spent INR 2,499.00 on your ICICI Bank Credit Card ending 9012 at Reliance Digital on 25-Oct-24. Avl Lmt: INR 95,000. Ref 11223344."
        assertTrue(parser.isFinancialAlert(sms, sender = "BP-ICICIB"))

        val result = parser.parse(sms, sender = "BP-ICICIB")
        assertNotNull(result)
        assertEquals(2499.00, result!!.amount)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("9012", result.accountLast4)
        assertEquals("11223344", result.referenceId)

        val medium = PaymentMedium.detect(sms, result.merchantRaw, result.referenceId)
        assertEquals(PaymentMedium.CARD, medium)
    }

    @Test
    fun `test UPI VPA payment extraction with merchant cleanup and medium detection`() {
        val sms = "Paid Rs. 280.00 to swiggy@icici via PhonePe on 23-Oct. UPI Ref 33445566."
        assertTrue(parser.isFinancialAlert(sms, sender = "AD-YESBNK"))

        val result = parser.parse(sms, sender = "AD-YESBNK")
        assertNotNull(result)
        assertEquals(280.00, result!!.amount)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("Swiggy", result.cleanMerchant)
        assertEquals("33445566", result.referenceId)

        val medium = PaymentMedium.detect(sms, result.merchantRaw, result.referenceId)
        assertEquals(PaymentMedium.UPI, medium)
    }

    @Test
    fun `test ATM cash withdrawal extraction and medium detection`() {
        val sms = "Rs. 2,000.00 withdrawn from ATM using Debit Card XX7788 on 21-Oct. Avl Bal: Rs 15,200. Txn: 556677"
        assertTrue(parser.isFinancialAlert(sms, sender = "VM-KOTAKB"))

        val result = parser.parse(sms, sender = "VM-KOTAKB")
        assertNotNull(result)
        assertEquals(2000.00, result!!.amount)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("7788", result.accountLast4)
        assertEquals("556677", result.referenceId)

        val medium = PaymentMedium.detect(sms, result.merchantRaw, result.referenceId)
        assertEquals(PaymentMedium.ATM, medium)
    }

    @Test
    fun `test Netbanking and Wallet medium detections`() {
        val netbankingText = "Transferred Rs. 10,000 via IMPS netbanking to Amit Sharma. Ref IMPS889900."
        assertEquals(PaymentMedium.NETBANKING, PaymentMedium.detect(netbankingText, null, "IMPS889900"))

        val walletText = "Paid Rs. 150.00 using Paytm Wallet to Cafe Coffee Day."
        assertEquals(PaymentMedium.WALLET, PaymentMedium.detect(walletText, "Cafe Coffee Day", null))
    }

    @Test
    fun `test transaction sender property returns notes or fallback`() {
        val txnWithNotes = Transaction(
            amount = 100.0,
            type = TransactionType.DEBIT,
            timestamp = 1000L,
            monthKey = "2026-10",
            merchantRaw = "Swiggy",
            merchantClean = "Swiggy",
            notes = "AD-HDFCBK"
        )
        assertEquals("AD-HDFCBK", txnWithNotes.sender)

        val txnWithSnippetFallback = Transaction(
            amount = 50.0,
            type = TransactionType.DEBIT,
            timestamp = 1000L,
            monthKey = "2026-10",
            merchantRaw = "Unknown Merchant",
            merchantClean = "Unknown Merchant",
            rawSnippet = "[Sender: VM-SBIBNK] Sent Rs 50 to merchant"
        )
        assertEquals("VM-SBIBNK", txnWithSnippetFallback.sender)

        val txnWithMerchantFallback = Transaction(
            amount = 50.0,
            type = TransactionType.DEBIT,
            timestamp = 1000L,
            monthKey = "2026-10",
            merchantRaw = "AD-KOTAKB",
            merchantClean = "Kotak"
        )
        assertEquals("AD-KOTAKB", txnWithMerchantFallback.sender)
    }
}

