package com.kinetic.keyboard.suggest

import java.io.File

/**
 * Learns the user's words (SPEC.md P4.8 — file-backed for now; Room migration is a listed
 * refinement). Words the user actually commits rank above corpus words next time.
 */
class UserDictionary(private val store: File) {

    private val counts = HashMap<String, Int>()
    @Volatile private var dirty = false

    fun load() {
        if (!store.exists()) return
        store.forEachLine { line ->
            val tab = line.indexOf('\t')
            if (tab > 0) {
                counts[line.substring(0, tab)] = line.substring(tab + 1).toIntOrNull() ?: 1
            }
        }
    }

    fun learn(word: String) {
        if (word.length < 2) return
        counts[word] = (counts[word] ?: 0) + 1
        dirty = true
    }

    /** User words starting with [prefix], most used first. */
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
        if (!dirty) return
        dirty = false
        store.parentFile?.mkdirs()
        store.writeText(buildString {
            counts.forEach { (w, c) -> append(w).append('\t').append(c).append('\n') }
        })
    }
}
