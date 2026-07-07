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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.emoji2.emojipicker.EmojiPickerView
import androidx.emoji2.emojipicker.RecentEmojiProvider
import com.kinetic.keyboard.ui.theme.KbTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DELETE_INITIAL_MS = 350L
private const val DELETE_INTERVAL_MS = 50L

/**
 * P5.5 rev2: emoji panel built on the AndroidX EmojiPickerView — categories, recents, and
 * built-in skin-tone variant popups, rendered through EmojiCompat's bundled font so every
 * emoji draws correctly regardless of the device's system font. Our own ABC/backspace bar
 * sits below it. Replaces strip + key rows at the same total height.
 */
@Composable
fun EmojiPanel(
    recentEmojiProvider: RecentEmojiProvider,
    theme: KbTheme,
    height: Dp,
    onEmoji: (String) -> Unit,
    onBack: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().height(height).background(theme.background)) {
        AndroidView(
            factory = { context ->
                EmojiPickerView(context).apply {
                    emojiGridColumns = 9
                    setRecentEmojiProvider(recentEmojiProvider)
                    setOnEmojiPickedListener { onEmoji(it.emoji) }
                }
            },
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
        Row(Modifier.fillMaxWidth().height(44.dp)) {
            PanelKey("ABC", theme, Modifier.weight(2f), "Back to letters") { onBack() }
            Box(Modifier.weight(6f))
            RepeatingDeleteKey(theme, Modifier.weight(2f), onDelete)
        }
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
