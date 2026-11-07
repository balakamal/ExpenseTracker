package com.innovatex.spendwell.features.aimodel

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
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
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var benchmarkResult by remember { mutableStateOf<String?>(null) }
    var isBenchmarking by remember { mutableStateOf(false) }

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
                        Spacer(modifier = Modifier.height(16.dp))

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
                            text = "Download Gemma 2B (~1.4 GB)",
                            leadingIcon = Icons.Default.CloudDownload,
                            onClick = { viewModel.startDownload() }
                        )
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
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, tint = CrimsonTertiary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CrimsonTertiary
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            SpendwellPrimaryButton(
                                text = "Retry Download",
                                leadingIcon = Icons.Default.Refresh,
                                onClick = { viewModel.startDownload() }
                            )
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
    }
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
