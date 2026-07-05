package com.kinetic.keyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.kinetic.keyboard.engine.KeyboardUiState
import com.kinetic.keyboard.engine.model.KeyDef
import com.kinetic.keyboard.engine.model.KeyTypes
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

private object KbColors {
    val background = Color(0xFF141414)
    val key = Color(0xFF3B3B3B)
    val keyModifier = Color(0xFF262626)
    val keyPressed = Color(0xFF5A5A5A)
    val label = Color.White
    val hint = Color(0xFFB0B0B0)
    val popupBg = Color(0xFF4A4A4A)
    val accent = Color(0xFF4FC3F7)
}

private val KeyHeight = 72.dp
private const val REPEAT_INITIAL_MS = 350L
private const val REPEAT_INTERVAL_MS = 50L

@Composable
fun KeyboardScreen(
    ui: KeyboardUiState,
    suggestions: List<String>,
    onAction: (KeyAction) -> Unit,
    onSuggestion: (String) -> Unit,
) {
    Surface(color = KbColors.background) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 1.dp, vertical = 3.dp)) {
            SuggestionStrip(
                suggestions = suggestions,
                onSuggestion = onSuggestion,
                onPunctuation = { onAction(KeyAction.Text(it)) },
            )
            ui.layout.rows.forEach { row ->
                Row(Modifier.fillMaxWidth().height(KeyHeight)) {
                    var used = 0f
                    row.keys.forEach { key ->
                        if (key.gap > 0f) {
                            Spacer(Modifier.weight(key.gap))
                            used += key.gap
                        }
                        val w = key.widthOrDefault()
                        KeyView(key, ui, onAction, Modifier.weight(w))
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
    onAction: (KeyAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pressed by remember { mutableStateOf(false) }
    var popupOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val isModifierKey = key.type != KeyTypes.CHAR
    val popupYOffset = with(LocalDensity.current) { -(KeyHeight + 8.dp).roundToPx() }

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
            .padding(horizontal = 1.5.dp, vertical = 2.dp)
            .background(
                when {
                    pressed -> KbColors.keyPressed
                    isModifierKey && key.type != KeyTypes.SPACE -> KbColors.keyModifier
                    else -> KbColors.key
                },
                RoundedCornerShape(7.dp),
            )
            .then(gestures),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (key.type == KeyTypes.SHIFT && ui.shiftVisual) KbColors.accent else KbColors.label,
            fontSize = if (key.type == KeyTypes.SPACE) 14.sp else 24.sp,
            textAlign = TextAlign.Center,
        )
        // Long-press hint: first popup char, top-right corner (matches the original's superscripts).
        if (key.popup.isNotEmpty()) {
            Text(
                text = key.popup.first(),
                color = KbColors.hint,
                fontSize = 10.sp,
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
                        .background(KbColors.popupBg, RoundedCornerShape(8.dp))
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                ) {
                    key.popup.forEach { alt ->
                        Text(
                            text = alt,
                            color = KbColors.label,
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
