package com.innovatex.expensetracker.ingestion.parser

import com.innovatex.expensetracker.domain.model.SourceChannel
import com.innovatex.expensetracker.domain.model.TransactionType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EmailParserTest {

    private lateinit var emailParser: EmailParserEngine

    @BeforeEach
    fun setup() {
        emailParser = EmailParserEngine(SmsParserEngine())
    }

    @Test
    fun `test email transaction alert parsing from Gmail`() {
        val result = emailParser.parseEmailNotification(
            packageName = "com.google.android.gm",
            title = "Chase Alert: Transaction of $42.50 at Trader Joe's",
            text = "Your card ending in 9012 was charged USD 42.50 on 22-Oct. Ref: CHASE8891",
            bigText = null
        )

        assertNotNull(result)
        assertEquals(42.50, result!!.amount)
        assertEquals("USD", result.currency)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals(SourceChannel.EMAIL, result.sourceChannel)
        assertEquals("Trader Joe's", result.cleanMerchant)
        assertEquals("9012", result.accountLast4)
    }

    @Test
    fun `test marketing discount email is rejected`() {
        val result = emailParser.parseEmailNotification(
            packageName = "com.google.android.gm",
            title = "Swiggy Deals: Flat 50% off on your next order",
            text = "Hungry? Use coupon code FEAST to get up to Rs 150 discount on orders above Rs 299.",
            bigText = null
        )
        assertNull(result)
    }

    @Test
    fun `test ecommerce delivery tracking email is rejected`() {
        val result = emailParser.parseEmailNotification(
            packageName = "com.google.android.gm",
            title = "Amazon.in: Package out for delivery",
            text = "Your order #402-998811-1122 is arriving today by 8 PM.",
            bigText = null
        )
        assertNull(result)
    }

    @Test
    fun `test cart abandonment email is rejected`() {
        val result = emailParser.parseEmailNotification(
            packageName = "com.google.android.gm",
            title = "ZARA: Items in your cart are waiting",
            text = "You left items worth $89.00 in your shopping cart. Complete your purchase now.",
            bigText = null
        )
        assertNull(result)
    }

    @Test
    fun `test genuine Uber email receipt is parsed`() {
        val result = emailParser.parseEmailNotification(
            packageName = "com.google.android.gm",
            title = "Receipt for your trip with Uber",
            text = "Total USD 18.50 paid for Uber using card ending 3421 on 24-Oct. Ref: UBR9876",
            bigText = null
        )

        assertNotNull(result)
        assertEquals(18.50, result!!.amount)
        assertEquals("USD", result.currency)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("Uber", result.cleanMerchant)
        assertEquals("3421", result.accountLast4)
    }

    @Test
    fun `test unsupported package is rejected`() {
        val result = emailParser.parseEmailNotification(
            packageName = "com.random.unsupported.app",
            title = "Transaction Alert",
            text = "$10 spent",
            bigText = null
        )
        assertNull(result)
    }
}
