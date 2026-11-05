package com.innovatex.spendwell.features.aiadvisor

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.innovatex.spendwell.ai.AiChatMessage
import com.innovatex.spendwell.ai.MessageSender
import com.innovatex.spendwell.ai.SpendwellAiAdvisorEngine
import com.innovatex.spendwell.designsystem.components.SpendwellPillBadge
import com.innovatex.spendwell.designsystem.theme.PillShape
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun AiAdvisorScreen(
    engine: SpendwellAiAdvisorEngine,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    val messages = remember {
        mutableStateListOf(
            AiChatMessage(
                id = "init_1",
                sender = MessageSender.ADVISOR,
                text = "Hello! I am your Spendwell On-Device Financial Advisor. My queries are 100% private and run strictly locally on this phone. How can I help you today?"
            )
        )
    }

    val suggestionChips = listOf(
        "Monthly expenses?",
        "Top spending categories",
        "Active subscriptions",
        "How can I save more?"
    )

    fun sendUserQuery(query: String) {
        if (query.isBlank()) return
        val userMsg = AiChatMessage(UUID.randomUUID().toString(), MessageSender.USER, query)
        messages.add(userMsg)
        inputText = ""

        scope.launch {
            val response = engine.answerQuery(query)
            messages.add(AiChatMessage(UUID.randomUUID().toString(), MessageSender.ADVISOR, response))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header with On-Device badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "AI Financial Advisor",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "LiteRT On-Device Intelligence",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            SpendwellPillBadge(
                text = "OFFLINE",
                icon = Icons.Default.Lock
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Chat message history
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            reverseLayout = false
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubble(msg)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Suggestion chips
        LazyRow(modifier = Modifier.fillMaxWidth()) {
            items(suggestionChips) { chip ->
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable { sendUserQuery(chip) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = chip,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Input row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Ask about your finances...", style = MaterialTheme.typography.bodyMedium) },
                modifier = Modifier.weight(1f),
                shape = PillShape,
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { sendUserQuery(inputText) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(message: AiChatMessage) {
    val isUser = message.sender == MessageSender.USER
    val bubbleColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val shape = if (isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .clip(shape)
                .background(bubbleColor)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(0.85f)
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
        }
    }
}
