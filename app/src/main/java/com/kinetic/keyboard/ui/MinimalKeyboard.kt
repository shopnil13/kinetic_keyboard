package com.kinetic.keyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Placeholder keyboard for Phase 0 — a handful of keys to prove Compose-in-IME renders and that key
 * presses reach the InputConnection. Replaced by the data-driven layout engine in Phase 1
 * (SPEC.md §6, encoded from reference/EXTRACTED_LAYOUTS.md).
 */
@Composable
fun MinimalKeyboard(
    onText: (String) -> Unit,
    onDelete: () -> Unit,
    onEnter: () -> Unit,
) {
    val rows = listOf(
        listOf("অ", "আ", "ই", "ঈ", "উ"),
        listOf("ক", "খ", "গ", "ঘ", "ঙ"),
        listOf("A", "B", "C", "D", "E"),
    )
    Surface(color = Color(0xFF1E1E1E)) {
        Column(Modifier.fillMaxWidth().padding(6.dp)) {
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    row.forEach { label ->
                        Key(label, Modifier.weight(1f)) { onText(label) }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Key("⌫", Modifier.weight(1f)) { onDelete() }
                Key("space", Modifier.weight(3f)) { onText(" ") }
                Key("⏎", Modifier.weight(1f)) { onEnter() }
            }
        }
    }
}

@Composable
private fun Key(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .padding(3.dp)
            .background(Color(0xFF3A3A3A), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = Color.White, fontSize = 20.sp, textAlign = TextAlign.Center)
    }
}

@Preview
@Composable
private fun MinimalKeyboardPreview() {
    MinimalKeyboard(onText = {}, onDelete = {}, onEnter = {})
}
