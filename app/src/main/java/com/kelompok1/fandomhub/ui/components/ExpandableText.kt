package com.kelompok1.fandomhub.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@Composable
fun ExpandableText(
    text: String,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color.Unspecified,
    maxLines: Int = 3,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var isOverflowing by remember { mutableStateOf(false) }

    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    val styledText = buildAnnotatedString {
        val urlRegex = Regex("((http|https)://[\\w\\d:#@%/;\$()~_?\\+-=\\\\\\.&]*)")
        val tagRegex = Regex("([@#][\\w_]+)")
        
        data class TextMatch(val range: IntRange, val value: String, val type: String)
        
        val urlMatches = urlRegex.findAll(text).map { TextMatch(it.range, it.value, "URL") }
        val tagMatches = tagRegex.findAll(text).map { TextMatch(it.range, it.value, "TAG") }
        
        val allMatches = (urlMatches + tagMatches).sortedBy { it.range.first }.toList()
        
        var currentIndex = 0
        
        allMatches.forEach { match ->
            if (match.range.first > currentIndex) {
                append(text.substring(currentIndex, match.range.first))
            }
            if (match.range.first >= currentIndex) {
                if (match.type == "URL") {
                    pushStringAnnotation(tag = "URL", annotation = match.value)
                    withStyle(style = SpanStyle(color = Color.Blue, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)) {
                        append(match.value)
                    }
                    pop()
                } else {
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)) {
                        append(match.value)
                    }
                }
                currentIndex = match.range.last + 1
            }
        }
        if (currentIndex < text.length) {
            append(text.substring(currentIndex))
        }
    }

    Column(modifier = modifier) {
        // Text Content
        androidx.compose.foundation.text.ClickableText(
            text = styledText,
            style = style.merge(TextStyle(color = color)), // merge to apply default color to non-styled parts
            maxLines = if (expanded) Int.MAX_VALUE else maxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { textLayoutResult ->
                if (!expanded && textLayoutResult.hasVisualOverflow) {
                    isOverflowing = true
                } else if (expanded && textLayoutResult.lineCount > maxLines) {
                    isOverflowing = true 
                }
            },
            onClick = { offset ->
                styledText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        try {
                            uriHandler.openUri(annotation.item)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
            }
        )
        
        if (isOverflowing) {
            Text(
                text = if (expanded) "Read less" else "Read more",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { expanded = !expanded }
                    .padding(top = 4.dp)
            )
        }
    }
}
