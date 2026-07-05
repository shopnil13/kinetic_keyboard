package com.kinetic.keyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.kinetic.keyboard.engine.KeyboardUiState
import com.kinetic.keyboard.engine.model.KeyDef
import com.kinetic.keyboard.engine.model.KeyTypes
import com.kinetic.keyboard.ui.theme.KbTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Actions emitted by the keyboard UI, handled by the IME service (P1.13). */
sealed interface KeyAction {
    data class Text(val text: String) : KeyAction
    data object Delete : KeyAction
    data object Enter : KeyAction
    data object Space : KeyAction
    data object Shift : KeyAction
    data class LayerSwitch(val target: String) : KeyAction
    data object CycleLanguage : KeyAction
}

private const val REPEAT_INITIAL_MS = 350L
private const val REPEAT_INTERVAL_MS = 50L

@Composable
fun KeyboardScreen(
    ui: KeyboardUiState,
    suggestions: List<String>,
    theme: KbTheme,
    keyHeight: Dp,
    onAction: (KeyAction) -> Unit,
    onSuggestion: (String) -> Unit,
) {
    Surface(color = theme.background) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 1.dp, vertical = 3.dp)) {
            SuggestionStrip(
                suggestions = suggestions,
                theme = theme,
                onSuggestion = onSuggestion,
                onPunctuation = { onAction(KeyAction.Text(it)) },
            )
            ui.layout.rows.forEach { row ->
                Row(Modifier.fillMaxWidth().height(keyHeight)) {
                    var used = 0f
                    row.keys.forEach { key ->
                        if (key.gap > 0f) {
                            Spacer(Modifier.weight(key.gap))
                            used += key.gap
                        }
                        val w = key.widthOrDefault()
                        KeyView(key, ui, theme, keyHeight, onAction, Modifier.weight(w))
                        used += w
                    }
                    if (used < 99.5f) Spacer(Modifier.weight(100f - used))
                }
            }
        }
    }
}

@Composable
private fun KeyView(
    key: KeyDef,
    ui: KeyboardUiState,
    theme: KbTheme,
    keyHeight: Dp,
    onAction: (KeyAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pressed by remember { mutableStateOf(false) }
    var popupOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val isModifierKey = key.type != KeyTypes.CHAR
    val popupYOffset = with(LocalDensity.current) { -(keyHeight + 8.dp).roundToPx() }
    // Glyphs scale with key height so taller keys genuinely look bigger
    // (24sp at the 72dp default), not just more padded.
    val glyphSize = (keyHeight.value * 0.34f).sp
    val hintSize = (keyHeight.value * 0.15f).sp

    val label = when (key.type) {
        KeyTypes.SHIFT -> if (ui.capsLock) "⇪" else if (ui.shiftVisual) "⬆" else "⇧"
        KeyTypes.BACKSPACE -> "⌫"
        KeyTypes.ENTER -> "⏎"
        KeyTypes.SPACE -> "◄  ${ui.spaceLabel}  ►"
        else -> if (ui.uppercase) key.label.uppercase() else key.label
    }

    val gestures = when (key.type) {
        KeyTypes.CHAR -> Modifier.pointerInput(key, ui.uppercase) {
            detectTapGestures(
                onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                onTap = { onAction(KeyAction.Text(key.outputText(ui.uppercase))) },
                onLongPress = {
                    if (key.popup.isNotEmpty()) popupOpen = true
                    else onAction(KeyAction.Text(key.outputText(ui.uppercase)))
                },
            )
        }
        KeyTypes.BACKSPACE -> Modifier.pointerInput(Unit) {
            detectTapGestures(onPress = {
                pressed = true
                onAction(KeyAction.Delete)
                val job: Job = scope.launch {
                    delay(REPEAT_INITIAL_MS)
                    while (true) {
                        onAction(KeyAction.Delete)
                        delay(REPEAT_INTERVAL_MS)
                    }
                }
                tryAwaitRelease()
                job.cancel()
                pressed = false
            })
        }
        KeyTypes.SPACE -> Modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap = { onAction(KeyAction.Space) },
                )
            }
            .pointerInput(Unit) {
                var dragged = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dragged = 0f },
                    onHorizontalDrag = { _, delta -> dragged += delta },
                    onDragEnd = {
                        if (abs(dragged) > 64.dp.toPx()) onAction(KeyAction.CycleLanguage)
                    },
                )
            }
        KeyTypes.SHIFT -> Modifier.pointerInput(Unit) {
            detectTapGestures(
                onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                onTap = { onAction(KeyAction.Shift) },
            )
        }
        KeyTypes.ENTER -> Modifier.pointerInput(Unit) {
            detectTapGestures(
                onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                onTap = { onAction(KeyAction.Enter) },
            )
        }
        KeyTypes.LAYER_SWITCH -> Modifier.pointerInput(key) {
            detectTapGestures(
                onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                onTap = { key.target?.let { onAction(KeyAction.LayerSwitch(it)) } },
            )
        }
        KeyTypes.TAB -> Modifier.pointerInput(Unit) {
            detectTapGestures(
                onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                onTap = { onAction(KeyAction.Text("\t")) },
            )
        }
        else -> Modifier
    }

    Box(
        modifier = modifier
            .fillMaxHeight() // fill the row — without this, caps wrap their text and leave row gaps
            .padding(horizontal = 1.5.dp, vertical = 2.dp)
            .background(
                when {
                    pressed -> theme.keyPressed
                    isModifierKey && key.type != KeyTypes.SPACE -> theme.keyModifier
                    else -> theme.key
                },
                RoundedCornerShape(7.dp),
            )
            .then(gestures),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (key.type == KeyTypes.SHIFT && ui.shiftVisual) theme.accent else theme.label,
            fontSize = if (key.type == KeyTypes.SPACE) 14.sp else glyphSize,
            textAlign = TextAlign.Center,
        )
        // Long-press hint: first popup char, top-right corner (matches the original's superscripts).
        if (key.popup.isNotEmpty()) {
            Text(
                text = key.popup.first(),
                color = theme.hint,
                fontSize = hintSize,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 5.dp),
            )
        }

        if (popupOpen) {
            Popup(
                offset = IntOffset(0, popupYOffset),
                onDismissRequest = { popupOpen = false },
            ) {
                Row(
                    Modifier
                        .background(theme.popupBg, RoundedCornerShape(8.dp))
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                ) {
                    key.popup.forEach { alt ->
                        Text(
                            text = alt,
                            color = theme.label,
                            fontSize = 22.sp,
                            modifier = Modifier
                                .pointerInput(alt) {
                                    detectTapGestures(onTap = {
                                        onAction(KeyAction.Text(alt))
                                        popupOpen = false
                                    })
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}
