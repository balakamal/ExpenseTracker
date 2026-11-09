package com.innovatex.spendwell.integrations.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.innovatex.spendwell.MainActivity
import com.innovatex.spendwell.analytics.DailyFinancialSummary
import com.innovatex.spendwell.analytics.FinancialAnalyticsEngine
import com.innovatex.spendwell.analytics.MonthlyFinancialSummary
import com.innovatex.spendwell.analytics.WeeklyFinancialSummary
import com.innovatex.spendwell.common.Money
import com.innovatex.spendwell.data.preferences.UserPreferencesRepository
import com.innovatex.spendwell.domain.model.Transaction
import com.innovatex.spendwell.domain.model.TransactionType
import com.innovatex.spendwell.domain.repository.AccountRepository
import com.innovatex.spendwell.domain.repository.TransactionRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar
import java.util.Locale

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SpendwellWidgetEntryPoint {
    fun transactionRepository(): TransactionRepository
    fun accountRepository(): AccountRepository
    fun userPreferencesRepository(): UserPreferencesRepository
}

object SpendwellWidgetManager {
    suspend fun updateAllWidgets(context: Context) {
        try {
            SmallSpendwellGlanceWidget().updateAll(context)
        } catch (_: Throwable) {}
        try {
            MediumSpendwellGlanceWidget().updateAll(context)
        } catch (_: Throwable) {}
        try {
            LargeSpendwellGlanceWidget().updateAll(context)
        } catch (_: Throwable) {}
    }
}

// -------------------------------------------------------------
// 1. SMALL WIDGET (2x2) - TODAY'S RUN-RATE
// -------------------------------------------------------------
class SmallSpendwellGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetFinancialData(context)

        provideContent {
            GlanceTheme {
                SmallWidgetContent(data)
            }
        }
    }

    @Composable
    private fun SmallWidgetContent(data: WidgetSnapshot) {
        val d = data.daily
        val isOverAvg = d.comparisonTo30DayAvgPct > 0

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(18.dp)
                .background(Color(0xFF0C101A))
                .padding(14.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = GlanceModifier
                        .cornerRadius(4.dp)
                        .background(Color(0xFF3D6FDF))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "TODAY",
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = "${d.transactionCount} txns",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF8899A6)),
                        fontSize = 10.sp
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            Text(
                text = d.totalExpenses.formatInr(true),
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = GlanceModifier.height(4.dp))

            val statusText = when {
                d.totalExpenses.isZero -> "No spends today"
                isOverAvg -> "+${d.comparisonTo30DayAvgPct.toInt()}% vs avg"
                else -> "${d.comparisonTo30DayAvgPct.toInt()}% vs avg"
            }
            val statusColor = when {
                d.totalExpenses.isZero -> Color(0xFF10B981)
                isOverAvg -> Color(0xFFEF4444)
                else -> Color(0xFF10B981)
            }

            Text(
                text = statusText,
                style = TextStyle(
                    color = ColorProvider(statusColor),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

// -------------------------------------------------------------
// 2. MEDIUM WIDGET (4x2) - WEEKLY & MONTHLY CYCLE
// -------------------------------------------------------------
class MediumSpendwellGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetFinancialData(context)

        provideContent {
            GlanceTheme {
                MediumWidgetContent(data)
            }
        }
    }

    @Composable
    private fun MediumWidgetContent(data: WidgetSnapshot) {
        val w = data.weekly
        val m = data.monthly

        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(18.dp)
                .background(Color(0xFF0C101A))
                .padding(16.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: This Week
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "THIS WEEK'S SPEND",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF8899A6)),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.height(3.dp))
                Text(
                    text = w.totalExpenses.formatInr(true),
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                val isWoWDown = w.weekOverWeekPercentage <= 0
                Text(
                    text = "WoW ${String.format(Locale.ENGLISH, "%+.0f", w.weekOverWeekPercentage)}% • ${w.peakDayLabel.take(8)}",
                    style = TextStyle(
                        color = ColorProvider(if (isWoWDown) Color(0xFF10B981) else Color(0xFFEF4444)),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Spacer(modifier = GlanceModifier.width(12.dp))

            // Right Column: Monthly Net & Inflow
            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "MONTHLY SAVINGS",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF8899A6)),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.height(3.dp))
                Text(
                    text = m.netSavings.formatInr(true),
                    style = TextStyle(
                        color = ColorProvider(if (m.netSavings.isPositive) Color(0xFF10B981) else Color(0xFFEF4444)),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = "${m.savingsRatePercentage.toInt()}% saved (Daily ₹${m.averageDailySpend.toDisplayDouble().toInt()})",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF3D6FDF)),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 3. LARGE WIDGET (4x4) - LIVE LEDGER CONTROL CENTER
