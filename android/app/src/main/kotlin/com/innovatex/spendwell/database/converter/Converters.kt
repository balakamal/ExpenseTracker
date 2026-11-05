package com.innovatex.spendwell.database.converter

import androidx.room.TypeConverter
import com.innovatex.spendwell.common.Money
import java.math.BigDecimal
import java.math.RoundingMode

class Converters {

    @TypeConverter
    fun fromMoney(money: Money?): Long? {
        return money?.toPaisa()
    }

    @TypeConverter
    fun toMoney(paisa: Long?): Money? {
        return paisa?.let { Money.fromPaisa(it) }
    }

    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? {
        return value?.setScale(Money.DECIMAL_SCALE, RoundingMode.HALF_EVEN)?.toPlainString()
    }

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? {
        return value?.let { BigDecimal(it).setScale(Money.DECIMAL_SCALE, RoundingMode.HALF_EVEN) }
    }
}
