package com.innovatex.expensetracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.innovatex.expensetracker.MainActivity

class LargeExpenseGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            LargeWidgetContent(context)
        }
    }

    @Composable
    private fun LargeWidgetContent(context: Context) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(28.dp)
                .background(Color(0xFF12151F))
                .clickable(actionStartActivity<MainActivity>())
                .padding(20.dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize()
            ) {
                // Header Row
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EXPENSES",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF10B981)),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = "LIVE",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF94A3B8)),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(10.dp))

                // Monthly Total
                Text(
                    text = "₹1,248.50",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFF8FAFC)),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Text(
                    text = "₹1,251.50 remaining of ₹25,000 budget",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF94A3B8)),
                        fontSize = 12.sp
                    )
                )

                Spacer(modifier = GlanceModifier.height(14.dp))

                // Section Title
                Text(
                    text = "RECENT ACTIVITY",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF64748B)),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.height(6.dp))

                // Recent Items
                WidgetItemRow("Swiggy", "-₹258.20", "Food & Dining")
                Spacer(modifier = GlanceModifier.height(4.dp))
                WidgetItemRow("Blinkit", "-₹418.75", "Groceries")
                Spacer(modifier = GlanceModifier.height(4.dp))
                WidgetItemRow("Salary Deposit", "+₹45,000.00", "Income")
            }
        }
    }

    @Composable
    private fun WidgetItemRow(merchant: String, amount: String, category: String) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = merchant,
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFF8FAFC)),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = category,
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF94A3B8)),
                        fontSize = 10.sp
                    )
                )
            }
            Text(
                text = amount,
                style = TextStyle(
                    color = ColorProvider(if (amount.startsWith("+")) Color(0xFF10B981) else Color(0xFFF8FAFC)),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

class LargeExpenseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LargeExpenseGlanceWidget()
}
