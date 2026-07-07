package com.kinetic.keyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.emoji2.emojipicker.EmojiPickerView
import androidx.emoji2.emojipicker.RecentEmojiProvider
import com.kinetic.keyboard.ui.theme.KbTheme

/**
 * P5.5 rev2: emoji panel built on the AndroidX EmojiPickerView — categories, recents, and
 * built-in skin-tone variant popups, rendered through EmojiCompat's bundled font so every
 * emoji draws correctly regardless of the device's system font. The shared Gboard-style
 * tab bar (ABC · 😊 · GIF · sticker · ⌫) sits below it (P5.10).
 */
@Composable
fun EmojiPanel(
    recentEmojiProvider: RecentEmojiProvider,
    theme: KbTheme,
    height: Dp,
    onEmoji: (String) -> Unit,
    onTab: (PanelMode) -> Unit,
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
        MediaTabBar(
            active = PanelMode.EMOJI,
            theme = theme,
            onTab = onTab,
            onBack = onBack,
            onDelete = onDelete,
        )
    }
}
