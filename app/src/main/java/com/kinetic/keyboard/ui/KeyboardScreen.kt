package com.kinetic.keyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.emoji2.emojipicker.RecentEmojiProvider
import com.kinetic.keyboard.R
import com.kinetic.keyboard.giphy.GiphyItem
import com.kinetic.keyboard.engine.KeyboardUiState
import com.kinetic.keyboard.engine.model.KeyDef
import com.kinetic.keyboard.engine.model.KeyTypes
import com.kinetic.keyboard.ui.theme.KbTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Key-cap glyphs render in Tiro Bangla so Bengali conjuncts/matras shape correctly. */
private val KeyLabelFontFamily = FontFamily(Font(R.font.tiro_bangla))

/** Bengali block (U+0980–U+09FF): Latin letters/digits/symbols keep the system default font. */
private fun String.hasBengali(): Boolean = any { it.code in 0x0980..0x09FF }

private fun keyLabelFontFor(text: String): FontFamily? =
    if (text.hasBengali()) KeyLabelFontFamily else null

/** Bengali dependent vowel signs and other combining marks — shown alone (matra keys) they have
 *  no base glyph to attach to, so the shaper draws a dotted-circle placeholder. */
private val BengaliCombiningMarks = setOf(
    'া', 'ি', 'ী', 'ু', 'ূ', 'ৃ', 'ৄ',
    'ে', 'ৈ', 'ো', 'ৌ', 'ৗ',
    'ঁ', 'ং', 'ঃ', '্',
)

/** Display-only fix: prefix a non-breaking space so the mark has a base to attach to instead of
 *  triggering the dotted-circle fallback. Checks the first non-ZWJ character, since phala keys
 *  (ra-phala ‍্র, ya-phala ‍্য) lead with a ZWJ that — being zero-width — is NOT a valid
 *  base and still leaves the following হসন্ত circled. Never touches what gets committed —
 *  callers keep using [KeyDef.outputText]/[KeyDef.popup] for that, which read the model's raw,
 *  un-prefixed label/output. */
private fun bengaliDisplayLabel(text: String): String {
    val base = text.firstOrNull { it != '‍' } ?: return text
    return if (base in BengaliCombiningMarks) " $text" else text
}

/** Actions emitted by the keyboard UI, handled by the IME service (P1.13). */
sealed interface KeyAction {
    data class Text(val text: String) : KeyAction
    data object Delete : KeyAction
    data object Enter : KeyAction
    data object Space : KeyAction
    data object Shift : KeyAction
    data class LayerSwitch(val target: String) : KeyAction
    data object CycleLanguage : KeyAction

    /** Long-press on space: open the system input-method picker (switch keyboards). */
    data object ShowImePicker : KeyAction

    /** P5.5: open/close the emoji panel. */
    data object ToggleEmoji : KeyAction

    /** P5.5: commit an emoji chosen from the panel. */
    data class EmojiInput(val emoji: String) : KeyAction
}

private const val REPEAT_INITIAL_MS = 350L
private const val REPEAT_INTERVAL_MS = 50L

/** Popup cell width — the slide-to-select math and the rendered cells must agree on this. */
private val POPUP_CELL = 44.dp

