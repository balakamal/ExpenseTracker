package com.innovatex.spendwell.features.permissions

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.innovatex.spendwell.designsystem.components.SpendwellCard
import com.innovatex.spendwell.designsystem.components.SpendwellPillBadge
import com.innovatex.spendwell.designsystem.components.SpendwellTonalButton
import com.innovatex.spendwell.integrations.sms.SmsHistoryImportResult
import com.innovatex.spendwell.integrations.sms.SmsHistoryImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PermissionManagementViewModel @Inject constructor(
    private val importer: SmsHistoryImporter
) : ViewModel() {
    private val _importResult = MutableStateFlow<SmsHistoryImportResult?>(null)
    val importResult: StateFlow<SmsHistoryImportResult?> = _importResult

    private val _importError = MutableStateFlow<String?>(null)
    val importError: StateFlow<String?> = _importError

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting

    fun importHistory() {
        if (_isImporting.value) return
        viewModelScope.launch {
            _isImporting.value = true
            _importError.value = null
            _importResult.value = null
            try {
                _importResult.value = importer.importRecentHistory()
            } catch (error: Exception) {
                _importError.value = error.message ?: "SMS history import failed."
            } finally {
                _isImporting.value = false
            }
        }
    }
}

@Composable
fun PermissionManagementScreen(
    modifier: Modifier = Modifier,
    viewModel: PermissionManagementViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var readSmsGranted by remember { mutableStateOf(false) }
    var receiveSmsGranted by remember { mutableStateOf(false) }
    var notificationAccessGranted by remember { mutableStateOf(false) }
    var batteryExemptionGranted by remember { mutableStateOf(false) }
    val importResult by viewModel.importResult.collectAsState()
    val importError by viewModel.importError.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()

    fun refreshPermissions() {
        readSmsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        receiveSmsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        val enabledListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners").orEmpty()
        notificationAccessGranted = enabledListeners.split(':').any {
            ComponentName.unflattenFromString(it)?.packageName == context.packageName
        }
        val powerManager = context.getSystemService(PowerManager::class.java)
        batteryExemptionGranted = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        refreshPermissions()
        if (grants[Manifest.permission.READ_SMS] == true) {
            viewModel.importHistory()
        }
    }

    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshPermissions()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) { refreshPermissions() }

    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp).verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(20.dp))
        Text("Permission Management", style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(6.dp))
        Text("Choose which on-device sources Spendwell can access. Permissions are optional and can be revoked in Android settings.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))

        PermissionItemCard(
            title = "SMS history & live alerts",
            description = "Read existing bank SMS alerts to build your ledger, then receive new alerts as they arrive. Message processing stays on this device.",
            icon = Icons.Default.Sms,
            status = when {
                readSmsGranted && receiveSmsGranted -> "Granted"
                readSmsGranted -> "History access granted"
                else -> "Not granted"
            },
            granted = readSmsGranted && receiveSmsGranted,
            actionLabel = if (readSmsGranted) "Import SMS history again / enable live alerts" else "Grant SMS access"
        ) {
            val missingPermissions = buildList {
                if (!readSmsGranted) add(Manifest.permission.READ_SMS)
                if (!receiveSmsGranted) add(Manifest.permission.RECEIVE_SMS)
            }
            if (missingPermissions.isNotEmpty()) {
                permissionLauncher.launch(missingPermissions.toTypedArray())
            } else {
                viewModel.importHistory()
            }
        }

        if (isImporting) {
            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator()
                Text("Scanning SMS history and processing transaction alerts…", style = MaterialTheme.typography.bodyMedium)
            }
        }
        importResult?.let { result ->
            SpendwellCard(Modifier.fillMaxWidth()) {
                Column {
                    Text("SMS import complete", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${result.scanned} messages scanned • ${result.imported} new transactions • ${result.needsReview} need review • ${result.alreadyPresentOrIgnored} ignored or already present",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        importError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 12.dp))
        }

        Spacer(Modifier.height(16.dp))
        PermissionItemCard(
            title = "Notification listener",
            description = "Optionally capture payment confirmations from supported banking and UPI apps.",
            icon = Icons.Default.Notifications,
            status = if (notificationAccessGranted) "Granted" else "Not granted",
            granted = notificationAccessGranted,
            actionLabel = "Open notification access settings"
        ) { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }

        Spacer(Modifier.height(16.dp))
        PermissionItemCard(
            title = "Battery optimization",
            description = "Battery restrictions may delay background processing on some devices. This is optional; Android and device settings remain in control.",
            icon = Icons.Default.BatteryAlert,
            status = if (batteryExemptionGranted) "Exempted" else "System-managed",
            granted = batteryExemptionGranted,
            actionLabel = "Open battery settings"
        ) { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun PermissionItemCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    status: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    SpendwellCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                SpendwellPillBadge(
                    text = status,
                    backgroundColor = if (granted) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    textColor = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))
            SpendwellTonalButton(text = actionLabel, onClick = onAction)
        }
    }
}
