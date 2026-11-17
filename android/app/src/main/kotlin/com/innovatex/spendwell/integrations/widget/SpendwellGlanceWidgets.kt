package com.innovatex.spendwell.integrations.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.currentState
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
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
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

val WIDGET_PERIOD_PREF_KEY = stringPreferencesKey("widget_period_filter")
val WIDGET_PERIOD_PARAM_KEY = ActionParameters.Key<String>("target_period")

class SetWidgetPeriodCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val targetPeriod = parameters[WIDGET_PERIOD_PARAM_KEY] ?: "DAY"
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[WIDGET_PERIOD_PREF_KEY] = targetPeriod
            }
        }
        LargeSpendwellGlanceWidget().update(context, glanceId)
        MediumSpendwellGlanceWidget().update(context, glanceId)
        SmallSpendwellGlanceWidget().update(context, glanceId)
    }
}

// -------------------------------------------------------------
// 1. SMALL WIDGET (2x2) - CLEAR CREDITS & DEBITS SUMMARY
// -------------------------------------------------------------
class SmallSpendwellGlanceWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

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
        val context = LocalContext.current
        val prefs = currentState<Preferences>()
        val selectedPeriod = prefs[WIDGET_PERIOD_PREF_KEY] ?: "DAY"
        val activeSummary = when (selectedPeriod) {
            "WEEK" -> data.week
            "MONTH" -> data.month
            else -> data.day
        }
        val periodLabel = when (selectedPeriod) {
            "WEEK" -> "THIS WEEK"
            "MONTH" -> "THIS MONTH"
            else -> "TODAY"
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        // Tapping the period badge cycles DAY -> WEEK -> MONTH -> DAY
        val nextPeriod = when (selectedPeriod) {
            "DAY" -> "WEEK"
            "WEEK" -> "MONTH"
            else -> "DAY"
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(18.dp)
                .background(Color(0xFF0C101A))
                .padding(12.dp)
                .clickable(actionStartActivity(launchIntent)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start
        ) {
            // Header Row: Period toggle chip + Txn count
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = GlanceModifier
                        .cornerRadius(6.dp)
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .clickable(actionRunCallback<SetWidgetPeriodCallback>(
                            actionParametersOf(WIDGET_PERIOD_PARAM_KEY to nextPeriod)
                        ))
                ) {
                    Text(
                        text = "$periodLabel ▾",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF38BDF8)),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = "${activeSummary.totalTransactions} txns",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF8899A6)),
                        fontSize = 10.sp
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Card 1: Debits (Spent)
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .cornerRadius(8.dp)
                    .background(Color(0xFF161B26))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = "DEBITS",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFFEF4444)),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "- " + activeSummary.debits.formatInr(true),
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            // Card 2: Credits (Received)
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .cornerRadius(8.dp)
                    .background(Color(0xFF161B26))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = "CREDITS",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF10B981)),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "+ " + activeSummary.credits.formatInr(true),
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF10B981)),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. MEDIUM WIDGET (4x2) - DUAL CARDS & INTERACTIVE PERIOD TABS
// -------------------------------------------------------------
class MediumSpendwellGlanceWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

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
        val context = LocalContext.current
        val prefs = currentState<Preferences>()
        val selectedPeriod = prefs[WIDGET_PERIOD_PREF_KEY] ?: "DAY"
        val activeSummary = when (selectedPeriod) {
            "WEEK" -> data.week
            "MONTH" -> data.month
            else -> data.day
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(18.dp)
                .background(Color(0xFF0C101A))
                .padding(14.dp)
                .clickable(actionStartActivity(launchIntent)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Header Bar: Title + Segmented Period Tabs (Day / Week / Month)
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SPENDWELL",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.defaultWeight())

                // Segmented Tabs
                Row(
                    modifier = GlanceModifier
                        .cornerRadius(8.dp)
                        .background(Color(0xFF161B26))
                        .padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PeriodTabChip(label = "Day", period = "DAY", currentPeriod = selectedPeriod)
                    PeriodTabChip(label = "Week", period = "WEEK", currentPeriod = selectedPeriod)
                    PeriodTabChip(label = "Month", period = "MONTH", currentPeriod = selectedPeriod)
                }
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            // Two Cards Side-by-Side: Debits & Credits
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Card 1: Debits (Spent)
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .cornerRadius(12.dp)
                        .background(Color(0xFF161B26))
                        .padding(10.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "DEBITS (SPENT)",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFFEF4444)),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.defaultWeight())
                            Text(
                                text = "${activeSummary.debitsCount}",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFF8899A6)),
                                    fontSize = 9.sp
                                )
                            )
                        }
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = "- " + activeSummary.debits.formatInr(true),
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.width(10.dp))

                // Card 2: Credits (Received)
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .cornerRadius(12.dp)
                        .background(Color(0xFF161B26))
                        .padding(10.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "CREDITS (RECEIVED)",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFF10B981)),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.defaultWeight())
                            Text(
                                text = "${activeSummary.creditsCount}",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFF8899A6)),
                                    fontSize = 9.sp
                                )
                            )
                        }
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = "+ " + activeSummary.credits.formatInr(true),
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF10B981)),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Footer Net Flow
            val netFlow = activeSummary.credits - activeSummary.debits
            val netPrefix = if (netFlow.isPositive) "Net Inflow: +" else if (netFlow.isNegative) "Net Outflow: " else "Net: "
            val netColor = if (netFlow.isPositive) Color(0xFF10B981) else if (netFlow.isNegative) Color(0xFFEF4444) else Color(0xFF8899A6)

            Text(
                text = "$netPrefix${netFlow.formatInr(true)}",
                style = TextStyle(
                    color = ColorProvider(netColor),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

// -------------------------------------------------------------
// 3. LARGE WIDGET (4x4) - ALL TRANSACTIONS IN DESCENDING ORDER
// -------------------------------------------------------------
class LargeSpendwellGlanceWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

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
        val context = LocalContext.current
        val prefs = currentState<Preferences>()
        val selectedPeriod = prefs[WIDGET_PERIOD_PREF_KEY] ?: "DAY"
        val activeSummary = when (selectedPeriod) {
            "WEEK" -> data.week
            "MONTH" -> data.month
            else -> data.day
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(20.dp)
                .background(Color(0xFF0C101A))
                .padding(14.dp)
        ) {
            // Header Bar: App Name + Period Tabs (Day / Week / Month)
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SPENDWELL",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.clickable(actionStartActivity(launchIntent))
                )

                Spacer(modifier = GlanceModifier.defaultWeight())

                // Segmented Tabs
                Row(
                    modifier = GlanceModifier
                        .cornerRadius(8.dp)
                        .background(Color(0xFF161B26))
                        .padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PeriodTabChip(label = "Day", period = "DAY", currentPeriod = selectedPeriod)
                    PeriodTabChip(label = "Week", period = "WEEK", currentPeriod = selectedPeriod)
                    PeriodTabChip(label = "Month", period = "MONTH", currentPeriod = selectedPeriod)
                }
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            // Two Cards Side-by-Side: Debits & Credits
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Card 1: Debits Card
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .cornerRadius(12.dp)
                        .background(Color(0xFF161B26))
                        .padding(10.dp)
                        .clickable(actionStartActivity(launchIntent))
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "DEBITS",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFFEF4444)),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.defaultWeight())
                            Text(
                                text = "${activeSummary.debitsCount} txns",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFF8899A6)),
                                    fontSize = 9.sp
                                )
                            )
                        }
                        Spacer(modifier = GlanceModifier.height(3.dp))
                        Text(
                            text = "- " + activeSummary.debits.formatInr(true),
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.width(10.dp))

                // Card 2: Credits Card
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .cornerRadius(12.dp)
                        .background(Color(0xFF161B26))
                        .padding(10.dp)
                        .clickable(actionStartActivity(launchIntent))
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "CREDITS",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFF10B981)),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.defaultWeight())
                            Text(
                                text = "${activeSummary.creditsCount} txns",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFF8899A6)),
                                    fontSize = 9.sp
                                )
                            )
                        }
                        Spacer(modifier = GlanceModifier.height(3.dp))
                        Text(
                            text = "+ " + activeSummary.credits.formatInr(true),
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF10B981)),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            // Sub-header: Activity Feed title
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ALL TRANSACTIONS (LATEST FIRST)",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF8899A6)),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = "${data.allTransactionsDescending.size} total",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF8899A6)),
                        fontSize = 9.sp
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            // Scrollable LazyColumn with transactions in descending order of created date
            if (data.allTransactionsDescending.isEmpty()) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(12.dp)
                        .background(Color(0xFF161B26))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No transactions logged yet",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF8899A6)),
                            fontSize = 12.sp
                        )
                    )
                }
            } else {
                LazyColumn(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(12.dp)
                        .background(Color(0xFF161B26))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    items(data.allTransactionsDescending) { tx ->
                        GlanceTransactionRow(tx = tx, launchIntent = launchIntent)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SHARED WIDGET COMPONENTS
// -------------------------------------------------------------
@Composable
private fun PeriodTabChip(label: String, period: String, currentPeriod: String) {
    val isSelected = period.equals(currentPeriod, ignoreCase = true)
    val bgColor = if (isSelected) Color(0xFF2563EB) else Color.Transparent
    val textColor = if (isSelected) Color.White else Color(0xFF8899A6)

    Box(
        modifier = GlanceModifier
            .cornerRadius(6.dp)
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .clickable(actionRunCallback<SetWidgetPeriodCallback>(
                actionParametersOf(WIDGET_PERIOD_PARAM_KEY to period)
            )),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = ColorProvider(textColor),
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        )
    }
}

@Composable
private fun GlanceTransactionRow(tx: Transaction, launchIntent: Intent) {
    val isCredit = tx.type == TransactionType.CREDIT
    val isToday = android.text.format.DateUtils.isToday(tx.timestamp)
    val isYesterday = android.text.format.DateUtils.isToday(tx.timestamp + 86400_000L)
    val timeFormat = SimpleDateFormat("h:mm a", Locale.ENGLISH)
    val dateFormat = SimpleDateFormat("dd MMM", Locale.ENGLISH)
    val dateLabel = when {
        isToday -> "Today, " + timeFormat.format(Date(tx.timestamp))
        isYesterday -> "Yest, " + timeFormat.format(Date(tx.timestamp))
        else -> dateFormat.format(Date(tx.timestamp)) + ", " + timeFormat.format(Date(tx.timestamp))
    }

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable(actionStartActivity(launchIntent)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Category initial badge
        val initial = (tx.categoryName.ifBlank { tx.merchantName }).firstOrNull()?.uppercaseChar()?.toString() ?: "•"
        Box(
            modifier = GlanceModifier
                .cornerRadius(12.dp)
                .background(if (isCredit) Color(0xFF064E3B) else Color(0xFF27272A))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                style = TextStyle(
                    color = ColorProvider(if (isCredit) Color(0xFF10B981) else Color(0xFFD4D4D8)),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = GlanceModifier.width(8.dp))

        // Merchant and Date
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = tx.merchantName.ifBlank { tx.description }.take(22),
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.height(1.dp))
            Text(
                text = "${tx.categoryName} • $dateLabel",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF8899A6)),
                    fontSize = 9.sp
                ),
                maxLines = 1
            )
        }

        Spacer(modifier = GlanceModifier.width(6.dp))

        // Amount with sign
        val sign = if (isCredit) "+ " else "- "
        Text(
            text = sign + tx.amount.formatInr(true),
            style = TextStyle(
                color = ColorProvider(if (isCredit) Color(0xFF10B981) else Color(0xFFEF4444)),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

// -------------------------------------------------------------
// DATA LOADER & SUMMARY MODELS
// -------------------------------------------------------------
data class PeriodSummary(
    val credits: Money,
    val debits: Money,
    val creditsCount: Int,
    val debitsCount: Int,
    val totalTransactions: Int
)

data class WidgetSnapshot(
    val day: PeriodSummary,
    val week: PeriodSummary,
    val month: PeriodSummary,
    val allTransactionsDescending: List<Transaction>,
    val daily: DailyFinancialSummary,
    val weekly: WeeklyFinancialSummary,
    val monthly: MonthlyFinancialSummary
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

    // Sort strictly in descending order of created date (newest first)
    val sortedTx = allTx.sortedByDescending { it.timestamp }

    val cal = Calendar.getInstance()
    val year = cal.get(Calendar.YEAR)
    val monthIndex = cal.get(Calendar.MONTH)

    // Start of Day
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val startOfDay = cal.timeInMillis

    // Start of Week
    cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
    val startOfWeek = cal.timeInMillis

    // Start of Month
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val startOfMonth = cal.timeInMillis

    fun summarize(txs: List<Transaction>): PeriodSummary {
        val credits = txs.filter { it.type == TransactionType.CREDIT }
        val debits = txs.filter { it.type == TransactionType.DEBIT }
        val totalCredits = credits.fold(Money.ZERO) { acc, tx -> acc + tx.amount }
        val totalDebits = debits.fold(Money.ZERO) { acc, tx -> acc + tx.amount }
        return PeriodSummary(
            credits = totalCredits,
            debits = totalDebits,
            creditsCount = credits.size,
            debitsCount = debits.size,
            totalTransactions = txs.size
        )
    }

    val daySummary = summarize(allTx.filter { it.timestamp >= startOfDay })
    val weekSummary = summarize(allTx.filter { it.timestamp >= startOfWeek })
    val monthSummary = summarize(allTx.filter { it.timestamp >= startOfMonth })

    val daily = FinancialAnalyticsEngine.analyzeDaily(allTx)
    val weekly = FinancialAnalyticsEngine.analyzeWeekly(allTx)
    val monthly = FinancialAnalyticsEngine.analyze(allTx, year, monthIndex, cycleDay)

    return WidgetSnapshot(
        day = daySummary,
        week = weekSummary,
        month = monthSummary,
        allTransactionsDescending = sortedTx,
        daily = daily,
        weekly = weekly,
        monthly = monthly
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
