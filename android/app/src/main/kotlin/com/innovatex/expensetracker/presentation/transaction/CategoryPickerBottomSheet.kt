package com.innovatex.expensetracker.presentation.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovatex.expensetracker.domain.model.Category
import com.innovatex.expensetracker.presentation.components.getCategoryIconVector
import com.innovatex.expensetracker.presentation.theme.ApexThemeTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerBottomSheet(
    merchantName: String,
    currentCategoryId: String,
    categories: List<Category>,
    onCategorySelected: (newCategoryId: String, createRule: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var createRuleChecked by remember { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ApexThemeTokens.BgSurfaceElevated,
        shape = ApexThemeTokens.RadiusSheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Reassign Category",
                color = ApexThemeTokens.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Select a category for \"$merchantName\"",
                color = ApexThemeTokens.TextSecondary,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Categories Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(240.dp)
            ) {
                items(categories) { category ->
                    val isSelected = category.id == currentCategoryId
                    val color = runCatching { Color(android.graphics.Color.parseColor(category.colorHex)) }
                        .getOrDefault(ApexThemeTokens.AccentPrimary)

                    Box(
                        modifier = Modifier
                            .clip(ApexThemeTokens.RadiusMd)
                            .background(if (isSelected) color.copy(alpha = 0.18f) else ApexThemeTokens.BgSurface)
                            .border(
                                1.dp,
                                if (isSelected) color else ApexThemeTokens.StrokeSubtle,
                                ApexThemeTokens.RadiusMd
                            )
                            .clickable {
                                onCategorySelected(category.id, createRuleChecked)
                                onDismiss()
                            }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getCategoryIconVector(category.id),
                                    contentDescription = category.name,
                                    tint = color,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = category.name,
                                color = if (isSelected) color else ApexThemeTokens.TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Checkbox: "Always categorize this merchant as..."
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { createRuleChecked = !createRuleChecked }
            ) {
                Checkbox(
                    checked = createRuleChecked,
                    onCheckedChange = { createRuleChecked = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = ApexThemeTokens.AccentPrimary,
                        uncheckedColor = ApexThemeTokens.TextTertiary,
                        checkmarkColor = ApexThemeTokens.BgCanvas
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Always apply for \"$merchantName\"",
                    color = ApexThemeTokens.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}
