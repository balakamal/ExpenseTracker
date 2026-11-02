package com.innovatex.expensetracker.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Calendar

class SalaryCycleHelperTest {

    private fun createTimestamp(year: Int, month1Based: Int, day: Int, hour: Int = 12): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month1Based - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun `standard calendar month when salaryCreditDay is 1`() {
        val midOct = createTimestamp(2026, 10, 15)
        val endOct = createTimestamp(2026, 10, 31)
        val startOct = createTimestamp(2026, 10, 1)

        assertEquals("2026-10", SalaryCycleHelper.getCycleMonthKey(midOct, 1))
        assertEquals("2026-10", SalaryCycleHelper.getCycleMonthKey(endOct, 1))
        assertEquals("2026-10", SalaryCycleHelper.getCycleMonthKey(startOct, 1))

        val (start, end) = SalaryCycleHelper.getCycleRange("2026-10", 1)
        val startCal = Calendar.getInstance().apply { timeInMillis = start }
        val endCal = Calendar.getInstance().apply { timeInMillis = end }

        assertEquals(1, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(31, endCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, endCal.get(Calendar.MINUTE))
        assertEquals(59, endCal.get(Calendar.SECOND))

        assertEquals("October 2026", SalaryCycleHelper.formatCycleLabel("2026-10", 1))
        assertEquals(31, SalaryCycleHelper.getDaysInCycle("2026-10", 1))
    }

    @Test
    fun `custom salary credit day 25th partitions transactions accurately`() {
        val salaryDay = 25

        // On or after 25th -> current month cycle "2026-10"
        val oct25 = createTimestamp(2026, 10, 25)
        val oct27 = createTimestamp(2026, 10, 27)
        val nov24 = createTimestamp(2026, 11, 24)

        assertEquals("2026-10", SalaryCycleHelper.getCycleMonthKey(oct25, salaryDay))
        assertEquals("2026-10", SalaryCycleHelper.getCycleMonthKey(oct27, salaryDay))
        assertEquals("2026-10", SalaryCycleHelper.getCycleMonthKey(nov24, salaryDay))

        // Before 25th -> previous month cycle "2026-09"
        val oct24 = createTimestamp(2026, 10, 24)
        val oct1 = createTimestamp(2026, 10, 1)

        assertEquals("2026-09", SalaryCycleHelper.getCycleMonthKey(oct24, salaryDay))
        assertEquals("2026-09", SalaryCycleHelper.getCycleMonthKey(oct1, salaryDay))

        // Year boundary: Jan 5 belongs to Dec of previous year
        val jan5 = createTimestamp(2026, 1, 5)
        assertEquals("2025-12", SalaryCycleHelper.getCycleMonthKey(jan5, salaryDay))
    }

    @Test
    fun `custom salary credit day range calculation`() {
        val (start, end) = SalaryCycleHelper.getCycleRange("2026-10", 25)
        val startCal = Calendar.getInstance().apply { timeInMillis = start }
        val endCal = Calendar.getInstance().apply { timeInMillis = end }

        assertEquals(2026, startCal.get(Calendar.YEAR))
        assertEquals(Calendar.OCTOBER, startCal.get(Calendar.MONTH))
        assertEquals(25, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))

        assertEquals(2026, endCal.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, endCal.get(Calendar.MONTH))
        assertEquals(24, endCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, endCal.get(Calendar.MINUTE))
        assertEquals(59, endCal.get(Calendar.SECOND))

        val label = SalaryCycleHelper.formatCycleLabel("2026-10", 25)
        assertTrue(label.contains("Oct 2026"), "Expected month in label, got $label")
        assertTrue(label.contains("25 Oct"), "Expected start date in label, got $label")
        assertTrue(label.contains("24 Nov"), "Expected end date in label, got $label")

        assertEquals(31, SalaryCycleHelper.getDaysInCycle("2026-10", 25))
    }

    @Test
    fun `shorter month boundary with 31st salary credit day`() {
        // February does not have 31 days; clamped to 28 (in non-leap year)
        val (start, end) = SalaryCycleHelper.getCycleRange("2025-02", 31)
        val startCal = Calendar.getInstance().apply { timeInMillis = start }
        val endCal = Calendar.getInstance().apply { timeInMillis = end }

        assertEquals(Calendar.FEBRUARY, startCal.get(Calendar.MONTH))
        assertEquals(28, startCal.get(Calendar.DAY_OF_MONTH))

        // End is before next cycle (March 31) -> March 30
        assertEquals(Calendar.MARCH, endCal.get(Calendar.MONTH))
        assertEquals(30, endCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `clamping values outside 1 to 31`() {
        val midOct = createTimestamp(2026, 10, 15)
        // 0 clamps to 1
        assertEquals("2026-10", SalaryCycleHelper.getCycleMonthKey(midOct, 0))
        // 35 clamps to 31
        assertEquals("2026-09", SalaryCycleHelper.getCycleMonthKey(midOct, 35))
    }
}

