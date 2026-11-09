package com.innovatex.spendwell.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovatex.spendwell.analytics.CategorySpending
import com.innovatex.spendwell.analytics.CumulativeSpendPoint
import com.innovatex.spendwell.analytics.DaySpendBar
import com.innovatex.spendwell.common.Money
import com.innovatex.spendwell.designsystem.theme.CardSurfaceWhite
import com.innovatex.spendwell.designsystem.theme.CobaltPrimary
import com.innovatex.spendwell.designsystem.theme.CrimsonTertiary
import com.innovatex.spendwell.designsystem.theme.EmeraldSecondary
import com.innovatex.spendwell.designsystem.theme.PillShape
import com.innovatex.spendwell.designsystem.theme.SquircleCardShape
import com.innovatex.spendwell.designsystem.theme.SurfaceContainer
import com.innovatex.spendwell.designsystem.theme.SurfaceContainerLow
import java.util.Locale

/**
 * High-end Cubic Bezier Curve for cumulative spend velocity & burn-rate tracking.
 * Features:
 * - Fluid Cubic Bezier spline interpolation between daily cumulative points.
 * - Vertical gradient under-fill fading to transparent.
 * - Subtle dashed linear benchmark pace line.
 * - Interactive touch scrubber with floating tooltip pill showing exact date and cumulative spend.
 */
