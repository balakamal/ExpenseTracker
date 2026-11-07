package com.innovatex.spendwell.features.aimodel

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innovatex.spendwell.ai.SpendwellAiAdvisorEngine
import com.innovatex.spendwell.ai.model.ModelDownloadManager
import com.innovatex.spendwell.ai.model.ModelDownloadState
import com.innovatex.spendwell.data.preferences.UserPreferencesRepository
import com.innovatex.spendwell.designsystem.components.SpendwellCard
import com.innovatex.spendwell.designsystem.components.SpendwellPillBadge
import com.innovatex.spendwell.designsystem.components.SpendwellPrimaryButton
import com.innovatex.spendwell.designsystem.components.SpendwellTonalButton
import com.innovatex.spendwell.designsystem.theme.CobaltPrimary
import com.innovatex.spendwell.designsystem.theme.CrimsonTertiary
import com.innovatex.spendwell.designsystem.theme.EmeraldSecondary
import com.innovatex.spendwell.designsystem.theme.PillShape
import com.innovatex.spendwell.designsystem.theme.SquircleCardShape
import com.innovatex.spendwell.designsystem.theme.SurfaceContainerHigh
import com.innovatex.spendwell.designsystem.theme.SurfaceContainerLow
import com.innovatex.spendwell.designsystem.theme.WarningAmber
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AiModelManagementViewModel @Inject constructor(
    private val modelDownloadManager: ModelDownloadManager,
    private val preferencesRepository: UserPreferencesRepository,
    private val aiAdvisorEngine: SpendwellAiAdvisorEngine
) : ViewModel() {

    val downloadState: StateFlow<ModelDownloadState> = modelDownloadManager.downloadState
    val isWifiOnly: StateFlow<Boolean> = preferencesRepository.isWifiOnlyDownload
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val hfApiToken: StateFlow<String?> = preferencesRepository.hfApiToken
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val customModelUrl: StateFlow<String?> = preferencesRepository.customModelUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun startDownload() {
        viewModelScope.launch {
            modelDownloadManager.startDownload()
        }
    }

    fun cancelDownload() {
        modelDownloadManager.cancelDownload()
    }

    fun deleteModel() {
        viewModelScope.launch {
            modelDownloadManager.deleteModel()
        }
    }

    fun setWifiOnly(wifiOnly: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setWifiOnlyDownload(wifiOnly)
        }
    }

    fun saveHfApiToken(token: String?) {
        viewModelScope.launch {
            preferencesRepository.setHfApiToken(token)
        }
    }

    fun saveCustomModelUrl(url: String?) {
        viewModelScope.launch {
            preferencesRepository.setCustomModelUrl(url)
        }
    }

    fun scanLocalModel(): Boolean {
        return modelDownloadManager.scanForLocalModel()
    }

    fun importModel(uri: android.net.Uri) {
        viewModelScope.launch {
            modelDownloadManager.importModelFromUri(uri)
        }
    }

    suspend fun runBenchmark(): String {
        val start = System.currentTimeMillis()
        val response = aiAdvisorEngine.answerQuery("How much did I spend this month?")
        val elapsed = System.currentTimeMillis() - start
        return "Benchmark executed locally in ${elapsed}ms:\n$response"
    }
}

