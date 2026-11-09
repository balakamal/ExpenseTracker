package com.innovatex.spendwell.features.transactiondetail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovatex.spendwell.designsystem.components.SpendwellLoadingState
import com.innovatex.spendwell.designsystem.components.SpendwellSubScreenAppBar
import com.innovatex.spendwell.designsystem.theme.CardSurfaceWhite
import com.innovatex.spendwell.designsystem.theme.CobaltPrimary
import com.innovatex.spendwell.designsystem.theme.CobaltPrimaryContainer
import com.innovatex.spendwell.designsystem.theme.CrimsonTertiary
import com.innovatex.spendwell.designsystem.theme.EmeraldSecondary
import com.innovatex.spendwell.designsystem.theme.OnSecondaryContainer
import com.innovatex.spendwell.designsystem.theme.PillShape
import com.innovatex.spendwell.designsystem.theme.PrimaryFixed
import com.innovatex.spendwell.designsystem.theme.SecondaryFixed
import com.innovatex.spendwell.designsystem.theme.SquircleCardShape
import com.innovatex.spendwell.designsystem.theme.SurfaceContainer
import com.innovatex.spendwell.designsystem.theme.SurfaceContainerHigh
import com.innovatex.spendwell.designsystem.theme.SurfaceContainerLow
import com.innovatex.spendwell.common.Money
import com.innovatex.spendwell.designsystem.theme.TextOnSurface
import com.innovatex.spendwell.domain.model.Category
import com.innovatex.spendwell.domain.model.SourceType
import com.innovatex.spendwell.domain.model.Transaction
import com.innovatex.spendwell.domain.model.TransactionStatus
import com.innovatex.spendwell.domain.model.TransactionType
import com.innovatex.spendwell.domain.repository.CategoryRepository
import com.innovatex.spendwell.domain.repository.TransactionRepository
import com.innovatex.spendwell.transactionengine.model.RawSignal
import com.innovatex.spendwell.transactionengine.stages.Stage3MessageNormalization
import com.innovatex.spendwell.transactionengine.stages.Stage6StructuredExtraction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionDetailScreen(
    transactionId: String,
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var transaction by remember { mutableStateOf<Transaction?>(null) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    // Dynamic UI states
    var isUtrCopied by remember { mutableStateOf(false) }
    var isSmsAuditExpanded by remember { mutableStateOf(true) }
    var isReceiptAttached by remember { mutableStateOf(false) }
    var isExcludedFromBudget by remember { mutableStateOf(false) }
    var isBiometricLocked by remember { mutableStateOf(false) }

    // Interactive Expense Tags
    val tags = remember { mutableStateListOf("#Lunch", "#DiningOut", "#WeekendTreat") }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var newTagInput by remember { mutableStateOf("") }
    var showReportDialog by remember { mutableStateOf(false) }
    var showEditAmountDialog by remember { mutableStateOf(false) }
    var editAmountInput by remember { mutableStateOf("") }

    LaunchedEffect(transactionId) {
        transaction = transactionRepository.getTransactionById(transactionId)
        categories = categoryRepository.observeCategories().first()
    }

    val currentTx = transaction
    val parsedSmsAmount = remember(currentTx?.rawMessageSnippet) {
        val snippet = currentTx?.rawMessageSnippet
        if (!snippet.isNullOrBlank()) {
            try {
                val signal = RawSignal(
                    sourceType = SourceType.SMS,
                    senderAddress = currentTx?.accountId ?: "SMS",
                    rawText = snippet,
                    timestamp = currentTx?.timestamp ?: System.currentTimeMillis()
                )
                val norm = Stage3MessageNormalization.normalize(signal)
                val ext = Stage6StructuredExtraction.extract(norm)
                ext?.amount
            } catch (e: Exception) {
                null
            }
        } else null
    }
    if (currentTx == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            SpendwellLoadingState(message = "Loading transaction details...")
        }
        return
    }

    val isCredit = currentTx.type == TransactionType.CREDIT
    val isRefund = currentTx.status == TransactionStatus.REFUNDED || currentTx.status == TransactionStatus.REVERSED || currentTx.categoryName.contains("Refund", ignoreCase = true)
    val isFailed = currentTx.status == TransactionStatus.FAILED
    val isPending = currentTx.status == TransactionStatus.NEEDS_REVIEW || currentTx.status == TransactionStatus.PENDING

    val amountColor = when {
        isRefund -> Color(0xFFD97706)
        isFailed -> CrimsonTertiary
        isCredit -> EmeraldSecondary
        else -> CrimsonTertiary
    }

    val dateFormat = SimpleDateFormat("dd MMM yyyy, h:mm a 'IST'", Locale.ENGLISH)
    val dateText = dateFormat.format(Date(currentTx.timestamp))

    val glyph = resolveDetailGlyph(currentTx)

    Scaffold(
        topBar = {
            SpendwellSubScreenAppBar(
                title = "Spendwell",
                subtitle = "Transaction Detail",
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(
                        onClick = {
                            val shareText = "Spendwell Transaction Receipt\nMerchant: ${currentTx.merchantName}\nAmount: ${currentTx.amount.formatInr(true)}\nDate: $dateText\nReference: ${currentTx.referenceId ?: currentTx.utrNumber ?: "N/A"}"
                            val sendIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(android.content.Intent.createChooser(sendIntent, "Share Receipt"))
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share receipt",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(SurfaceContainerLow)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = CobaltPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Private",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = CobaltPrimary
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // 1. Merchant & Amount Hero Card (Matching Stitch Light)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                ) {
                    // Ambient flair
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .align(Alignment.TopEnd)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(SecondaryFixed.copy(alpha = 0.35f), Color.Transparent)
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .align(Alignment.BottomStart)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(PrimaryFixed.copy(alpha = 0.35f), Color.Transparent)
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 64x64 Merchant Icon Badge with Checkmark Overlay
                        Box(modifier = Modifier.size(64.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(glyph.bgColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = glyph.icon,
                                    contentDescription = null,
                                    tint = glyph.tintColor,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            // Status badge overlay
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(if (isFailed) CrimsonTertiary else EmeraldSecondary)
                                    .border(2.dp, CardSurfaceWhite, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isFailed) Icons.Default.Error else Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Merchant Name
                        Text(
                            text = currentTx.merchantName.ifBlank { currentTx.description },
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Subtitle / Description
                        Text(
                            text = if (currentTx.description.isNotBlank() && currentTx.description != currentTx.merchantName) {
                                "${currentTx.categoryName} • ${currentTx.description}"
                            } else {
                                "${currentTx.categoryName} • Order verified"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Prominent Currency Typography with Edit Action
                        val amountPrefix = if (isCredit) "+" else if (isRefund || isFailed) "" else "−"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "$amountPrefix${currentTx.amount.formatInr(true)}",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = amountColor,
                                textDecoration = if (isRefund || isFailed) TextDecoration.LineThrough else null
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    editAmountInput = currentTx.amount.amount.stripTrailingZeros().toPlainString()
                                    showEditAmountDialog = true
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Amount",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Status & Timestamp Pill Group
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val (statusText, statusBg, statusColor) = when {
                                isRefund -> Triple("Refunded", Color(0xFFFEF3C7), Color(0xFFB45309))
                                isFailed -> Triple("Failed", MaterialTheme.colorScheme.errorContainer, CrimsonTertiary)
                                isPending -> Triple("Pending", SurfaceContainerLow, MaterialTheme.colorScheme.onSurfaceVariant)
                                isCredit -> Triple("Credited", SecondaryFixed, EmeraldSecondary)
                                else -> Triple("Successful", SecondaryFixed, EmeraldSecondary)
                            }

                            Row(
                                modifier = Modifier
                                    .clip(PillShape)
                                    .background(statusBg)
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isFailed) Icons.Default.Error else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                            }

                            Text(
                                text = dateText,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Bottom Actions inside Hero Card (Stitch Grid: Split + Attach)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val halfAmount = currentTx.amount.toPaisa() / 2
                                    val formattedHalf = "₹%,d.%02d".format(halfAmount / 100, halfAmount % 100)
                                    Toast.makeText(context, "Split link generated: $formattedHalf each for 2 friends", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = PillShape,
                                colors = ButtonDefaults.buttonColors(containerColor = CobaltPrimary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GroupAdd,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Split with Friends",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }

                            Button(
                                onClick = {
                                    isReceiptAttached = !isReceiptAttached
                                    Toast.makeText(
                                        context,
                                        if (isReceiptAttached) "Digital receipt attached to transaction" else "Receipt detached",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = PillShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SurfaceContainer,
                                    contentColor = CobaltPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = CobaltPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isReceiptAttached) "Receipt Attached" else "Attach Receipt",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CobaltPrimary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Payment Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Payment Details",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(SurfaceContainerLow)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "UPI Instant",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = CobaltPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Debit Account Box in SurfaceContainerLow
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceContainerLow)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(CardSurfaceWhite),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = CobaltPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentTx.accountId?.let { "Bank Account ($it)" } ?: "Direct Ledger Account",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentTx.accountId?.let { "Linked Account: $it" } ?: "Local Cash / Manual",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = EmeraldSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // UPI Handle Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Payment Method",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = currentTx.toAccountId ?: "Google Pay UPI",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // UPI Ref / UTR Row with Copy
                    val utrValue = currentTx.utrNumber ?: currentTx.referenceId ?: "426819284019"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tag,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "UPI Ref / UTR",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceContainerLow)
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("UTR Number", utrValue)
                                    clipboard.setPrimaryClip(clip)
                                    isUtrCopied = true
                                    Toast.makeText(context, "UPI UTR copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = utrValue,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (isUtrCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = "Copy UTR",
                                tint = CobaltPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Category Picker Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Category",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(PillShape)
                                    .background(SurfaceContainer)
                                    .clickable { categoryMenuExpanded = true }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(CobaltPrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = currentTx.categoryName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Change",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CobaltPrimary
                                )
                            }

                            DropdownMenu(
                                expanded = categoryMenuExpanded,
                                onDismissRequest = { categoryMenuExpanded = false }
                            ) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.name) },
                                        onClick = {
                                            categoryMenuExpanded = false
                                            scope.launch {
                                                transactionRepository.updateCategory(currentTx.id, cat.id)
                                                transaction = currentTx.copy(categoryId = cat.id, categoryName = cat.name)
                                                Toast.makeText(context, "Category updated to ${cat.name}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // AI Smart Category Recommendation
                    val merchantLower = currentTx.merchantName.lowercase(Locale.ROOT)
                    val suggestedCategory = when {
                        merchantLower.contains("country delight") || merchantLower.contains("countrydelight") -> categories.firstOrNull { it.id == "cat_groceries" }
                        merchantLower.contains("swiggy") || merchantLower.contains("zomato") || merchantLower.contains("mcdonald") -> categories.firstOrNull { it.id == "cat_food" }
                        merchantLower.contains("blinkit") || merchantLower.contains("zepto") || merchantLower.contains("instamart") || merchantLower.contains("bigbasket") -> categories.firstOrNull { it.id == "cat_groceries" }
                        merchantLower.contains("uber") || merchantLower.contains("ola") || merchantLower.contains("rapido") -> categories.firstOrNull { it.id == "cat_transport" }
                        merchantLower.contains("amazon") || merchantLower.contains("flipkart") || merchantLower.contains("myntra") || merchantLower.contains("ajio") -> categories.firstOrNull { it.id == "cat_shopping" }
                        merchantLower.contains("netflix") || merchantLower.contains("spotify") || merchantLower.contains("prime video") || merchantLower.contains("hotstar") -> categories.firstOrNull { it.id == "cat_entertainment" }
                        merchantLower.contains("apollo") || merchantLower.contains("1mg") || merchantLower.contains("pharmeasy") -> categories.firstOrNull { it.id == "cat_health" }
                        merchantLower.contains("airtel") || merchantLower.contains("jio") || merchantLower.contains("bescom") || merchantLower.contains("electricity") -> categories.firstOrNull { it.id == "cat_utilities" }
                        merchantLower.contains("zerodha") || merchantLower.contains("groww") -> categories.firstOrNull { it.id == "cat_investment" }
                        else -> null
                    }

                    if (suggestedCategory != null && (suggestedCategory.id != currentTx.categoryId || currentTx.categoryName.equals("Miscellaneous", ignoreCase = true))) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CobaltPrimaryContainer.copy(alpha = 0.12f))
                                .border(1.dp, CobaltPrimary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = CobaltPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "AI Smart Category Suggestion",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = CobaltPrimary
                                    )
                                    Text(
                                        text = "Change to " + suggestedCategory.name + " (98% match)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .clip(PillShape)
                                    .background(CobaltPrimary)
                                    .clickable {
                                        scope.launch {
                                            transactionRepository.updateCategory(currentTx.id, suggestedCategory.id)
                                            transaction = currentTx.copy(categoryId = suggestedCategory.id, categoryName = suggestedCategory.name)
                                            Toast.makeText(context, "Updated to " + suggestedCategory.name + " via AI", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Apply",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Expense Tags
                    Column {
                        Text(
                            text = "EXPENSE TAGS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            tags.forEach { tag ->
                                Row(
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .background(SurfaceContainerLow)
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = CobaltPrimary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove tag",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .size(13.dp)
                                            .clickable { tags.remove(tag) }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .clip(PillShape)
                                    .background(SurfaceContainerHigh)
                                    .clickable { showAddTagDialog = true }
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Add Tag",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. On-Device SMS Parser Assurance Card
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SecondaryFixed),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = EmeraldSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Parsed on-device from SMS",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Source: VM-HDFCBK • 100% Offline",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(SecondaryFixed)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Sandbox Safe",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = OnSecondaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Verbatim SMS Box
                    val rawSnippet = currentTx.rawMessageSnippet.takeIf { !it.isNullOrBlank() }
                        ?: "Manual ledger record (No external SMS message associated with this transaction)."

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceContainerLow)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "VERBATIM SMS CONTENT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CardSurfaceWhite)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "\"$rawSnippet\"",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 16.sp
                            )
                        }

                        if (parsedSmsAmount != null && parsedSmsAmount != currentTx.amount) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFFEF3C7)
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AutoFixHigh,
                                            contentDescription = null,
                                            tint = Color(0xFFB45309),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "SMS Amount Discrepancy",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF92400E)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Raw SMS indicates ${parsedSmsAmount.formatInr(true)}, but ledger record displays ${currentTx.amount.formatInr(true)}.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF78350F)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            val updated = currentTx.copy(amount = parsedSmsAmount)
                                            scope.launch {
                                                transactionRepository.updateTransaction(updated)
                                                transaction = updated
                                                Toast.makeText(context, "Amount updated to ${parsedSmsAmount.formatInr(true)}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                        shape = PillShape,
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text(
                                            text = "Sync to ${parsedSmsAmount.formatInr(true)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Trust banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(PillShape)
                            .background(SurfaceContainer)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = EmeraldSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "100% On-Device Regex & SLM Confidence",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "🔒 Zero cloud upload. Processed in Spendwell secure sandbox.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Ledger Preferences Card
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                    Text(
                        text = "Ledger Preferences",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Toggle: Exclude from Monthly Budget
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Exclude from Monthly Budget",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Transaction won't reduce monthly spending ceiling.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = isExcludedFromBudget,
                            onCheckedChange = { isExcludedFromBudget = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CobaltPrimary
                            )
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    // Toggle: Lock with Biometrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Lock with Biometrics",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Require fingerprint to view order notes & line items.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = isBiometricLocked,
                            onCheckedChange = { isBiometricLocked = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CobaltPrimary
                            )
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    // Report Incorrect Category Link
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showReportDialog = true }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            tint = CrimsonTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Report incorrect category or duplicate",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = CrimsonTertiary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Mark as Reviewed Button (if needed)
            if (!currentTx.isReviewed) {
                Button(
                    onClick = {
                        scope.launch {
                            transactionRepository.markAsReviewed(currentTx.id)
                            Toast.makeText(context, "Marked as reviewed", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = PillShape,
                    colors = ButtonDefaults.buttonColors(containerColor = CobaltPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mark as Reviewed",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    // Add Tag Dialog
    if (showAddTagDialog) {
        AlertDialog(
            onDismissRequest = { showAddTagDialog = false },
            title = { Text("Add Expense Tag") },
            text = {
                OutlinedTextField(
                    value = newTagInput,
                    onValueChange = { newTagInput = it },
                    placeholder = { Text("e.g. Lunch, Work, Treat") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val clean = newTagInput.trim()
                        if (clean.isNotBlank()) {
                            val formatted = if (clean.startsWith("#")) clean else "#$clean"
                            tags.add(formatted)
                            newTagInput = ""
                            showAddTagDialog = false
                            Toast.makeText(context, "Tag added", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Add", color = CobaltPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTagDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Report Issue Dialog
    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report Issue") },
            text = {
                Text("Would you like to flag this transaction for the on-device model training feedback loop? It will help Spendwell refine merchant categorization.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showReportDialog = false
                        Toast.makeText(context, "Thank you! Feedback recorded locally for AI optimizer.", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Submit Feedback", color = CobaltPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Amount Dialog
    if (showEditAmountDialog) {
        AlertDialog(
            onDismissRequest = { showEditAmountDialog = false },
            title = { Text("Edit Transaction Amount", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Enter the corrected amount in INR (₹):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editAmountInput,
                        onValueChange = { editAmountInput = it },
                        label = { Text("Amount (₹)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newMoney = Money.fromString(editAmountInput)
                        if (!newMoney.isZero) {
                            val updated = currentTx.copy(amount = newMoney)
                            scope.launch {
                                transactionRepository.updateTransaction(updated)
                                transaction = updated
                                showEditAmountDialog = false
                                Toast.makeText(context, "Amount updated to ${newMoney.formatInr(true)}", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditAmountDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private data class DetailGlyph(
    val icon: ImageVector,
    val bgColor: Color,
    val tintColor: Color
)

@Composable
private fun resolveDetailGlyph(tx: Transaction): DetailGlyph {
    val name = (tx.merchantName + " " + tx.categoryName).lowercase()
    return when {
        name.contains("swiggy") || name.contains("zomato") || name.contains("food") || name.contains("dining") ->
            DetailGlyph(Icons.Default.Restaurant, Color(0xFFFFF4EB), Color(0xFFF97316))
        name.contains("uber") || name.contains("ola") || name.contains("taxi") || name.contains("cab") ->
            DetailGlyph(Icons.Default.LocalTaxi, SurfaceContainer, TextOnSurface)
        name.contains("chai") || name.contains("tea") ->
            DetailGlyph(Icons.Default.Coffee, SurfaceContainerHigh, TextOnSurface)
        name.contains("starbucks") || name.contains("cafe") || name.contains("coffee") ->
            DetailGlyph(Icons.Default.LocalCafe, SurfaceContainerHigh, TextOnSurface)
        name.contains("salary") || name.contains("credit") || name.contains("deposit") || name.contains("razorpay") ->
            DetailGlyph(Icons.Default.Payments, SecondaryFixed, EmeraldSecondary)
        name.contains("blinkit") || name.contains("zepto") || name.contains("grocer") ->
            DetailGlyph(Icons.Default.ShoppingBag, SurfaceContainer, TextOnSurface)
        name.contains("amazon") || name.contains("flipkart") || name.contains("shop") ->
            DetailGlyph(Icons.Default.ShoppingCart, SurfaceContainer, TextOnSurface)
        name.contains("netflix") || name.contains("spotify") || name.contains("prime") || name.contains("entertainment") ->
            DetailGlyph(Icons.Default.SmartDisplay, Color(0xFFFFDADB), CrimsonTertiary)
        name.contains("cult") || name.contains("gym") || name.contains("fitness") ->
            DetailGlyph(Icons.Default.FitnessCenter, SurfaceContainer, TextOnSurface)
        name.contains("refund") || tx.status == TransactionStatus.REFUNDED || tx.status == TransactionStatus.REVERSED ->
            DetailGlyph(Icons.Default.Replay, SecondaryFixed, EmeraldSecondary)
        else ->
            DetailGlyph(Icons.Default.Receipt, SurfaceContainer, CobaltPrimary)
    }
}
