package com.kinetic.keyboard.emoji

import androidx.emoji2.emojipicker.RecentEmojiProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * P5.5 rev2: adapts our file-backed [EmojiRecents] to the AndroidX EmojiPickerView's provider
 * interface, keeping recents in app-private storage (PRIVACY.md) instead of the picker's
 * default SharedPreferences.
 */
class FileRecentEmojiProvider(
    private val store: EmojiRecents,
    private val scope: CoroutineScope,
) : RecentEmojiProvider {

    override suspend fun getRecentEmojiList(): List<String> = store.current()

    override fun recordSelection(emoji: String) {
        store.record(emoji)
        scope.launch(Dispatchers.IO) { store.save() }
    }
}
