package com.innovatex.spendwell.features.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.innovatex.spendwell.common.Money
import com.innovatex.spendwell.designsystem.components.SpendwellPrimaryButton
import com.innovatex.spendwell.designsystem.theme.BottomSheetTopShape
import com.innovatex.spendwell.designsystem.theme.PillShape
import com.innovatex.spendwell.domain.model.FinancialNature
import com.innovatex.spendwell.domain.model.Transaction
import com.innovatex.spendwell.domain.model.TransactionStatus
import com.innovatex.spendwell.domain.model.TransactionType
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogExpenseSheet(
    onDismiss: () -> Unit,
    onSaveTransaction: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var amountText by remember { mutableStateOf("") }
    var merchantText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Food & Dining") }
    var selectedType by remember { mutableStateOf(TransactionType.DEBIT) }
    var isCashExpense by remember { mutableStateOf(false) }

    val categories = listOf(
        "Food & Dining",
        "Groceries",
        "Shopping",
        "Transportation",
        "Utilities & Bills",
        "Housing & Rent",
        "Entertainment",
        "Health & Medical",
        "Miscellaneous"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = BottomSheetTopShape,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Record Transaction",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Debit vs Credit toggle
                Row {
                    val isDebit = selectedType == TransactionType.DEBIT
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(if (isDebit) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable { selectedType = TransactionType.DEBIT }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Expense",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDebit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(if (!isDebit) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable { selectedType = TransactionType.CREDIT }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Income",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (!isDebit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Amount input
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount (₹)") },
                modifier = Modifier.fillMaxWidth(),
                shape = PillShape,
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Merchant / Description
            OutlinedTextField(
                value = merchantText,
                onValueChange = { merchantText = it },
                label = { Text("Merchant / Description (e.g. Swiggy, Metro)") },
                modifier = Modifier.fillMaxWidth(),
                shape = PillShape,
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Category",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(modifier = Modifier.fillMaxWidth()) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer)
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = cat,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SpendwellPrimaryButton(
                text = "Save Transaction",
                enabled = amountText.isNotBlank() && merchantText.isNotBlank(),
                onClick = {
                    val money = Money.fromString(amountText)
                    val nature = if (selectedType == TransactionType.CREDIT) {
                        FinancialNature.INCOME
                    } else {
                        FinancialNature.CONSUMPTION_EXPENSE
                    }

                    val tx = Transaction(
                        id = UUID.randomUUID().toString(),
                        amount = money,
                        type = selectedType,
                        status = TransactionStatus.SUCCESSFUL,
                        financialNature = nature,
                        timestamp = System.currentTimeMillis(),
                        accountId = null,
                        merchantName = merchantText.trim(),
                        categoryId = when (selectedCategory) {
                            "Food & Dining" -> "cat_food"
                            "Groceries" -> "cat_groceries"
                            "Shopping" -> "cat_shopping"
                            "Transportation" -> "cat_transport"
                            "Utilities & Bills" -> "cat_utilities"
                            "Housing & Rent" -> "cat_housing"
                            "Entertainment" -> "cat_entertainment"
                            "Health & Medical" -> "cat_health"
                            else -> "cat_misc"
                        },
                        categoryName = selectedCategory,
                        description = merchantText.trim(),
                        isReviewed = true,
                        createdAt = System.currentTimeMillis()
                    )
                    onSaveTransaction(tx)
                    onDismiss()
                }
            )
        }
    }
}
