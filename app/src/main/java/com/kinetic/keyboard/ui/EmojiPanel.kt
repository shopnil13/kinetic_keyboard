package com.kinetic.keyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinetic.keyboard.emoji.EmojiCategory
import com.kinetic.keyboard.ui.theme.KbTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val RECENTS_ID = "recents"
private const val DELETE_INITIAL_MS = 350L
private const val DELETE_INTERVAL_MS = 50L

/**
 * P5.5: full-keyboard emoji panel — category tabs on top, scrollable grid, ABC/backspace
 * bottom bar. Replaces the strip + key rows at the same total height, so the IME window
 * doesn't jump when it opens.
 */
@Composable
fun EmojiPanel(
    categories: List<EmojiCategory>,
    recents: List<String>,
    theme: KbTheme,
    height: Dp,
    onEmoji: (String) -> Unit,
    onBack: () -> Unit,
    onDelete: () -> Unit,
) {
    var selected by remember {
        mutableStateOf(if (recents.isNotEmpty()) RECENTS_ID else categories.firstOrNull()?.id ?: RECENTS_ID)
    }
    val current = if (selected == RECENTS_ID) recents
    else categories.firstOrNull { it.id == selected }?.emoji.orEmpty()

    Column(Modifier.fillMaxWidth().height(height)) {
        // Category tabs: recents first, then the library categories.
        Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
            CategoryTab("🕘", selected == RECENTS_ID, theme, Modifier.weight(1f), "Recent emoji") {
                selected = RECENTS_ID
            }
            categories.forEach { cat ->
                CategoryTab(cat.icon, selected == cat.id, theme, Modifier.weight(1f), cat.id) {
                    selected = cat.id
                }
            }
        }

        if (current.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("No recent emoji yet", color = theme.hint, fontSize = 14.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 44.dp),
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                items(current, key = { it }) { emoji ->
                    Text(
                        text = emoji,
                        fontSize = 26.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .semantics { role = Role.Button }
                            .clickable { onEmoji(emoji) }
                            .padding(vertical = 7.dp),
                    )
                }
            }
        }

        // Bottom bar: back to letters + backspace (with key repeat, like the main keyboard).
        Row(Modifier.fillMaxWidth().height(44.dp)) {
            PanelKey("ABC", theme, Modifier.weight(2f), "Back to letters") { onBack() }
            Box(Modifier.weight(6f))
            RepeatingDeleteKey(theme, Modifier.weight(2f), onDelete)
        }
    }
}

@Composable
private fun CategoryTab(
    icon: String,
    active: Boolean,
    theme: KbTheme,
    modifier: Modifier,
    description: String,
    onSelect: () -> Unit,
) {
    Box(
        modifier
            .fillMaxHeight()
            .padding(horizontal = 2.dp, vertical = 4.dp)
            .background(if (active) theme.keyPressed else theme.background, RoundedCornerShape(7.dp))
            .semantics { contentDescription = description }
            .clickable(onClick = onSelect),
        contentAlignment = Alignment.Center,
    ) {
        Text(icon, fontSize = 18.sp)
    }
}

@Composable
private fun PanelKey(
    label: String,
    theme: KbTheme,
    modifier: Modifier,
    description: String,
    onTap: () -> Unit,
) {
    Box(
        modifier
            .fillMaxHeight()
            .padding(horizontal = 2.dp, vertical = 3.dp)
            .background(theme.keyModifier, RoundedCornerShape(7.dp))
            .semantics {
                role = Role.Button
                contentDescription = description
            }
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = theme.label, fontSize = 15.sp)
    }
}

@Composable
private fun RepeatingDeleteKey(theme: KbTheme, modifier: Modifier, onDelete: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Box(
        modifier
            .fillMaxHeight()
            .padding(horizontal = 2.dp, vertical = 3.dp)
            .background(if (pressed) theme.keyPressed else theme.keyModifier, RoundedCornerShape(7.dp))
            .semantics {
                role = Role.Button
                contentDescription = "Backspace"
                onClick { onDelete(); true }
            }
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    pressed = true
                    onDelete()
                    val job: Job = scope.launch {
                        delay(DELETE_INITIAL_MS)
                        while (true) {
                            onDelete()
                            delay(DELETE_INTERVAL_MS)
                        }
                    }
                    tryAwaitRelease()
                    job.cancel()
                    pressed = false
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        Text("⌫", color = theme.label, fontSize = 18.sp)
    }
}
