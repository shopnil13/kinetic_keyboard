package com.kinetic.keyboard.suggest

import java.io.File

/**
 * Next-word prediction learned from the user's own typing (SPEC.md P4.6).
 *
 * No licensed Bengali sentence corpus is available to ship static n-grams, so — like the original
 * keyboard's UserBigramDictionary — bigrams accumulate on-device: every "prev next" pair the user
 * commits bumps a count, and after a space the strip offers the words that usually follow.
 * Fully offline; the store is a small TSV in filesDir.
 */
class UserBigrams(private val store: File) {

    private val counts = HashMap<String, HashMap<String, Int>>() // prev -> (next -> count)
    @Volatile private var dirty = false

    fun load() {
        if (!store.exists()) return
        store.forEachLine { line ->
            val parts = line.split('\t')
            if (parts.size == 3) {
                val c = parts[2].toIntOrNull() ?: return@forEachLine
                counts.getOrPut(parts[0]) { HashMap() }[parts[1]] = c
            }
        }
    }

    fun learn(prev: String, next: String) {
        if (prev.length < 2 || next.length < 2) return
        val m = counts.getOrPut(prev) { HashMap() }
        m[next] = (m[next] ?: 0) + 1
        dirty = true
    }

    /** Most likely followers of [prev], best first. */
    fun predict(prev: String, limit: Int = 3): List<String> {
        val m = counts[prev] ?: return emptyList()
        return m.entries.sortedByDescending { it.value }.take(limit).map { it.key }
    }

    fun saveIfDirty() {
        if (!dirty) return
        dirty = false
        store.parentFile?.mkdirs()
        store.writeText(buildString {
            counts.forEach { (prev, m) ->
                m.forEach { (next, c) -> append(prev).append('\t').append(next).append('\t').append(c).append('\n') }
            }
        })
    }
}
