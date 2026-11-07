package com.innovatex.spendwell.features.aiadvisor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import com.innovatex.spendwell.ai.AiChatMessage
import com.innovatex.spendwell.ai.MessageSender
import com.innovatex.spendwell.ai.SpendwellAiAdvisorEngine
import com.innovatex.spendwell.designsystem.components.SpendwellTopAppBar
import com.innovatex.spendwell.designsystem.theme.CardSurfaceWhite
import com.innovatex.spendwell.designsystem.theme.CobaltPrimary
import com.innovatex.spendwell.designsystem.theme.CobaltPrimaryContainer
import com.innovatex.spendwell.designsystem.theme.EmeraldSecondary
import com.innovatex.spendwell.designsystem.theme.InterFontFamily
import com.innovatex.spendwell.designsystem.theme.OnPrimary
import com.innovatex.spendwell.designsystem.theme.OnPrimaryFixed
import com.innovatex.spendwell.designsystem.theme.PillShape
import com.innovatex.spendwell.designsystem.theme.PrimaryFixed
import com.innovatex.spendwell.designsystem.theme.SurfaceContainer
import com.innovatex.spendwell.designsystem.theme.SurfaceContainerHigh
import com.innovatex.spendwell.designsystem.theme.SurfaceContainerLow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun AiAdvisorScreen(
    engine: SpendwellAiAdvisorEngine,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToModelManagement: () -> Unit = {},
    userName: String = "",
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }

    val isGemmaReady = remember { engine.isGemmaModelReady() }
    val isModelDownloaded by engine.isModelDownloaded.collectAsState(initial = false)
    val aiEngineMode by engine.aiEngineMode.collectAsState(initial = "AUTO")
    var engineMenuExpanded by remember { mutableStateOf(false) }

    val greetingName = if (userName.isNotBlank()) " $userName" else ""
    val messages = remember(userName) {
        mutableStateListOf(
            AiChatMessage(
                id = "init_1",
                sender = MessageSender.ADVISOR,
                text = "Good morning$greetingName! Based on your ledger, your finances are processed with 100% on-device privacy. Ask me anything about your cashflow, savings targets, or spending velocity."
            )
        )
    }

    val suggestionChips = listOf(
        Pair("Where did my money go?", Icons.Default.QueryStats),
        Pair("How to save ₹5,000 next month?", Icons.Default.Savings),
        Pair("Analyze recurring subscriptions", Icons.Default.EventRepeat),
        Pair("Show tax deductions", Icons.Default.Receipt)
    )

    fun sendUserQuery(query: String) {
        if (query.isBlank() || isThinking) return
        val userMsg = AiChatMessage(UUID.randomUUID().toString(), MessageSender.USER, query.trim())
        messages.add(userMsg)
        inputText = ""
        isThinking = true

        scope.launch {
            try {
                val response = engine.answerQuery(query.trim(), aiEngineMode)
                messages.add(AiChatMessage(UUID.randomUUID().toString(), MessageSender.ADVISOR, response))
            } catch (e: Exception) {
                messages.add(
                    AiChatMessage(
                        UUID.randomUUID().toString(),
                        MessageSender.ADVISOR,
                        "I was unable to complete the query locally: ${e.message}"
                    )
                )
            } finally {
                isThinking = false
            }
        }
    }

    Scaffold(
        topBar = {
            SpendwellTopAppBar(
                title = "Spendwell",
                subtitle = "AI Advisor",
                isPrivate = true,
                userName = userName,
                onProfileClick = onNavigateToSettings
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // 1. Dynamic Reasoning Engine Selector & Status Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "REASONING ENGINE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = when (aiEngineMode) {
                                            "GEMMA_LLM" -> "Gemma 2B Local LLM"
                                            "APP_ENGINE" -> "Spendwell App Engine"
                                            else -> "Auto (Smart Hybrid)"
                                        },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Box {
                                    Row(
                                        modifier = Modifier
                                            .clip(PillShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .clickable { engineMenuExpanded = true }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = when (aiEngineMode) {
                                                "GEMMA_LLM" -> Icons.Default.Memory
                                                "APP_ENGINE" -> Icons.Default.Bolt
                                                else -> Icons.Default.AutoAwesome
                                            },
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Select",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Dropdown",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = engineMenuExpanded,
                                        onDismissRequest = { engineMenuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(
                                                        text = "Auto (Smart Hybrid)",
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                    Text(
                                                        text = "Prefers Gemma 2B if available, fallback to App Engine",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CobaltPrimary)
                                            },
                                            trailingIcon = {
                                                if (aiEngineMode == "AUTO") {
                                                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = EmeraldSecondary)
                                                }
                                            },
                                            onClick = {
                                                scope.launch { engine.setAiEngineMode("AUTO") }
                                                engineMenuExpanded = false
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(
                                                        text = "Gemma 2B Local LLM",
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                    Text(
                                                        text = "Generative on-device neural model (~140ms/tok)",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Default.Memory, contentDescription = null, tint = CobaltPrimary)
                                            },
                                            trailingIcon = {
                                                if (aiEngineMode == "GEMMA_LLM") {
                                                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = EmeraldSecondary)
                                                }
                                            },
                                            onClick = {
                                                scope.launch { engine.setAiEngineMode("GEMMA_LLM") }
                                                engineMenuExpanded = false
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(
                                                        text = "Spendwell App Engine",
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                    Text(
                                                        text = "Instant deterministic financial ledger calculations",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Default.Bolt, contentDescription = null, tint = CobaltPrimary)
                                            },
                                            trailingIcon = {
                                                if (aiEngineMode == "APP_ENGINE") {
                                                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = EmeraldSecondary)
                                                }
                                            },
                                            onClick = {
                                                scope.launch { engine.setAiEngineMode("APP_ENGINE") }
                                                engineMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Dynamic Status & Warning Banner
                            if (aiEngineMode == "GEMMA_LLM" && !isGemmaReady && !isModelDownloaded) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Gemma weights not installed (~1.3 GB).",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }

                                    Row(
                                        modifier = Modifier
                                            .clip(PillShape)
                                            .background(MaterialTheme.colorScheme.error)
                                            .clickable { onNavigateToModelManagement() }
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudDownload,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Download",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when (aiEngineMode) {
                                                        "GEMMA_LLM" -> if (isGemmaReady || isModelDownloaded) EmeraldSecondary else MaterialTheme.colorScheme.error
                                                        "APP_ENGINE" -> CobaltPrimary
                                                        else -> if (isGemmaReady || isModelDownloaded) EmeraldSecondary else CobaltPrimary
                                                    }
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = when (aiEngineMode) {
                                                "GEMMA_LLM" -> if (isGemmaReady || isModelDownloaded) "Gemma 2B Ready (INT4)" else "Download Required"
                                                "APP_ENGINE" -> "Deterministic Rules Active"
                                                else -> if (isGemmaReady || isModelDownloaded) "Hybrid (Gemma 2B Ready)" else "Hybrid (App Engine Active)"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Text(
                                        text = when (aiEngineMode) {
                                            "GEMMA_LLM" -> "~140ms/tok latency"
                                            "APP_ENGINE" -> "<5ms • Exact Precision"
                                            else -> "0ms cloud lag"
                                        },
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // 2. Suggestion Prompts Horizontal Scroll
                item {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(suggestionChips) { chip ->
                            Row(
                                modifier = Modifier
                                    .clip(PillShape)
                                    .background(CardSurfaceWhite)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                        PillShape
                                    )
                                    .clickable { sendUserQuery(chip.first) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = chip.second,
                                    contentDescription = null,
                                    tint = CobaltPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = chip.first,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // 3. Conversation Messages
                items(messages, key = { it.id }) { msg ->
                    when (msg.sender) {
                        MessageSender.USER -> {
                            // User Message Bubble (Align Right)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                                        .background(SurfaceContainerLow)
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        text = msg.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(msg.timestamp))} You • Local SLM processed",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                        }

                        MessageSender.ADVISOR -> {
                            // Advisor Message Card (Align Left)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                val rawText = msg.text
                                val engineBadge = when {
                                    rawText.startsWith("[Gemma 2B Local LLM]") -> "🧠 Gemma 2B LLM"
                                    rawText.startsWith("[Spendwell App Engine") -> "⚡ App Engine"
                                    rawText.startsWith("[Gemma 2B Mode:") -> "⚠️ Gemma Notice"
                                    else -> null
                                }
                                val cleanText = if (engineBadge != null && rawText.contains("]\n\n")) {
                                    rawText.substringAfter("]\n\n")
                                } else {
                                    rawText
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(CobaltPrimaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Spendwell Brain",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(msg.timestamp)),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (engineBadge != null) {
                                        Box(
                                            modifier = Modifier
                                                .clip(PillShape)
                                                .background(
                                                    if (engineBadge.contains("Gemma")) EmeraldSecondary.copy(alpha = 0.15f)
                                                    else MaterialTheme.colorScheme.primaryContainer
                                                )
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = engineBadge,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                fontWeight = FontWeight.Bold,
                                                color = if (engineBadge.contains("Gemma")) EmeraldSecondary else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(
                                                1.dp,
                                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                                RoundedCornerShape(18.dp)
                                            )
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = cleanText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            lineHeight = 22.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (isThinking) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = CobaltPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val thinkingMsg = when (aiEngineMode) {
                                "GEMMA_LLM" -> "🧠 Running on-device Gemma 2B neural inference..."
                                "APP_ENGINE" -> "⚡ Calculating ledger metrics via Spendwell App Engine..."
                                else -> if (isGemmaReady || isModelDownloaded) "🧠 Consulting Gemma 2B on-device neural model..." else "⚡ Computing ledger analytics on-device..."
                            }
                            Text(
                                text = thinkingMsg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // 4. Chat Input Bar & Disclaimer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Input Pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(CardSurfaceWhite)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            RoundedCornerShape(28.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    BasicTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(
                            fontFamily = InterFontFamily,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (inputText.isEmpty()) {
                                Text(
                                    text = "Ask anything about your money...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            innerTextField()
                        }
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = { sendUserQuery(inputText) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (inputText.isNotBlank() && !isThinking) CobaltPrimary else SurfaceContainerHigh)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank() && !isThinking) OnPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "🔒 SLM suggestions are advisory. Core financial balances and double-entry reconciliation are computed deterministically without neural networks.",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 14.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}
