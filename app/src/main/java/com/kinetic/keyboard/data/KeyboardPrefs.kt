package com.kinetic.keyboard.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kinetic.keyboard.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** User preferences (SPEC.md P5.1), shared by the IME service and the settings screen. */
data class KeyboardPrefs(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val keyHeightDp: Int = DEFAULT_KEY_HEIGHT,
    val haptics: Boolean = true,
    val sound: Boolean = false,
    /** How long a key must be held before the long-press popup opens. */
    val longPressMs: Int = DEFAULT_LONG_PRESS_MS,
) {
    companion object {
        const val DEFAULT_KEY_HEIGHT = 72
        const val MIN_KEY_HEIGHT = 52
        const val MAX_KEY_HEIGHT = 88
        const val DEFAULT_LONG_PRESS_MS = 400
        const val MIN_LONG_PRESS_MS = 150
        const val MAX_LONG_PRESS_MS = 800
    }
}

private val Context.store by preferencesDataStore(name = "keyboard_prefs")

class PrefsRepository(private val context: Context) {

    private object Keys {
        val theme = intPreferencesKey("theme_mode")
        val keyHeight = intPreferencesKey("key_height_dp")
        val haptics = booleanPreferencesKey("haptics")
        val sound = booleanPreferencesKey("sound")
        val longPress = intPreferencesKey("long_press_ms")
    }

    val prefs: Flow<KeyboardPrefs> = context.store.data.map { p ->
        KeyboardPrefs(
            themeMode = ThemeMode.entries.getOrElse(p[Keys.theme] ?: 0) { ThemeMode.SYSTEM },
            keyHeightDp = (p[Keys.keyHeight] ?: KeyboardPrefs.DEFAULT_KEY_HEIGHT)
                .coerceIn(KeyboardPrefs.MIN_KEY_HEIGHT, KeyboardPrefs.MAX_KEY_HEIGHT),
            haptics = p[Keys.haptics] ?: true,
            sound = p[Keys.sound] ?: false,
            longPressMs = (p[Keys.longPress] ?: KeyboardPrefs.DEFAULT_LONG_PRESS_MS)
                .coerceIn(KeyboardPrefs.MIN_LONG_PRESS_MS, KeyboardPrefs.MAX_LONG_PRESS_MS),
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = context.store.edit { it[Keys.theme] = mode.ordinal }
    suspend fun setKeyHeight(dp: Int) = context.store.edit { it[Keys.keyHeight] = dp }
    suspend fun setHaptics(on: Boolean) = context.store.edit { it[Keys.haptics] = on }
    suspend fun setSound(on: Boolean) = context.store.edit { it[Keys.sound] = on }
    suspend fun setLongPressMs(ms: Int) = context.store.edit { it[Keys.longPress] = ms }
}
