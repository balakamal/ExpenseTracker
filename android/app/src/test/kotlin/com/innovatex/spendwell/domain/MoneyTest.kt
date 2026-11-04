package com.innovatex.spendwell.domain

import com.innovatex.spendwell.common.Money
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class MoneyTest {

    @Test
    fun `test money creation and exact decimal addition`() {
        val m1 = Money.fromString("100.10")
        val m2 = Money.fromString("200.20")
        val result = m1 + m2

        assertEquals("₹ 300.30", result.formatInr())
        assertEquals(3003000L, result.toPaisa() * 100) // 300.30 * 10000 internal scale
    }

    @Test
    fun `test money indian numbering grouping format`() {
        val thousand = Money.fromString("1234.50")
        assertEquals("₹ 1,234.50", thousand.formatInr())

        val lakh = Money.fromString("152345.75")
        assertEquals("₹ 1,52,345.75", lakh.formatInr())

        val crore = Money.fromString("12345678.90")
        assertEquals("₹ 1,23,45,678.90", crore.formatInr())
    }

    @Test
    fun `test zero floating point drift on repeated addition`() {
        var total = Money.ZERO
        val step = Money.fromString("0.10")
        for (i in 1..10) {
            total += step
        }
        assertEquals("₹ 1.00", total.formatInr())
        assertEquals(BigDecimal("1.0000"), total.amount)
    }

    @Test
    fun `test money comparisons`() {
        val m1 = Money.fromString("50.00")
        val m2 = Money.fromString("100.00")
        assertTrue(m1 < m2)
        assertTrue(m2 > m1)
        assertFalse(m1.isZero)
        assertTrue(Money.ZERO.isZero)
    }
}
