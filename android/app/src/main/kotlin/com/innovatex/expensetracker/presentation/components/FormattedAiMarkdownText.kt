package com.innovatex.expensetracker.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovatex.expensetracker.presentation.theme.PastelKineticTokens

/**
 * Rich formatted markdown and typographic layout renderer for on-device AI responses.
 * Renders headings, bullet lists, numbered steps, bold highlights, currency callouts, and inline code pills.
 */
@Composable
fun FormattedAiMarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = PastelKineticTokens.TextPrimary,
    baseFontSize: TextUnit = 13.5.sp,
    baseLineHeight: TextUnit = 20.sp
) {
    val lines = remember(text) {
        text.lineSequence()
            .map { it.trimEnd() }
            .filterIndexed { index, line ->
                // Remove repeated blank lines
                line.isNotBlank() || index == 0
            }
            .toList()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        lines.forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.isBlank() -> {
                    // Blank separator
                    Spacer(modifier = Modifier.size(2.dp))
                }
                line.startsWith("### ") -> {
                    Text(
                        text = parseInlineFormatting(line.removePrefix("### "), textColor),
                        fontSize = (baseFontSize.value + 1.5f).sp,
                        fontWeight = FontWeight.Bold,
                        color = PastelKineticTokens.AmberDeep,
                        lineHeight = (baseLineHeight.value + 2f).sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }
                line.startsWith("## ") -> {
                    Text(
                        text = parseInlineFormatting(line.removePrefix("## "), textColor),
                        fontSize = (baseFontSize.value + 2.5f).sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PastelKineticTokens.TextPrimary,
                        lineHeight = (baseLineHeight.value + 3f).sp,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                line.startsWith("# ") -> {
                    Text(
                        text = parseInlineFormatting(line.removePrefix("# "), textColor),
                        fontSize = (baseFontSize.value + 3.5f).sp,
                        fontWeight = FontWeight.Black,
                        color = PastelKineticTokens.TextPrimary,
                        lineHeight = (baseLineHeight.value + 4f).sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }
                line.startsWith("---") || line.startsWith("***") -> {
                    HorizontalDivider(
                        color = PastelKineticTokens.OutlineSubtle,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                line.startsWith("• ") || line.startsWith("- ") || line.startsWith("* ") -> {
                    val bulletContent = line.replaceFirst(Regex("^([•\\-*]\\s+)"), "")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 1.dp, bottom = 1.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 7.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(PastelKineticTokens.AmberAccent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = parseInlineFormatting(bulletContent, textColor),
                            fontSize = baseFontSize,
                            color = textColor,
                            lineHeight = baseLineHeight,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Regex("^\\d+\\.\\s+.*").matches(line) -> {
                    val match = Regex("^(\\d+)\\.\\s+(.*)").find(line)
                    val stepNum = match?.groupValues?.get(1) ?: "1"
                    val stepContent = match?.groupValues?.get(2) ?: line
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 2.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(PastelKineticTokens.SurfaceContainer)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "$stepNum.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PastelKineticTokens.AmberDeep
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = parseInlineFormatting(stepContent, textColor),
                            fontSize = baseFontSize,
                            color = textColor,
                            lineHeight = baseLineHeight,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                else -> {
                    Text(
                        text = parseInlineFormatting(line, textColor),
                        fontSize = baseFontSize,
                        color = textColor,
                        lineHeight = baseLineHeight
                    )
                }
            }
        }
    }
}

/** Parses markdown-style bold (**text**), currency figures, and inline code (`code`). */
private fun parseInlineFormatting(input: String, defaultColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0
        // Match bold **...**, inline code `...`, or currency figures like ₹123 or $123
        val pattern = Regex("(\\*\\*.*?\\*\\*|`.*?`|[₹$][0-9,]+(\\.[0-9]{1,2})?)")
        val matches = pattern.findAll(input)

        for (match in matches) {
            // Append preceding plain text
            if (match.range.first > currentIndex) {
                append(input.substring(currentIndex, match.range.first))
            }

            val token = match.value
            when {
                token.startsWith("**") && token.endsWith("**") && token.length >= 4 -> {
                    val inner = token.substring(2, token.length - 2)
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = defaultColor)) {
                        append(inner)
                    }
                }
                token.startsWith("`") && token.endsWith("`") && token.length >= 2 -> {
                    val inner = token.substring(1, token.length - 1)
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = PastelKineticTokens.AnchorPrimary,
                            background = PastelKineticTokens.SurfaceContainer
                        )
                    ) {
                        append(" $inner ")
                    }
                }
                token.startsWith("₹") || token.startsWith("$") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = PastelKineticTokens.AmberDeep)) {
                        append(token)
                    }
                }
                else -> {
                    append(token)
                }
            }
            currentIndex = match.range.last + 1
        }

        // Append remaining text
        if (currentIndex < input.length) {
            append(input.substring(currentIndex))
        }
    }
}

