package com.kinetic.keyboard.ui

import android.content.res.Configuration
import android.view.ContextThemeWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
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
        // The picker is a classic View: its header/category colours come from the platform
        // theme of the Context it is built with, not from KbTheme. The IME's own theme is
        // Material.Light, which left the headers unreadable on the dark palette — hand it a
        // matching dark/light theme + night uiMode, and rebuild it when the palette flips.
        key(theme.isDark) {
            AndroidView(
                factory = { context ->
                    val night = if (theme.isDark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                    val config = Configuration(context.resources.configuration).apply {
                        uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
                    }
                    val themed = ContextThemeWrapper(
                        context.createConfigurationContext(config),
                        if (theme.isDark) android.R.style.Theme_Material else android.R.style.Theme_Material_Light,
                    )
                    EmojiPickerView(themed).apply {
                        emojiGridColumns = 9
                        setRecentEmojiProvider(recentEmojiProvider)
                        setOnEmojiPickedListener { onEmoji(it.emoji) }
                    }
                },
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        }
        MediaTabBar(
            active = PanelMode.EMOJI,
            theme = theme,
            onTab = onTab,
            onBack = onBack,
            onDelete = onDelete,
        )
    }
}
