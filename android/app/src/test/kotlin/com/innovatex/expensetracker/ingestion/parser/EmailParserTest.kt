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
        assertEquals("9012", result.accountLast4)
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
