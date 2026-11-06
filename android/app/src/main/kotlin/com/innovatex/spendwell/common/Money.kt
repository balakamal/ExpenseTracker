package com.innovatex.spendwell.common

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Decimal-safe representation for all monetary calculations in Spendwell.
 * Backed by BigDecimal with 4 decimal places internally and 2 decimal places for display/storage.
 * Guarantees zero floating-point precision loss.
 */
data class Money(
    val amount: BigDecimal = BigDecimal.ZERO.setScale(DECIMAL_SCALE, RoundingMode.HALF_EVEN),
    val currencyCode: String = DEFAULT_CURRENCY
) : Comparable<Money> {

    companion object {
        const val DECIMAL_SCALE = 4
        const val DISPLAY_SCALE = 2
        const val DEFAULT_CURRENCY = "INR"

        val ZERO = Money(BigDecimal.ZERO.setScale(DECIMAL_SCALE, RoundingMode.HALF_EVEN))

        fun fromDouble(value: Double, currencyCode: String = DEFAULT_CURRENCY): Money {
            val bd = BigDecimal.valueOf(value).setScale(DECIMAL_SCALE, RoundingMode.HALF_EVEN)
            return Money(bd, currencyCode)
        }

        fun fromRupees(value: Double, currencyCode: String = DEFAULT_CURRENCY): Money {
            return fromDouble(value, currencyCode)
        }

        fun fromRupees(value: Number, currencyCode: String = DEFAULT_CURRENCY): Money {
            return fromDouble(value.toDouble(), currencyCode)
        }

        fun fromString(value: String, currencyCode: String = DEFAULT_CURRENCY): Money {
            val cleaned = value.replace(",", "").trim()
            val bd = try {
                BigDecimal(cleaned).setScale(DECIMAL_SCALE, RoundingMode.HALF_EVEN)
            } catch (e: Exception) {
                BigDecimal.ZERO.setScale(DECIMAL_SCALE, RoundingMode.HALF_EVEN)
            }
            return Money(bd, currencyCode)
        }

        fun fromPaisa(paisa: Long, currencyCode: String = DEFAULT_CURRENCY): Money {
            val bd = BigDecimal(paisa).divide(BigDecimal(100), DECIMAL_SCALE, RoundingMode.HALF_EVEN)
            return Money(bd, currencyCode)
        }
    }

    val isPositive: Boolean get() = amount > BigDecimal.ZERO
    val isNegative: Boolean get() = amount < BigDecimal.ZERO
    val isZero: Boolean get() = amount.compareTo(BigDecimal.ZERO) == 0

    operator fun plus(other: Money): Money {
        require(currencyCode == other.currencyCode) { "Cannot add different currencies: $currencyCode and ${other.currencyCode}" }
        return Money(amount.add(other.amount).setScale(DECIMAL_SCALE, RoundingMode.HALF_EVEN), currencyCode)
    }

    operator fun minus(other: Money): Money {
        require(currencyCode == other.currencyCode) { "Cannot subtract different currencies: $currencyCode and ${other.currencyCode}" }
        return Money(amount.subtract(other.amount).setScale(DECIMAL_SCALE, RoundingMode.HALF_EVEN), currencyCode)
    }

    operator fun times(multiplier: BigDecimal): Money {
        return Money(amount.multiply(multiplier).setScale(DECIMAL_SCALE, RoundingMode.HALF_EVEN), currencyCode)
    }

    operator fun times(multiplier: Long): Money {
        return times(BigDecimal(multiplier))
    }

    operator fun unaryMinus(): Money {
        return Money(amount.negate().setScale(DECIMAL_SCALE, RoundingMode.HALF_EVEN), currencyCode)
    }

    fun abs(): Money = Money(amount.abs().setScale(DECIMAL_SCALE, RoundingMode.HALF_EVEN), currencyCode)

    fun toPaisa(): Long {
        return amount.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_EVEN).toLong()
    }

    fun toDisplayDouble(): Double {
        return amount.setScale(DISPLAY_SCALE, RoundingMode.HALF_EVEN).toDouble()
    }

    override fun compareTo(other: Money): Int {
        require(currencyCode == other.currencyCode) { "Cannot compare different currencies: $currencyCode and ${other.currencyCode}" }
        return amount.compareTo(other.amount)
    }

    /**
     * Formats according to the Indian Rupee numbering format (Lakhs, Crores):
     * e.g. ₹1,23,456.78
     */
    fun formatInr(includeSymbol: Boolean = true): String {
        val displayAmount = amount.abs().setScale(DISPLAY_SCALE, RoundingMode.HALF_EVEN)
        val plain = displayAmount.toPlainString()
        val parts = plain.split(".")
        val integerPart = parts[0]
        val fractionPart = if (parts.size > 1) parts[1] else "00"

        val formattedInteger = formatIndianGrouping(integerPart)
        val formattedNumber = "$formattedInteger.$fractionPart"

        return when {
            includeSymbol && currencyCode == "INR" -> "₹ $formattedNumber"
            includeSymbol -> "$currencyCode $formattedNumber"
            else -> formattedNumber
        }
    }

    fun formatSigned(isCredit: Boolean): String {
        val sign = if (isCredit) "+ " else "- "
        return "$sign${formatInr(true)}"
    }

    private fun formatIndianGrouping(digits: String): String {
        if (digits.length <= 3) return digits
        val lastThree = digits.substring(digits.length - 3)
        val rest = digits.substring(0, digits.length - 3)
        val sb = StringBuilder()
        var count = 0
        for (i in rest.length - 1 downTo 0) {
            sb.append(rest[i])
            count++
            if (count % 2 == 0 && i != 0) {
                sb.append(",")
            }
        }
        return sb.reverse().toString() + "," + lastThree
    }
}
