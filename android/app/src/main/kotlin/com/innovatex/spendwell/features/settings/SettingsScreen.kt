package com.innovatex.spendwell.features.settings

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.innovatex.spendwell.analytics.FinancialAnalyticsEngine
import com.innovatex.spendwell.designsystem.components.SpendwellCard
import com.innovatex.spendwell.designsystem.components.SpendwellPillBadge
import com.innovatex.spendwell.designsystem.components.SpendwellTonalButton
import com.innovatex.spendwell.designsystem.theme.CardSurfaceWhite
import com.innovatex.spendwell.designsystem.theme.CobaltPrimary
import com.innovatex.spendwell.designsystem.theme.EmeraldSecondary
import com.innovatex.spendwell.designsystem.theme.OutflowRed
import com.innovatex.spendwell.designsystem.theme.PillShape
import com.innovatex.spendwell.designsystem.theme.PrimaryFixed
import com.innovatex.spendwell.designsystem.theme.SecondaryFixed
import com.innovatex.spendwell.designsystem.theme.SquircleCardShape
import com.innovatex.spendwell.designsystem.theme.SurfaceContainerLow
import java.util.Calendar

@Composable
fun SettingsScreen(
    onNavigateToModelManagement: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateBack: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val currentCycleDay by viewModel.cycleStartDay.collectAsState()
    val isPrivateMode by viewModel.isPrivateMode.collectAsState()
    val isModelDownloaded by viewModel.isModelDownloaded.collectAsState()
    val userName by viewModel.userName.collectAsState()

    var isBiometricEnabled by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var editingName by remember { mutableStateOf("") }

    val presetOptions = listOf(
        Pair("1st of Month", 1),
        Pair("5th", 5),
        Pair("10th", 10),
        Pair("25th", 25),
        Pair("Last Day", -1),
        Pair("Last Working Day", -2)
    )

    val cal = Calendar.getInstance()
    val currentCycle = remember(currentCycleDay) {
        FinancialAnalyticsEngine.calculateCycle(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            currentCycleDay
        )
    }

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = {
                Text("Update Name", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Enter your name to personalize your dashboard greetings and local AI advisor.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editingName,
                        onValueChange = { editingName = it },
                        singleLine = true,
                        placeholder = { Text("Your name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setUserName(editingName.trim())
                        showEditNameDialog = false
                    }
                ) {
                    Text("Save", fontWeight = FontWeight.Bold, color = CobaltPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back to Home",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Settings & Data",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Preferences, salary cycle & encrypted enclave",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Profile & Personalization Card
        SpendwellCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(CobaltPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            if (userName.isNotBlank()) {
                                Text(
                                    text = userName.trim().take(1).uppercase(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = if (userName.isNotBlank()) userName else "Set your name",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Personalizing greetings & AI advisor",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                editingName = userName
                                showEditNameDialog = true
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Name",
                            tint = CobaltPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Edit",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = CobaltPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Financial Cycle / Salary Day Setting
        SpendwellCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PrimaryFixed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = CobaltPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Salary / Expense Cycle",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val dayLabel = when (currentCycleDay) {
                                -2 -> "Last working day of month"
                                -1 -> "Last day of month"
                                1 -> "1st of month (Calendar)"
                                else -> "${currentCycleDay}th of every month"
                            }
                            Text(
                                text = dayLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = CobaltPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    SpendwellPillBadge(text = "ACTIVE")
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Choose the day your salary is credited or when your monthly expense cycle should reset. Spendwell will calculate runway and summaries based on your personal cycle.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Presets grid (2 rows of 3 buttons)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetOptions.chunked(3).forEach { rowPresets ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowPresets.forEach { (label, day) ->
                                val isSelected = currentCycleDay == day
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) CobaltPrimary else SurfaceContainerLow)
                                        .clickable { viewModel.setCycleStartDay(day) }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom slider (1 to 31)
                Text(
                    text = "Or choose custom day of month (1 to 31):",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val sliderValue = if (currentCycleDay > 0) currentCycleDay.toFloat() else 31f
                Slider(
                    value = sliderValue,
                    onValueChange = { viewModel.setCycleStartDay(it.toInt()) },
                    valueRange = 1f..31f,
                    steps = 29,
                    colors = SliderDefaults.colors(
                        thumbColor = CobaltPrimary,
                        activeTrackColor = CobaltPrimary
                    )
                )

                // Live Cycle Range Display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceContainerLow)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Current Cycle Window: ${currentCycle.label} • ${currentCycle.daysRemaining} days left",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = CobaltPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Encryption Enclave Status
        SpendwellCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "SQLCipher Database", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = "AES-256 GCM KeyStore Protected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    SpendwellPillBadge(text = "ENCRYPTED")
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Your local Room database is encrypted with SQLCipher using a 256-bit key protected inside Android Keystore hardware.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Privacy & Biometrics
        SpendwellCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Strict Private Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(text = "100% on-device. SMS & transactions never leave this phone.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = isPrivateMode,
                        onCheckedChange = { viewModel.setPrivateMode(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Biometric App Lock", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(text = "Require fingerprint or face recognition to unlock.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = isBiometricEnabled,
                        onCheckedChange = { isBiometricEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. AI & Permission navigation
        SpendwellCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "On-Device AI Engine", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    SpendwellPillBadge(
                        text = if (isModelDownloaded) "GEMMA 2B ACTIVE" else "NOT INSTALLED"
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Manage your local Gemma 2B LiteRT LLM weights for private on-device financial insights and query reasoning.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                SpendwellTonalButton(
                    text = if (isModelDownloaded) "Configure Gemma 2B Engine" else "Download Gemma 2B Model",
                    leadingIcon = Icons.Default.SmartToy,
                    onClick = onNavigateToModelManagement
                )
                Spacer(modifier = Modifier.height(10.dp))
                SpendwellTonalButton(
                    text = "Manage SMS & Notification Permissions",
                    leadingIcon = Icons.Default.Info,
                    onClick = onNavigateToPermissions
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Data Management Card
        SpendwellCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(text = "Data Management", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Export your financial history or clear cached signals securely.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                SpendwellTonalButton(
                    text = "Export Ledger (CSV / JSON)",
                    leadingIcon = Icons.Default.FileDownload,
                    onClick = { /* Export action */ }
                )
                Spacer(modifier = Modifier.height(12.dp))
                SpendwellTonalButton(
                    text = "Erase Local Ledger",
                    leadingIcon = Icons.Default.Delete,
                    onClick = { showClearDataDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Erase All Data?", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently delete all encrypted transactions, accounts, and history from this device.") },
            confirmButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Delete Everything", color = OutflowRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
