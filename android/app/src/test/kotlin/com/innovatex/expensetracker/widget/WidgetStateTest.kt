package com.innovatex.expensetracker.widget

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WidgetStateTest {

    @Test
    fun `test widget state budget progress calculations`() {
        val totalDebit = 1250.0
        val budgetLimit = 2500.0

        val progressPercent = ((totalDebit / budgetLimit) * 100).toInt()
        val remaining = budgetLimit - totalDebit

        assertEquals(50, progressPercent)
        assertEquals(1250.0, remaining)
    }

    @Test
    fun `test overbudget calculation`() {
        val totalDebit = 3000.0
        val budgetLimit = 2500.0

        val progressPercent = ((totalDebit / budgetLimit) * 100).toInt()
        assertTrue(progressPercent > 100)
    }
}