@Composable
fun SpendwellBezierCumulativeBurnChart(
    points: List<CumulativeSpendPoint>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 190.dp,
    strokeColor: Color = CobaltPrimary,
    benchmarkTarget: Money? = null
) {
    if (points.isEmpty()) return

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(points) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
        )
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val density = LocalDensity.current

    val maxCumulative = remember(points) {
        val maxVal = points.maxOfOrNull { it.cumulativeSpend.toDisplayDouble() } ?: 1.0
        val targetVal = benchmarkTarget?.toDisplayDouble() ?: 0.0
        maxOf(maxVal, targetVal, 100.0) * 1.12 // 12% headroom
    }

    val activePoint = selectedIndex?.let { points.getOrNull(it) } ?: points.lastOrNull { it.isPastOrToday } ?: points.last()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = SquircleCardShape,
        colors = CardDefaults.cardColors(containerColor = CardSurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), SquircleCardShape)
                .padding(18.dp)
        ) {
            // Header with interactive scrubber readout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CUMULATIVE BURN VELOCITY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = activePoint.cumulativeSpend.formatInr(true),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = strokeColor
                    )
                }

                // Interactive Date Scrubber Indicator Pill
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(if (selectedIndex != null) CobaltPrimary.copy(alpha = 0.12f) else SurfaceContainerLow)
                        .border(1.dp, if (selectedIndex != null) CobaltPrimary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), PillShape)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (selectedIndex != null) "Scrubbing: ${activePoint.dateLabel}" else "Day ${activePoint.dayIndex} • ${activePoint.dateLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedIndex != null) CobaltPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Canvas for Bezier Curve
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(points) {
                            detectTapGestures(
                                onPress = { offset ->
                                    val paddingPx = with(density) { 16.dp.toPx() }
                                    val usableWidth = size.width - (2 * paddingPx)
                                    val ratio = ((offset.x - paddingPx) / usableWidth).coerceIn(0f, 1f)
                                    val idx = (ratio * (points.size - 1)).toInt().coerceIn(0, points.size - 1)
                                    selectedIndex = idx
                                }
                            )
                        }
                        .pointerInput(points) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val paddingPx = with(density) { 16.dp.toPx() }
                                    val usableWidth = size.width - (2 * paddingPx)
                                    val ratio = ((offset.x - paddingPx) / usableWidth).coerceIn(0f, 1f)
                                    val idx = (ratio * (points.size - 1)).toInt().coerceIn(0, points.size - 1)
                                    selectedIndex = idx
                                },
                                onDragEnd = {
                                    selectedIndex = null
                                },
                                onDragCancel = {
                                    selectedIndex = null
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val paddingPx = with(density) { 16.dp.toPx() }
                                    val usableWidth = size.width - (2 * paddingPx)
                                    val ratio = ((change.position.x - paddingPx) / usableWidth).coerceIn(0f, 1f)
                                    val idx = (ratio * (points.size - 1)).toInt().coerceIn(0, points.size - 1)
                                    selectedIndex = idx
                                }
                            )
                        }
                ) {
                    val paddingLeft = 14.dp.toPx()
                    val paddingRight = 14.dp.toPx()
                    val paddingTop = 12.dp.toPx()
                    val paddingBottom = 24.dp.toPx()

                    val chartW = size.width - paddingLeft - paddingRight
                    val chartH = size.height - paddingTop - paddingBottom

                    // Draw subtle horizontal gridlines (25%, 50%, 75%, 100%)
                    val gridEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    for (step in 1..3) {
                        val gridY = paddingTop + chartH * (step / 4f)
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.25f),
                            start = Offset(paddingLeft, gridY),
                            end = Offset(size.width - paddingRight, gridY),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = gridEffect
                        )
                    }

                    // Compute (X, Y) pixel coordinates for each data point
                    val coords = points.mapIndexed { idx, pt ->
                        val x = paddingLeft + (idx.toFloat() / (points.size - 1).coerceAtLeast(1)) * chartW
                        val normalizedY = (pt.cumulativeSpend.toDisplayDouble() / maxCumulative).toFloat()
                        val animatedY = normalizedY * animationProgress.value
                        val y = paddingTop + chartH * (1f - animatedY)
                        Offset(x, y)
                    }

                    if (coords.size >= 2) {
                        // 1. Build Cubic Bezier Smooth Path
                        val strokePath = Path().apply {
                            moveTo(coords[0].x, coords[0].y)
                            for (i in 0 until coords.size - 1) {
                                val p0 = coords[i]
                                val p1 = coords[i + 1]
                                // Smooth horizontal control points
                                val c1 = Offset(p0.x + (p1.x - p0.x) * 0.45f, p0.y)
                                val c2 = Offset(p0.x + (p1.x - p0.x) * 0.55f, p1.y)
                                cubicTo(c1.x, c1.y, c2.x, c2.y, p1.x, p1.y)
                            }
                        }

                        // 2. Build Gradient Under-Fill Path
                        val fillPath = Path().apply {
                            addPath(strokePath)
                            lineTo(coords.last().x, paddingTop + chartH)
                            lineTo(coords.first().x, paddingTop + chartH)
                            close()
                        }

                        // Draw area gradient
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    strokeColor.copy(alpha = 0.28f),
                                    strokeColor.copy(alpha = 0.08f),
                                    strokeColor.copy(alpha = 0.00f)
                                ),
                                startY = paddingTop,
                                endY = paddingTop + chartH
                            )
                        )

                        // Draw smooth Bezier curve line
                        drawPath(
                            path = strokePath,
                            color = strokeColor,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        // Draw dashed linear benchmark pace line if provided
                        if (benchmarkTarget != null) {
                            val targetY = paddingTop + chartH * (1f - (benchmarkTarget.toDisplayDouble() / maxCumulative).toFloat().coerceIn(0f, 1f))
                            val benchmarkPath = Path().apply {
                                moveTo(coords.first().x, paddingTop + chartH)
                                lineTo(coords.last().x, targetY)
                            }
                            drawPath(
                                path = benchmarkPath,
                                color = Color.Gray.copy(alpha = 0.4f),
                                style = Stroke(
                                    width = 1.5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                )
                            )
                        }

                        // Draw interactive scrubber cursor & glowing point
                        val targetIdx = selectedIndex ?: points.indexOfLast { it.isPastOrToday }.takeIf { it >= 0 } ?: (points.size - 1)
                        if (targetIdx in coords.indices) {
                            val activeCoord = coords[targetIdx]

                            // Vertical dashed cursor line
                            drawLine(
                                color = strokeColor.copy(alpha = 0.6f),
                                start = Offset(activeCoord.x, paddingTop),
                                end = Offset(activeCoord.x, paddingTop + chartH),
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                            )

                            // Outer translucent glow ring
                            drawCircle(
                                color = strokeColor.copy(alpha = 0.25f),
                                radius = 9.dp.toPx(),
                                center = activeCoord
                            )

                            // Inner solid core
                            drawCircle(
                                color = Color.White,
                                radius = 5.dp.toPx(),
                                center = activeCoord
                            )
                            drawCircle(
                                color = strokeColor,
                                radius = 3.5.dp.toPx(),
                                center = activeCoord
                            )
                        }
                    }
                }
            }

            // Bottom X-Axis Milestone Labels (Day 1, Mid, Final Day)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = points.firstOrNull()?.dateLabel ?: "Day 1",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (points.size > 14) {
                    val midPoint = points[points.size / 2]
                    Text(
                        text = midPoint.dateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = points.lastOrNull()?.dateLabel ?: "End",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Modern Donut Chart for multi-category spending distribution.
 * Features:
 * - Circular arc ring with clean rounded separation caps.
 * - Interactive slice highlight with dynamic center callout.
 * - Center display showing total expenditure and category count.
 */
@Composable
fun SpendwellDonutChart(
    categories: List<CategorySpending>,
    totalExpenses: Money,
    modifier: Modifier = Modifier,
    chartSize: Dp = 190.dp
) {
    if (categories.isEmpty() || totalExpenses.isZero) return

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(categories) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(700, easing = FastOutSlowInEasing))
    }

    var selectedCategory by remember { mutableStateOf<CategorySpending?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = SquircleCardShape,
        colors = CardDefaults.cardColors(containerColor = CardSurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), SquircleCardShape)
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CATEGORY WEIGHT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${categories.size} Categories",
                    style = MaterialTheme.typography.labelSmall,
                    color = CobaltPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Donut Canvas with Centered Content
            Box(
                modifier = Modifier.size(chartSize),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeW = 20.dp.toPx()
                    val diameter = size.minDimension - strokeW
                    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                    val arcSize = Size(diameter, diameter)

                    var currentAngle = -90f
                    val gapAngle = if (categories.size > 1) 3f else 0f

                    categories.forEach { cat ->
                        val sliceSweep = ((cat.percentageOfTotal / 100f) * 360f - gapAngle) * animProgress.value
                        if (sliceSweep > 0f) {
                            val catColor = try {
                                Color(android.graphics.Color.parseColor(cat.colorHex))
                            } catch (_: Exception) {
                                CobaltPrimary
                            }

                            drawArc(
                                color = catColor,
                                startAngle = currentAngle,
                                sweepAngle = sliceSweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeW, cap = StrokeCap.Round)
                            )
                        }
                        currentAngle += ((cat.percentageOfTotal / 100f) * 360f) * animProgress.value
                    }
                }

                // Inner Callout Container
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    val displayCat = selectedCategory
                    if (displayCat != null) {
                        Text(
                            text = displayCat.categoryName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = displayCat.amount.formatInr(true),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${String.format(Locale.ENGLISH, "%.1f", displayCat.percentageOfTotal)}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = CobaltPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "TOTAL SPENT",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = totalExpenses.formatInr(true),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * Interactive Weekly Bar Chart with gradient fills and rounded tops.
 * Highlights the peak spending day, weekends, and the current active day.
 */
@Composable
fun SpendwellWeeklyInteractiveBarChart(
    bars: List<DaySpendBar>,
    peakDayLabel: String,
    weekdayTotal: Money,
    weekendTotal: Money,
    weekendVariance: Float,
    modifier: Modifier = Modifier
) {
    var selectedBar by remember { mutableStateOf<DaySpendBar?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = SquircleCardShape,
        colors = CardDefaults.cardColors(containerColor = CardSurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), SquircleCardShape)
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "7-DAY ACTIVITY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = selectedBar?.let { "${it.dayLabel} (${it.fullDateLabel}): ${it.amount.formatInr(true)}" } ?: "Peak: $peakDayLabel",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = CobaltPrimary
                    )
                }

                if (selectedBar != null) {
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(SurfaceContainerLow)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Tap to clear",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bars Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                bars.forEach { bar ->
                    val isSelected = selectedBar == bar
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .pointerInput(bar) {
                                detectTapGestures(
                                    onTap = {
                                        selectedBar = if (selectedBar == bar) null else bar
                                    }
                                )
                            }
                    ) {
                        // Bar Column Canvas
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height((95 * bar.percentageOfMax).dp)
                                .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp))
                                .background(
                                    when {
                                        isSelected -> Brush.verticalGradient(listOf(CobaltPrimary, CobaltPrimary.copy(alpha = 0.7f)))
                                        bar.isToday -> Brush.verticalGradient(listOf(CobaltPrimary, CobaltPrimary.copy(alpha = 0.85f)))
                                        bar.isWeekend -> Brush.verticalGradient(listOf(CrimsonTertiary.copy(alpha = 0.9f), CrimsonTertiary.copy(alpha = 0.5f)))
                                        else -> Brush.verticalGradient(listOf(SurfaceContainer.copy(alpha = 0.95f), SurfaceContainerLow))
                                    }
                                )
                                .border(
                                    width = if (isSelected || bar.isToday) 1.5.dp else 0.dp,
                                    color = if (isSelected) CobaltPrimary else if (bar.isToday) CobaltPrimary.copy(alpha = 0.5f) else Color.Transparent,
                                    shape = RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp)
                                )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = bar.dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (bar.isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (bar.isToday || isSelected) CobaltPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Weekday vs Weekend Comparison
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceContainerLow)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Weekdays: ${weekdayTotal.formatInr(true)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Weekends: ${weekendTotal.formatInr(true)} (${String.format(Locale.ENGLISH, "%+.0f", weekendVariance)}%)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (weekendVariance > 0) CrimsonTertiary else EmeraldSecondary
                )
            }
        }
    }
}
