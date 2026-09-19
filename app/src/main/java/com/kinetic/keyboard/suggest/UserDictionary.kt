package com.kinetic.keyboard.suggest

import java.io.File

/**
 * Learns the user's words (SPEC.md P4.8 — file-backed for now; Room migration is a listed
 * refinement). Words the user actually commits rank above corpus words next time.
 *
 * Thread model: [learn]/[byPrefix] run on the main thread while [load]/[saveIfDirty] run on IO,
 * so every access to [counts] is synchronized (same as PerAppLanguage / EmojiRecents). The
 * file write itself happens outside the lock from a snapshot, so typing never blocks on disk.
 */
class UserDictionary(private val store: File) {

    private val counts = HashMap<String, Int>()
    private var dirty = false

    @Synchronized
    fun load() {
        if (!store.exists()) return
        store.forEachLine { line ->
            val tab = line.indexOf('\t')
            if (tab > 0) {
                counts[line.substring(0, tab)] = line.substring(tab + 1).toIntOrNull() ?: 1
            }
        }
    }

    @Synchronized
    fun learn(word: String) {
        if (word.length < 2) return
        counts[word] = (counts[word] ?: 0) + 1
        dirty = true
    }

    /** User words starting with [prefix], most used first. */
    @Synchronized
    fun byPrefix(prefix: String, limit: Int): List<String> {
        if (prefix.isEmpty()) return emptyList()
        return counts.entries.asSequence()
            .filter { it.key.startsWith(prefix) }
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key }
            .toList()
    }

    /** Persist if changed. Call from a background thread at natural pauses (field exit). */
    fun saveIfDirty() {
        val snapshot = synchronized(this) {
            if (!dirty) return
            dirty = false
            buildString {
                counts.forEach { (w, c) -> append(w).append('\t').append(c).append('\n') }
            }
        }
        store.parentFile?.mkdirs()
        store.writeText(snapshot)
    }
}
