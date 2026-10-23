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
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.innovatex.expensetracker.MainActivity

class MediumExpenseGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            MediumWidgetContent(context)
        }
    }

    @Composable
    private fun MediumWidgetContent(context: Context) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(24.dp)
                .background(Color(0xFF12151F))
                .clickable(actionStartActivity<MainActivity>())
                .padding(18.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Total spent & live status
                Column(
                    modifier = GlanceModifier.defaultWeight()
                ) {
                    Text(
                        text = "MONTHLY SPENT",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF94A3B8)),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = "₹1,248.50",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFF8FAFC)),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = "₹1,251.50 remaining",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF10B981)),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.width(16.dp))

                // Right Column: Top Category Breakdown
                Column(
                    modifier = GlanceModifier.defaultWeight()
                ) {
                    Text(
                        text = "TOP CATEGORIES",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF64748B)),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(6.dp))
                    Text(
                        text = "• Groceries: ₹480.00",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFF8FAFC)),
                            fontSize = 12.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(3.dp))
                    Text(
                        text = "• Dining: ₹215.20",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFF8FAFC)),
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}

class MediumExpenseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MediumExpenseGlanceWidget()
}
