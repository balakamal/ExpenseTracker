package com.innovatex.expensetracker.presentation.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovatex.expensetracker.domain.model.Category
import com.innovatex.expensetracker.domain.model.TransactionType
import com.innovatex.expensetracker.presentation.components.getCategoryIconVector
import com.innovatex.expensetracker.presentation.theme.PastelKineticTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogExpenseBottomSheet(
    availableCategories: List<Category>,
    currencySymbol: String = "₹",
    onDismissRequest: () -> Unit,
    onSaveExpense: (amount: Double, merchant: String, categoryId: String, type: TransactionType) -> Unit,
    onScanSmsClick: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountText by remember { mutableStateOf("") }
    var merchantText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.DEBIT) }
    var selectedCategoryId by remember { mutableStateOf(availableCategories.firstOrNull()?.id ?: "food_dining") }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = PastelKineticTokens.SurfaceCard,
        shape = PastelKineticTokens.RadiusCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 12.dp)
        ) {
            // Drag Handle
            Box(
                modifier = Modifier
                    .size(width = 38.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(PastelKineticTokens.OutlineVariant)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "NEW ENTRY",
                        color = PastelKineticTokens.TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Log Transaction",
                        color = PastelKineticTokens.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(PastelKineticTokens.SurfaceLow)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = PastelKineticTokens.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Debit vs Credit Type Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(PastelKineticTokens.RadiusFull)
                    .background(PastelKineticTokens.SurfaceLow)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val isDebit = selectedType == TransactionType.DEBIT
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(PastelKineticTokens.RadiusFull)
                        .background(if (isDebit) PastelKineticTokens.SkyFixed else Color.Transparent)
                        .clickable { selectedType = TransactionType.DEBIT }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Debit",
                            tint = if (isDebit) PastelKineticTokens.SkyText else PastelKineticTokens.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Outflow (Expense)",
                            color = if (isDebit) PastelKineticTokens.SkyText else PastelKineticTokens.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(PastelKineticTokens.RadiusFull)
                        .background(if (!isDebit) PastelKineticTokens.AmberFixed else Color.Transparent)
                        .clickable { selectedType = TransactionType.CREDIT }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Credit",
                            tint = if (!isDebit) PastelKineticTokens.AmberText else PastelKineticTokens.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Inflow (Income)",
                            color = if (!isDebit) PastelKineticTokens.AmberText else PastelKineticTokens.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Amount Input
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { char -> char.isDigit() || char == '.' } },
                label = { Text("Amount ($currencySymbol)") },
                placeholder = { Text("0.00") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = PastelKineticTokens.RadiusMd,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = PastelKineticTokens.SurfaceLow,
                    unfocusedContainerColor = PastelKineticTokens.SurfaceLow,
                    focusedIndicatorColor = PastelKineticTokens.AnchorPrimary,
                    unfocusedIndicatorColor = PastelKineticTokens.OutlineVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Merchant / Vendor Input
            OutlinedTextField(
                value = merchantText,
                onValueChange = { merchantText = it },
                label = { Text("Merchant or Description") },
                placeholder = { Text("e.g. Swiggy, Uber, Groceries, Salary") },
                singleLine = true,
                shape = PastelKineticTokens.RadiusMd,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = PastelKineticTokens.SurfaceLow,
                    unfocusedContainerColor = PastelKineticTokens.SurfaceLow,
                    focusedIndicatorColor = PastelKineticTokens.AnchorPrimary,
                    unfocusedIndicatorColor = PastelKineticTokens.OutlineVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Category Selection Chips
            Text(
                text = "SELECT CATEGORY",
                color = PastelKineticTokens.TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableCategories.forEach { category ->
                    val isSelected = category.id == selectedCategoryId
                    Box(
                        modifier = Modifier
                            .clip(PastelKineticTokens.RadiusFull)
                            .background(if (isSelected) PastelKineticTokens.AnchorPrimary else PastelKineticTokens.SurfaceLow)
                            .border(1.dp, if (isSelected) PastelKineticTokens.AnchorPrimary else PastelKineticTokens.OutlineVariant, PastelKineticTokens.RadiusFull)
                            .clickable { selectedCategoryId = category.id }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = getCategoryIconVector(category.id),
                                contentDescription = category.name,
                                tint = if (isSelected) PastelKineticTokens.OnAnchor else PastelKineticTokens.TextPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = category.name,
                                color = if (isSelected) PastelKineticTokens.OnAnchor else PastelKineticTokens.TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save Button
            val isValid = amountText.toDoubleOrNull() != null && amountText.toDouble() > 0
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    onSaveExpense(amt, merchantText, selectedCategoryId, selectedType)
                    onDismissRequest()
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = PastelKineticTokens.RadiusFull,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PastelKineticTokens.AnchorPrimary,
                    contentColor = PastelKineticTokens.OnAnchor,
                    disabledContainerColor = PastelKineticTokens.SurfaceContainer,
                    disabledContentColor = PastelKineticTokens.TextOutline
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save to Ledger",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Secondary Scan SMS Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(PastelKineticTokens.RadiusFull)
                    .clickable {
                        onScanSmsClick()
                        onDismissRequest()
                    }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DocumentScanner,
                    contentDescription = "Scan SMS",
                    tint = PastelKineticTokens.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Or trigger SMS / Alert Ingestion Scan",
                    color = PastelKineticTokens.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