@Composable
fun KeyboardScreen(
    ui: KeyboardUiState,
    suggestions: List<String>,
    theme: KbTheme,
    keyHeight: Dp,
    longPressMs: Int,
    panelMode: PanelMode,
    media: MediaUiState,
    recentEmojiProvider: RecentEmojiProvider,
    onAction: (KeyAction) -> Unit,
    onSuggestion: (String) -> Unit,
    onMediaTab: (PanelMode) -> Unit,
    onMediaSearchOpen: () -> Unit,
    onMediaPick: (GiphyItem) -> Unit,
) {
    // detectTapGestures takes its long-press timeout from LocalViewConfiguration —
    // wrapping it lets the user's "hold delay" setting apply to every key at once.
    val base = LocalViewConfiguration.current
    val tuned = remember(base, longPressMs) {
        object : ViewConfiguration by base {
            override val longPressTimeoutMillis: Long = longPressMs.toLong()
        }
    }
    CompositionLocalProvider(LocalViewConfiguration provides tuned) {
        Surface(color = theme.background) {
            // targetSdk 35 makes the IME window edge-to-edge: without this padding the bottom
            // row sits under the system nav bar and its keyboard-dismiss chevron covers ?123.
            Column(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 1.dp, vertical = 3.dp),
            ) {
                // Panels replace strip + key rows at the same total height (no window jump).
                val panelHeight = keyHeight * ui.layout.rows.size + 46.dp
                val mediaMode = panelMode == PanelMode.GIF || panelMode == PanelMode.STICKER
                when {
                    panelMode == PanelMode.EMOJI -> EmojiPanel(
                        recentEmojiProvider = recentEmojiProvider,
                        theme = theme,
                        height = panelHeight,
                        onEmoji = { onAction(KeyAction.EmojiInput(it)) },
                        onTab = onMediaTab,
                        onBack = { onMediaTab(PanelMode.NONE) },
                        onDelete = { onAction(KeyAction.Delete) },
                    )
                    mediaMode && !media.searchActive -> MediaPanel(
                        mode = panelMode,
                        state = media,
                        theme = theme,
                        height = panelHeight,
                        onSearchTap = onMediaSearchOpen,
                        onTab = onMediaTab,
                        onBack = { onMediaTab(PanelMode.NONE) },
                        onDelete = { onAction(KeyAction.Delete) },
                        onPick = onMediaPick,
                    )
                    else -> {
                        if (mediaMode) {
                            // P5.10: typing a GIPHY query on the normal keys; ⏎ submits.
                            MediaSearchBar(
                                query = media.query,
                                theme = theme,
                                onSubmit = { onAction(KeyAction.Enter) },
                            )
                        } else {
                            SuggestionStrip(
                                suggestions = suggestions,
                                theme = theme,
                                onSuggestion = onSuggestion,
                                onPunctuation = { onAction(KeyAction.Text(it)) },
                            )
                        }
                        ui.layout.rows.forEach { row ->
                            Row(Modifier.fillMaxWidth().height(keyHeight)) {
                                var used = 0f
                                row.keys.forEachIndexed { index, key ->
                                    if (key.gap > 0f) {
                                        Spacer(Modifier.weight(key.gap))
                                        used += key.gap
                                    }
                                    val w = key.widthOrDefault()
                                    KeyView(
                                        key = key,
                                        ui = ui,
                                        theme = theme,
                                        keyHeight = keyHeight,
                                        onAction = onAction,
                                        // Last key in the row butts against the screen edge — its
                                        // popup has no room to expand rightward (P1.11 fix-1).
                                        popupExpandsLeft = index == row.keys.lastIndex,
                                        modifier = Modifier.weight(w),
                                    )
                                    used += w
                                }
                                if (used < 99.5f) Spacer(Modifier.weight(100f - used))
                            }
                        }
                    }
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
    popupExpandsLeft: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var pressed by remember { mutableStateOf(false) }
    var popupOpen by remember { mutableStateOf(false) }
    var selectedAlt by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val isModifierKey = key.type != KeyTypes.CHAR
    val popupYOffset = with(LocalDensity.current) { -(keyHeight + 8.dp).roundToPx() }
    // Glyphs scale with key height so taller keys genuinely look bigger
    // (29sp at the 72dp default), not just more padded.
    val glyphSize = (keyHeight.value * 0.40f).sp
    val hintSize = (keyHeight.value * 0.24f).sp

    val label = when (key.type) {
        KeyTypes.SHIFT -> if (ui.capsLock) "⇪" else if (ui.shiftVisual) "⬆" else "⇧"
        KeyTypes.BACKSPACE -> "⌫"
        KeyTypes.ENTER -> "⏎"
        KeyTypes.SPACE -> "◄  ${ui.spaceLabel}  ►"
        else -> bengaliDisplayLabel(if (ui.uppercase) key.label.uppercase() else key.label)
    }

    // P6.2: what a tap does, exposed to TalkBack as the click action — the raw pointerInput
    // gestures below carry no semantics of their own.
    val primaryTap: (() -> Unit)? = when (key.type) {
        KeyTypes.CHAR -> {
            { onAction(KeyAction.Text(key.outputText(ui.uppercase))) }
        }
        KeyTypes.BACKSPACE -> {
            { onAction(KeyAction.Delete) }
        }
        KeyTypes.SPACE -> {
            { onAction(KeyAction.Space) }
        }
        KeyTypes.SHIFT -> {
            { onAction(KeyAction.Shift) }
        }
        KeyTypes.ENTER -> {
            { onAction(KeyAction.Enter) }
        }
        KeyTypes.LAYER_SWITCH -> key.target?.let { t -> { onAction(KeyAction.LayerSwitch(t)) } }
        KeyTypes.TAB -> {
            { onAction(KeyAction.Text("\t")) }
        }
        KeyTypes.EMOJI -> {
            { onAction(KeyAction.ToggleEmoji) }
        }
        else -> null
    }

    // P6.2: glyph labels (⇧ ⌫ ⏎) are meaningless to a screen reader — spell them out.
    val a11yLabel = when (key.type) {
        KeyTypes.SHIFT -> when {
            ui.capsLock -> "Caps lock on"
            ui.shiftVisual -> "Shift on"
            else -> "Shift"
        }
        KeyTypes.BACKSPACE -> "Backspace"
        KeyTypes.ENTER -> "Enter"
        KeyTypes.SPACE -> "Space, language ${ui.spaceLabel}. Hold to switch keyboard"
        KeyTypes.TAB -> "Tab"
        KeyTypes.EMOJI -> "Emoji"
        else -> null
    }

    val gestures = when (key.type) {
        // P1.11 (slide-to-select): tap commits the key; holding opens the popup with the FIRST
        // alternative preselected, sliding sideways moves the selection, release commits it —
        // the finger never has to lift, matching Gboard/the original Ridmik feel.
        KeyTypes.CHAR -> Modifier.pointerInput(key, ui.uppercase) {
            awaitEachGesture {
                val down = awaitFirstDown()
                down.consume()
                pressed = true
                // Wait for release or the long-press timeout, whichever comes first.
                // true = released (tap) · false = pointer lost · null = still held (long press).
                // NB: changedToUp() is false on a consumed change — always check BEFORE consume().
                val released = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                            ?: return@withTimeoutOrNull false
                        val up = change.changedToUp()
                        change.consume()
                        if (up) return@withTimeoutOrNull true
                    }
                    @Suppress("UNREACHABLE_CODE") false
                }
                when {
                    released == true -> onAction(KeyAction.Text(key.outputText(ui.uppercase)))
                    released == null && key.popup.isEmpty() -> {
                        // Long hold with no alternatives: commit the key itself (as before).
                        onAction(KeyAction.Text(key.outputText(ui.uppercase)))
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            val up = change.changedToUp()
                            change.consume()
                            if (up) break
                        }
                    }
                    released == null -> {
                        selectedAlt = 0
                        popupOpen = true
                        val cell = POPUP_CELL.toPx()
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            val up = change.changedToUp()
                            change.consume()
                            if (up) {
                                onAction(KeyAction.Text(key.popup[selectedAlt]))
                                break
                            }
                            // Cells normally start at the key's left edge and extend rightward.
                            // The last key in a row has no room to its right (screen edge), so its
                            // popup instead anchors at the key's right edge and extends leftward —
                            // mirror the sign so a left-swipe still advances the selection.
                            val raw = if (popupExpandsLeft) -change.position.x else change.position.x
                            selectedAlt = (raw / cell).toInt().coerceIn(0, key.popup.lastIndex)
                        }
                    }
                }
                pressed = false
                popupOpen = false
            }
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
                    onLongPress = { onAction(KeyAction.ShowImePicker) },
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
        KeyTypes.EMOJI -> Modifier.pointerInput(Unit) {
            detectTapGestures(
                onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                onTap = { onAction(KeyAction.ToggleEmoji) },
            )
        }
        else -> Modifier
    }

    Box(
        modifier = modifier
            .fillMaxHeight() // fill the row — without this, caps wrap their text and leave row gaps
            .padding(horizontal = 1.5.dp, vertical = 2.dp)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                a11yLabel?.let { contentDescription = it }
                primaryTap?.let { tap -> onClick { tap(); true } }
            }
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
        when (key.type) {
            KeyTypes.EMOJI -> Icon(
                painter = painterResource(R.drawable.ic_emoji),
                contentDescription = null,
                tint = theme.label,
                modifier = Modifier.size((keyHeight.value * 0.42f).dp),
            )
            // Wide-viewport vector instead of the ⇧/⬆/⇪ glyphs: those read as too thin/narrow at
            // any font size, and scaling text non-uniformly fights Compose's font-padding metrics
            // (glyph ends up visibly off-center). A dedicated shape gives exact width and centering.
            KeyTypes.SHIFT -> Icon(
                painter = painterResource(R.drawable.ic_shift_arrow),
                contentDescription = null,
                tint = if (ui.shiftVisual || ui.capsLock) theme.accent else theme.label,
                modifier = Modifier
                    .width((keyHeight.value * 0.48f).dp)
                    .height((keyHeight.value * 0.30f).dp),
            )
            else -> Text(
                text = label,
                color = theme.label,
                fontSize = if (key.type == KeyTypes.SPACE) 16.sp else glyphSize,
                fontFamily = keyLabelFontFor(label),
                textAlign = TextAlign.Center,
            )
        }
        // Long-press hint: first popup char, top-right corner (matches the original's superscripts).
        if (key.popup.isNotEmpty()) {
            val hint = key.popup.first()
            Text(
                text = bengaliDisplayLabel(hint),
                color = theme.hint,
                fontSize = hintSize,
                fontFamily = keyLabelFontFor(hint),
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 5.dp),
            )
        }

        if (popupOpen) {
            // Display-only: the still-held finger drives selection (slide) and commit (release).
            // Normally the popup anchors at the key's left edge and cells extend rightward; a
            // right-edge key instead anchors at its right edge, extending leftward, with cell
            // order reversed so index 0 still sits directly over the key (fix-1).
            val entries = key.popup.withIndex().let { if (popupExpandsLeft) it.reversed() else it }
            Popup(
                alignment = if (popupExpandsLeft) Alignment.TopEnd else Alignment.TopStart,
                offset = IntOffset(0, popupYOffset),
                onDismissRequest = { popupOpen = false },
            ) {
                Row(
                    Modifier
                        .background(theme.popupBg, RoundedCornerShape(8.dp))
                        .padding(vertical = 6.dp),
                ) {
                    entries.forEach { (index, alt) ->
                        Box(
                            Modifier
                                .width(POPUP_CELL)
                                .background(
                                    if (index == selectedAlt) theme.keyPressed else theme.popupBg,
                                    RoundedCornerShape(6.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = bengaliDisplayLabel(alt),
                                color = if (index == selectedAlt) theme.accent else theme.label,
                                fontSize = 32.sp,
                                fontFamily = keyLabelFontFor(alt),
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