// -------------------------------------------------------------
class LargeSpendwellGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetFinancialData(context)

        provideContent {
            GlanceTheme {
                LargeWidgetContent(data)
            }
        }
    }

    @Composable
    private fun LargeWidgetContent(data: WidgetSnapshot) {
        val d = data.daily
        val w = data.weekly
        val m = data.monthly

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(20.dp)
                .background(Color(0xFF0C101A))
                .padding(18.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            // Header Bar
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SPENDWELL LEDGER",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF8899A6)),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Box(
                    modifier = GlanceModifier
                        .cornerRadius(4.dp)
                        .background(Color(0xFF10B981).copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "LIVE",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF10B981)),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            // Main Cash Flow / Savings Figure
            Text(
                text = m.netSavings.formatInr(true),
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = GlanceModifier.height(14.dp))

            // 3-Metric Horizon: Today | This Week | This Month
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                MetricColumn(title = "TODAY", amount = d.totalExpenses.formatInr(true), modifier = GlanceModifier.defaultWeight())
                MetricColumn(title = "THIS WEEK", amount = w.totalExpenses.formatInr(true), modifier = GlanceModifier.defaultWeight())
                MetricColumn(title = "THIS MONTH", amount = m.totalExpenses.formatInr(true), modifier = GlanceModifier.defaultWeight())
            }

            Spacer(modifier = GlanceModifier.height(14.dp))

            // Recent 3 Transactions Feed
            Text(
                text = "RECENT ACTIVITY",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF8899A6)),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = GlanceModifier.height(6.dp))

            if (data.recentTransactions.isEmpty()) {
                Text(
                    text = "No recent transactions logged",
                    style = TextStyle(color = ColorProvider(Color(0xFF556677)), fontSize = 12.sp)
                )
            } else {
                data.recentTransactions.take(3).forEach { tx ->
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tx.merchantName.ifBlank { "Transfer" }.take(18),
                            style = TextStyle(color = ColorProvider(Color.White), fontSize = 12.sp, fontWeight = FontWeight.Medium),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        val isCredit = tx.type == TransactionType.CREDIT
                        Text(
                            text = (if (isCredit) "+ " else "- ") + tx.amount.formatInr(true),
                            style = TextStyle(
                                color = ColorProvider(if (isCredit) Color(0xFF10B981) else Color(0xFFEF4444)),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            // Footer Pace
            Text(
                text = "Burn Rate: ₹${m.averageDailySpend.toDisplayDouble().toInt()}/day • Projected: ${m.projectedMonthEndSpend.formatInr(true)}",
                style = TextStyle(color = ColorProvider(Color(0xFF3D6FDF)), fontSize = 11.sp, fontWeight = FontWeight.Medium)
            )
        }
    }

    @Composable
    private fun MetricColumn(title: String, amount: String, modifier: GlanceModifier) {
        Column(modifier = modifier) {
            Text(
                text = title,
                style = TextStyle(color = ColorProvider(Color(0xFF8899A6)), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = amount,
                style = TextStyle(color = ColorProvider(Color.White), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
}

// -------------------------------------------------------------
// DATA LOADER & RECEIVERS
// -------------------------------------------------------------
data class WidgetSnapshot(
    val daily: DailyFinancialSummary,
    val weekly: WeeklyFinancialSummary,
    val monthly: MonthlyFinancialSummary,
    val recentTransactions: List<Transaction>
)

private suspend fun loadWidgetFinancialData(context: Context): WidgetSnapshot {
    val entryPoint = EntryPointAccessors.fromApplication(
        context.applicationContext,
        SpendwellWidgetEntryPoint::class.java
    )
    val txRepo = entryPoint.transactionRepository()
    val userPrefs = entryPoint.userPreferencesRepository()

    val allTx = txRepo.observeAllTransactions().firstOrNull() ?: emptyList()
    val cycleDay = userPrefs.cycleStartDay.firstOrNull() ?: 1

    val cal = Calendar.getInstance()
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH)

    val daily = FinancialAnalyticsEngine.analyzeDaily(allTx)
    val weekly = FinancialAnalyticsEngine.analyzeWeekly(allTx)
    val monthly = FinancialAnalyticsEngine.analyze(allTx, year, month, cycleDay)

    return WidgetSnapshot(
        daily = daily,
        weekly = weekly,
        monthly = monthly,
        recentTransactions = allTx.take(5)
    )
}

class SmallSpendwellWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SmallSpendwellGlanceWidget()
}

class MediumSpendwellWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MediumSpendwellGlanceWidget()
}

class LargeSpendwellWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LargeSpendwellGlanceWidget()
}
