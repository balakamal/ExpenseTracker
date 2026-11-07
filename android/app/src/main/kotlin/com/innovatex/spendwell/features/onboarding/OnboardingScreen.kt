package com.innovatex.spendwell.features.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovatex.spendwell.designsystem.components.SpendwellCard
import com.innovatex.spendwell.designsystem.components.SpendwellPillBadge
import com.innovatex.spendwell.designsystem.components.SpendwellPrimaryButton
import com.innovatex.spendwell.designsystem.theme.CobaltPrimary
import com.innovatex.spendwell.designsystem.theme.EmeraldSecondary
import com.innovatex.spendwell.designsystem.theme.PillShape
import com.innovatex.spendwell.designsystem.theme.SquircleCardShape
import com.innovatex.spendwell.designsystem.theme.SurfaceContainerLow

@Composable
fun OnboardingScreen(
    onGetStarted: (userName: String, cycleStartDay: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableIntStateOf(0) }
    var userName by remember { mutableStateOf("") }
    var selectedCycleDay by remember { mutableIntStateOf(1) } // 1..31 or -1 for last day

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            SpendwellPillBadge(
                text = if (step == 0) "PRIVATE BY DEFAULT" else "PERSONALIZED FINANCIAL CYCLE",
                icon = if (step == 0) Icons.Default.Lock else Icons.Default.CalendarToday
            )

            Spacer(modifier = Modifier.height(20.dp))

            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "onboarding_step"
            ) { targetStep ->
                if (targetStep == 0) {
                    WelcomeIntroStep(
                        userName = userName,
                        onUserNameChange = { userName = it }
                    )
                } else {
                    SalaryCycleSelectionStep(
                        selectedCycleDay = selectedCycleDay,
                        onCycleDaySelected = { selectedCycleDay = it }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // CTA Button
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SpendwellPrimaryButton(
                text = if (step == 0) "Next: Set Salary Cycle" else "Continue to Privacy Setup",
                onClick = {
                    if (step == 0) {
                        step = 1
                    } else {
                        onGetStarted(userName.trim(), selectedCycleDay)
                    }
                }
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = if (step == 0) "Step 1 of 2 • Instant local setup" else "Step 2 of 2 • You can modify this anytime in Settings",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun WelcomeIntroStep(
    userName: String,
    onUserNameChange: (String) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Spendwell",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Your private, intelligent ledger.\nEffortless tracking without the cloud.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // User name personalization card
        SpendwellCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PrimaryFixed),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = CobaltPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "What should we call you?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Personalizes greetings and on-device AI advisor",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = userName,
                    onValueChange = onUserNameChange,
                    placeholder = { Text("Enter your name (e.g. Alex)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CobaltPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        SpendwellCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = MaterialTheme.colorScheme.surface
        ) {
            Column {
                OnboardingFeatureRow(
                    icon = Icons.Default.Speed,
                    title = "Zero Manual Entry",
                    description = "Automatically extracts financial transactions from verified bank alerts and notifications."
                )
                Spacer(modifier = Modifier.height(20.dp))
                OnboardingFeatureRow(
                    icon = Icons.Default.Security,
                    title = "100% Offline & Encrypted",
                    description = "Hardware Keystore encryption. Your financial numbers never leave your phone."
                )
                Spacer(modifier = Modifier.height(20.dp))
                OnboardingFeatureRow(
                    icon = Icons.Default.Savings,
                    title = "On-Device Gemma 2B AI",
                    description = "LiteRT local intelligence filters spam and explains trends with zero cloud transmission."
                )
            }
        }
    }
}

@Composable
private fun SalaryCycleSelectionStep(
    selectedCycleDay: Int,
    onCycleDaySelected: (Int) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "When does your month start?",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Choose your salary or billing day so your monthly totals, safe-to-spend allowance, and savings rate reflect your real cashflow.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Preset Options
        val presets = listOf(
            Pair("1st (Calendar Month)", 1),
            Pair("5th of the month", 5),
            Pair("10th of the month", 10),
            Pair("25th of the month", 25),
            Pair("Last Day of month", -1),
            Pair("Last Working Day (Salary)", -2)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            presets.forEach { (label, day) ->
                val isSelected = selectedCycleDay == day
                val borderColor = if (isSelected) CobaltPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                val bgColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(SquircleCardShape)
                        .background(bgColor)
                        .border(if (isSelected) 2.dp else 1.dp, borderColor, SquircleCardShape)
                        .clickable { onCycleDaySelected(day) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) CobaltPrimary else SurfaceContainerLow),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(CobaltPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Custom Slider if user wants a day outside common presets
        val isCustom = presets.none { it.second == selectedCycleDay }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceContainerLow)
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Custom Day Picker",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when (selectedCycleDay) {
                        -2 -> "Last Working Day"
                        -1 -> "Last Day"
                        else -> "Day $selectedCycleDay"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = CobaltPrimary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Slider(
                value = if (selectedCycleDay < 1) 31f else selectedCycleDay.toFloat(),
                onValueChange = { onCycleDaySelected(it.toInt().coerceIn(1, 31)) },
                valueRange = 1f..31f,
                steps = 29,
                colors = SliderDefaults.colors(
                    thumbColor = CobaltPrimary,
                    activeTrackColor = CobaltPrimary
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live preview of cycle
        val cyclePreview = when (selectedCycleDay) {
            1 -> "1st of the month to last day of the month"
            -1 -> "Last day of previous month to day before last day"
            -2 -> "Last working day of previous month to day before last working day"
            else -> "Day $selectedCycleDay of current month to Day ${selectedCycleDay - 1} of next month"
        }

        Row(
            modifier = Modifier
                .clip(PillShape)
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                tint = EmeraldSecondary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Cycle window: $cyclePreview",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun OnboardingFeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
