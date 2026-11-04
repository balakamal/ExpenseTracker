package com.innovatex.spendwell.security

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SensitiveDataSanitizerTest {

    @Test
    fun `test otp redaction in logging`() {
        val raw = "Your OTP is 482910 for NetBanking login at HDFC"
        val sanitized = SensitiveDataSanitizer.sanitizeForLogging(raw)
        assertFalse(sanitized.contains("482910"))
        assertTrue(sanitized.contains("[REDACTED]"))
    }

    @Test
    fun `test card number masking`() {
        val raw = "Transaction on card 4111-2222-3333-4444 approved"
        val sanitized = SensitiveDataSanitizer.sanitizeForLogging(raw)
        assertFalse(sanitized.contains("4111-2222-3333"))
        assertTrue(sanitized.contains("••••-••••-••••-4444"))
    }

    @Test
    fun `test authentication message detector`() {
        val otpMsg = "Do not share this OTP 849201 with anyone."
        assertTrue(SensitiveDataSanitizer.isAuthenticationOrOtpMessage(otpMsg))

        val txnMsg = "INR 500 debited from a/c 1234 to Uber. Avl bal INR 10000."
        assertFalse(SensitiveDataSanitizer.isAuthenticationOrOtpMessage(txnMsg))
    }
}
