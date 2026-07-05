package com.kinetic.keyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Strip above the keys (SPEC.md P4.4 + §5.4): ranked word candidates while typing; the original
 * keyboard's blue punctuation row when idle.
 */
@Composable
fun SuggestionStrip(
    suggestions: List<String>,
    onSuggestion: (String) -> Unit,
    onPunctuation: (String) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(46.dp).background(Color(0xFF1E1E1E)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (suggestions.isEmpty()) {
            PUNCTUATION.forEach { p ->
                Box(
                    Modifier.weight(1f).fillMaxHeight().clickable { onPunctuation(p) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(p, color = Color(0xFF4FC3F7), fontSize = 18.sp)
                }
            }
        } else {
            suggestions.forEachIndexed { index, word ->
                if (index > 0) {
                    Spacer(
                        Modifier.width(1.dp).height(26.dp).background(Color(0xFF3A3A3A)),
                    )
                }
                Box(
                    Modifier.weight(1f).fillMaxHeight().clickable { onSuggestion(word) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        word,
                        color = Color.White,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** The blue quick-punctuation row from the reference photos. */
private val PUNCTUATION = listOf("!", "?", ",", "\"", "'", "|", ":", ";", "(")