@Composable
fun AiModelManagementScreen(
    viewModel: AiModelManagementViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val downloadState by viewModel.downloadState.collectAsState()
    val isWifiOnly by viewModel.isWifiOnly.collectAsState()
    val hfApiToken by viewModel.hfApiToken.collectAsState()
    val customModelUrl by viewModel.customModelUrl.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var benchmarkResult by remember { mutableStateOf<String?>(null) }
    var isBenchmarking by remember { mutableStateOf(false) }
    var showSourceConfigDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.importModel(it)
            Toast.makeText(context, "Importing model from storage...", Toast.LENGTH_SHORT).show()
        }
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
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(
                    text = "On-Device AI Engine",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Gemma 2B LiteRT on-device intelligence",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Main Gemma 2B Card
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
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = CobaltPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Gemma 2B (LiteRT)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Official Google AI Quantized Model",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    when (downloadState) {
                        is ModelDownloadState.Ready -> {
                            SpendwellPillBadge(
                                text = "INSTALLED",
                                backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                                textColor = EmeraldSecondary,
                                icon = Icons.Default.CheckCircle
                            )
                        }
                        is ModelDownloadState.Downloading -> {
                            SpendwellPillBadge(
                                text = "DOWNLOADING",
                                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                                textColor = CobaltPrimary,
                                icon = Icons.Default.CloudDownload
                            )
                        }
                        else -> {
                            SpendwellPillBadge(
                                text = "OFFLINE ONLY",
                                backgroundColor = SurfaceContainerLow,
                                textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                icon = Icons.Default.Memory
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Dynamic Download Content
                when (val state = downloadState) {
                    is ModelDownloadState.NotDownloaded -> {
                        Text(
                            text = "Download the official on-device Gemma 2B model to enable advanced spam categorization and natural conversational ledger intelligence.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Source & Auth Card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceContainerLow)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (!customModelUrl.isNullOrBlank()) Icons.Default.Link else Icons.Default.Key,
                                        contentDescription = null,
                                        tint = CobaltPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Download Source & Auth",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (!customModelUrl.isNullOrBlank()) {
                                        "Custom URL: ${customModelUrl!!.take(28)}..."
                                    } else if (!hfApiToken.isNullOrBlank()) {
                                        "Hugging Face (Token Configured: hf_...)"
                                    } else {
                                        "Hugging Face (Token required for Gemma 2B)"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (customModelUrl.isNullOrBlank() && hfApiToken.isNullOrBlank()) CrimsonTertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            SpendwellTonalButton(
                                text = "Configure",
                                leadingIcon = Icons.Default.Settings,
                                onClick = { showSourceConfigDialog = true }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceContainerLow)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Wifi, contentDescription = null, tint = CobaltPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Download on Wi-Fi only",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Switch(
                                checked = isWifiOnly,
                                onCheckedChange = { viewModel.setWifiOnly(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CobaltPrimary)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        SpendwellPrimaryButton(
                            text = "Download Gemma 2B",
                            leadingIcon = Icons.Default.CloudDownload,
                            onClick = { viewModel.startDownload() },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SpendwellTonalButton(
                                text = "Import .bin File",
                                leadingIcon = Icons.Default.UploadFile,
                                onClick = { filePickerLauncher.launch("*/*") },
                                modifier = Modifier.weight(1f)
                            )
                            SpendwellTonalButton(
                                text = "Scan ADB",
                                leadingIcon = Icons.Default.Folder,
                                onClick = {
                                    val found = viewModel.scanLocalModel()
                                    Toast.makeText(
                                        context,
                                        if (found) "Model detected and ready!" else "No model file found in internal models/ directory.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceContainerLow)
                                .clickable { showSourceConfigDialog = true }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = null,
                                tint = CobaltPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Step-by-Step Setup Guide",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Need help getting a token or downloading? Tap for complete instructions.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = CobaltPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    is ModelDownloadState.CheckingNetwork -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = CobaltPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = "Connecting to official Google AI repository...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    is ModelDownloadState.Downloading -> {
                        val downloadedMb = state.bytesDownloaded / (1024 * 1024)
                        val totalMb = state.totalBytes / (1024 * 1024)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${state.progressPercentage}% downloaded ($downloadedMb MB / $totalMb MB)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = String.format(Locale.ENGLISH, "%.1f KB/s", state.speedKbps),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CobaltPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { (state.progressPercentage / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                color = CobaltPrimary,
                                trackColor = SurfaceContainerHigh
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            SpendwellTonalButton(
                                text = "Cancel Download",
                                onClick = { viewModel.cancelDownload() }
                            )
                        }
                    }

                    is ModelDownloadState.VerifyingChecksum -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = CobaltPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = "Verifying SHA-256 binary integrity...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    is ModelDownloadState.Ready -> {
                        val sizeMb = state.sizeBytes / (1024 * 1024)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                HardwareSpecItem(icon = Icons.Default.Storage, label = "Model Size", value = "$sizeMb MB")
                                HardwareSpecItem(icon = Icons.Default.Memory, label = "Quantization", value = "INT4 CPU/NPU")
                                HardwareSpecItem(icon = Icons.Default.Speed, label = "Architecture", value = "LiteRT Edge")
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SpendwellPrimaryButton(
                                    text = if (isBenchmarking) "Benchmarking..." else "Run Test Query",
                                    leadingIcon = Icons.Default.Speed,
                                    onClick = {
                                        if (!isBenchmarking) {
                                            isBenchmarking = true
                                            scope.launch {
                                                try {
                                                    benchmarkResult = viewModel.runBenchmark()
                                                } finally {
                                                    isBenchmarking = false
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                SpendwellTonalButton(
                                    text = "Delete",
                                    leadingIcon = Icons.Default.Delete,
                                    onClick = { viewModel.deleteModel() }
                                )
                            }
                        }
                    }

                    is ModelDownloadState.Error -> {
                        val isAuthError = state.message.contains("401") || state.message.contains("403") || state.message.contains("authentication", ignoreCase = true)
                        Column {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, tint = CrimsonTertiary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CrimsonTertiary
                                    )
                                    if (isAuthError) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Tip: Gemma 2B is a gated model. Ensure you have accepted terms at huggingface.co/google/gemma-2b-it-tflite and your token has 'Read' role. Or download the .bin file via browser and tap 'Import File'.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SpendwellPrimaryButton(
                                    text = if (isAuthError) "Configure Token" else "Retry Download",
                                    leadingIcon = if (isAuthError) Icons.Default.Key else Icons.Default.Refresh,
                                    onClick = {
                                        if (isAuthError) showSourceConfigDialog = true else viewModel.startDownload()
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                SpendwellTonalButton(
                                    text = "Import File",
                                    leadingIcon = Icons.Default.UploadFile,
                                    onClick = { filePickerLauncher.launch("*/*") }
                                )
                                if (isAuthError) {
                                    SpendwellTonalButton(
                                        text = "Retry",
                                        leadingIcon = Icons.Default.Refresh,
                                        onClick = { viewModel.startDownload() }
                                    )
                                } else {
                                    SpendwellTonalButton(
                                        text = "Configure",
                                        leadingIcon = Icons.Default.Settings,
                                        onClick = { showSourceConfigDialog = true }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (benchmarkResult != null) {
            Spacer(modifier = Modifier.height(16.dp))
            SpendwellCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "Local Execution Output",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CobaltPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = benchmarkResult!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Privacy Policy Card
        SpendwellCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    text = "Strict Private Inference Policy",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• AI inference runs exclusively on your device hardware.\n• Internet permission is used solely for downloading official models and updates.\n• Zero audio, financial SMS tokens, or personal logs are transmitted to any cloud servers.\n• All financial analysis runs read-only without modifying your private ledger.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (showSourceConfigDialog) {
            DownloadSourceConfigDialog(
                initialHfToken = hfApiToken,
                initialCustomUrl = customModelUrl,
                onDismiss = { showSourceConfigDialog = false },
                onSave = { token, url ->
                    viewModel.saveHfApiToken(token)
                    viewModel.saveCustomModelUrl(url)
                    showSourceConfigDialog = false
                    Toast.makeText(context, "Download source settings updated.", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun DownloadSourceConfigDialog(
    initialHfToken: String?,
    initialCustomUrl: String?,
    onDismiss: () -> Unit,
    onSave: (hfToken: String?, customUrl: String?) -> Unit
) {
    var selectedTab by remember { mutableStateOf(if (!initialCustomUrl.isNullOrBlank()) 1 else 0) }
    var hfToken by remember { mutableStateOf(initialHfToken ?: "") }
    var customUrl by remember { mutableStateOf(initialCustomUrl ?: "") }
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = CobaltPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Gemma 2B Model Setup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Authentication & Download Options",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Segmented tab switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceContainerLow)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabs = listOf("Hugging Face", "Custom URL", "Manual / ADB")
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CobaltPrimary else Color.Transparent)
                                .clickable { selectedTab = index }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (selectedTab) {
                    0 -> { // Hugging Face Tab
                        Text(
                            text = "Gemma 2B is an open gated model from Google. To download it directly in-app, complete these 3 steps:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Step 1
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceContainerLow)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Step 1: Accept License Terms",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Log in to Hugging Face and accept Google's terms on the official Gemma 2B LiteRT repository.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            SpendwellTonalButton(
                                text = "1. Open License Terms ↗",
                                leadingIcon = Icons.Default.OpenInNew,
                                onClick = {
                                    uriHandler.openUri("https://huggingface.co/google/gemma-2b-it-tflite")
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Step 2
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceContainerLow)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Step 2: Generate Access Token",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Go to your HF settings, create a User Access Token with the 'Read' role.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            SpendwellTonalButton(
                                text = "2. Open Token Settings ↗",
                                leadingIcon = Icons.Default.OpenInNew,
                                onClick = {
                                    uriHandler.openUri("https://huggingface.co/settings/tokens")
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Step 3
                        Text(
                            text = "Step 3: Paste Your Token",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = hfToken,
                            onValueChange = { hfToken = it },
                            label = { Text("Access Token (hf_...)") },
                            placeholder = { Text("hf_xxxxxxxxxxxxxxxxxxxxxx") },
                            trailingIcon = {
                                IconButton(onClick = {
                                    val clip = clipboardManager.getText()?.text
                                    if (!clip.isNullOrBlank()) {
                                        hfToken = clip.trim()
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "Paste",
                                        tint = CobaltPrimary
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (hfToken.startsWith("hf_")) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSecondary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Valid Hugging Face token format", style = MaterialTheme.typography.labelSmall, color = EmeraldSecondary)
                            }
                        } else if (hfToken.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tokens usually start with 'hf_'", style = MaterialTheme.typography.labelSmall, color = WarningAmber)
                            }
                        }
                    }

                    1 -> { // Custom URL Tab
                        Text(
                            text = "If you have hosted the model binary on a local network HTTP server, self-hosted mirror, or offline test environment, enter the direct URL below:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = customUrl,
                            onValueChange = { customUrl = it },
                            label = { Text("Custom Model URL") },
                            placeholder = { Text("http://192.168.1.5:8080/gemma-2b-it-cpu-int4.bin") },
                            trailingIcon = {
                                IconButton(onClick = {
                                    val clip = clipboardManager.getText()?.text
                                    if (!clip.isNullOrBlank()) {
                                        customUrl = clip.trim()
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "Paste",
                                        tint = CobaltPrimary
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "When configured, Spendwell downloads directly from this URL without requiring a Hugging Face token.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    2 -> { // Manual / ADB Tab
                        Text(
                            text = "Don't want to use an API token? You can download the model via your phone's browser or transfer it via ADB from PC:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Method A
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceContainerLow)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Method A: Browser Download & Import",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "1. Download 'gemma-2b-it-cpu-int4.bin' (~1.4 GB) directly using Chrome.\n2. On the AI Model screen, tap 'Import .bin File' and select it from your Downloads folder.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            SpendwellTonalButton(
                                text = "Open Download Page ↗",
                                leadingIcon = Icons.Default.OpenInNew,
                                onClick = {
                                    uriHandler.openUri("https://huggingface.co/google/gemma-2b-it-tflite/tree/main")
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Method B
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceContainerLow)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Method B: ADB Push (PC via USB)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Run these commands in terminal:\n\nadb push gemma-2b-it-cpu-int4.bin /data/local/tmp/\nadb shell run-as com.innovatex.spendwell cp /data/local/tmp/gemma-2b-it-cpu-int4.bin /data/data/com.innovatex.spendwell/files/models/\n\nThen tap 'Scan ADB' on the AI screen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            SpendwellTonalButton(
                                text = "Copy ADB Commands",
                                leadingIcon = Icons.Default.ContentCopy,
                                onClick = {
                                    clipboardManager.setText(AnnotatedString("adb push gemma-2b-it-cpu-int4.bin /data/local/tmp/ && adb shell run-as com.innovatex.spendwell cp /data/local/tmp/gemma-2b-it-cpu-int4.bin /data/data/com.innovatex.spendwell/files/models/"))
                                    Toast.makeText(context, "ADB commands copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            SpendwellPrimaryButton(
                text = "Save Configuration",
                onClick = {
                    onSave(
                        hfToken.trim().ifEmpty { null },
                        customUrl.trim().ifEmpty { null }
                    )
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun HardwareSpecItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = CobaltPrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
