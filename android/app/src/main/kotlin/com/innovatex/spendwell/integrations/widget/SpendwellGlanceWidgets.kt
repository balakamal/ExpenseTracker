package com.innovatex.spendwell.integrations.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
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

class SmallSpendwellGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                SmallWidgetContent()
            }
        }
    }

    @Composable
    private fun SmallWidgetContent() {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF101820))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SPENDWELL",
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(Color(0xFFA0B5AF)),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = "₹ 1,45,280",
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(Color.White),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = "Net Balance",
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(Color(0xFF36C99A)),
                    fontSize = 12.sp
                )
            )
        }
    }
}

class MediumSpendwellGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                MediumWidgetContent()
            }
        }
    }

    @Composable
    private fun MediumWidgetContent() {
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF101820))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "TOTAL BALANCE",
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(Color(0xFFA0B5AF)),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = "₹ 1,45,280",
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(Color.White),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "MONTHLY SPENT",
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(Color(0xFFA0B5AF)),
                        fontSize = 11.sp
                    )
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = "- ₹ 24,150",
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(Color(0xFFD9383A)),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

class LargeSpendwellGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                LargeWidgetContent()
            }
        }
    }

    @Composable
    private fun LargeWidgetContent() {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF101820))
                .padding(20.dp)
        ) {
            Text(
                text = "SPENDWELL LEDGER",
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(Color(0xFFA0B5AF)),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = "₹ 1,45,280",
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(Color.White),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.height(16.dp))
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Text(
                    text = "Inflow: + ₹ 80,000",
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(Color(0xFF159B70)),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = "Outflow: - ₹ 24,150",
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(Color(0xFFD9383A)),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
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
