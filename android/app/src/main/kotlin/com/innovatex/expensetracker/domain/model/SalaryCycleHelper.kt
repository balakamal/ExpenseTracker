package com.innovatex.expensetracker.domain.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object SalaryCycleHelper {

    /**
     * Determines the cycle monthKey string "yyyy-MM" corresponding to the start of the salary cycle
     * for a given timestamp and configured salary credit day (1..31).
     *
     * Example with salaryCreditDay = 25:
     * - 2026-10-27 -> "2026-10" (Cycle started 25 Oct)
     * - 2026-10-15 -> "2026-09" (Cycle started 25 Sep)
     * - 2026-01-05 -> "2025-12" (Cycle started 25 Dec)
     */
    fun getCycleMonthKey(timestamp: Long, salaryCreditDay: Int): String {
        val clampedDay = salaryCreditDay.coerceIn(1, 31)
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }

        if (clampedDay == 1) {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
            return sdf.format(cal.time)
        }

        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val maxDaysThisMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val effectiveCreditDay = minOf(clampedDay, maxDaysThisMonth)

        val cycleStartCal = if (currentDay >= effectiveCreditDay) {
            // Belongs to current month's salary cycle
            cal
        } else {
            // Belongs to previous month's salary cycle
            (cal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        }

        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        return sdf.format(cycleStartCal.time)
    }

    /**
     * Returns the exact start millisecond (00:00:00.000) and end millisecond (23:59:59.999)
     * for a given cycle monthKey ("yyyy-MM") and salary credit day.
     *
     * Correctly accommodates variable month lengths (e.g. February, 30-day months).
     */
    fun getCycleRange(monthKey: String, salaryCreditDay: Int): Pair<Long, Long> {
        val clampedDay = salaryCreditDay.coerceIn(1, 31)
        val parts = monthKey.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
        val month = (parts.getOrNull(1)?.toIntOrNull() ?: (Calendar.getInstance().get(Calendar.MONTH) + 1)) - 1

        val startCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, minOf(clampedDay, maxDay))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endCal = if (clampedDay == 1) {
            // End of standard calendar month
            (startCal.clone() as Calendar).apply {
                val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                set(Calendar.DAY_OF_MONTH, maxDay)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
        } else {
            // The day before next month's salary credit date
            val nextMonthCal = (startCal.clone() as Calendar).apply {
                add(Calendar.MONTH, 1)
                val maxDayNext = getActualMaximum(Calendar.DAY_OF_MONTH)
                set(Calendar.DAY_OF_MONTH, minOf(clampedDay, maxDayNext))
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            nextMonthCal.apply {
                add(Calendar.MILLISECOND, -1)
            }
        }

        return Pair(startCal.timeInMillis, endCal.timeInMillis)
    }

    /**
     * Returns the exact start and end millisecond timestamps for the active cycle
     * containing the specified timestamp.
     */
    fun getCycleRangeForTimestamp(timestamp: Long, salaryCreditDay: Int): Pair<Long, Long> {
        val monthKey = getCycleMonthKey(timestamp, salaryCreditDay)
        return getCycleRange(monthKey, salaryCreditDay)
    }

    /**
     * Human-readable label for a month key given salary credit day.
     * Examples:
     * - "October 2026" (salaryCreditDay == 1)
     * - "Oct 2026 (25 Oct – 24 Nov)" (salaryCreditDay == 25)
     */
    fun formatCycleLabel(monthKey: String, salaryCreditDay: Int): String {
        val clampedDay = salaryCreditDay.coerceIn(1, 31)
        val (startMillis, endMillis) = getCycleRange(monthKey, clampedDay)

        return if (clampedDay == 1) {
            val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            sdf.format(Date(startMillis))
        } else {
            val startMonthFmt = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(startMillis))
            val startDayFmt = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(startMillis))
            val endDayFmt = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(endMillis))
            "$startMonthFmt ($startDayFmt – $endDayFmt)"
        }
    }

    /**
     * Returns the total days in the salary cycle.
     */
    fun getDaysInCycle(monthKey: String, salaryCreditDay: Int): Int {
        val (start, end) = getCycleRange(monthKey, salaryCreditDay)
        val diffDays = ((end - start) / (24 * 60 * 60 * 1000L)).toInt() + 1
        return diffDays.coerceAtLeast(1)
    }
}

