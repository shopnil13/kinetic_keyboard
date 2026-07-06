package com.kinetic.keyboard.emoji

import java.io.BufferedReader

/** One emoji tab: [icon] is the glyph on the category tab. */
data class EmojiCategory(val id: String, val icon: String, val emoji: List<String>)

/**
 * P5.5: the built-in emoji library, loaded from assets/emoji/emoji.tsv (generated file — see
 * header comment inside it). Format: `@id<TAB>icon` opens a category; following lines are
 * space-separated emoji (ZWJ sequences contain no spaces, so the split is safe).
 */
object EmojiCatalog {

    fun parse(reader: BufferedReader): List<EmojiCategory> {
        val categories = mutableListOf<EmojiCategory>()
        var id: String? = null
        var icon = ""
        var items = mutableListOf<String>()
        fun flush() = id?.let { categories += EmojiCategory(it, icon, items.toList()) }
        reader.forEachLine { raw ->
            val line = raw.trim()
            when {
                line.isEmpty() || line.startsWith("#") -> {}
                line.startsWith("@") -> {
                    flush()
                    val parts = line.removePrefix("@").split("\t")
                    id = parts[0]
                    icon = parts.getOrElse(1) { "•" }
                    items = mutableListOf()
                }
                else -> items += line.split(' ').filter { it.isNotBlank() }
            }
        }
        flush()
        return categories
    }

    /**
     * Drop emoji the device font can't draw (older Android = older emoji font = tofu boxes).
     * [canRender] is `Paint::hasGlyph` at runtime; injected so this stays JVM-testable.
     */
    fun filterRenderable(
        categories: List<EmojiCategory>,
        canRender: (String) -> Boolean,
    ): List<EmojiCategory> =
        categories.map { it.copy(emoji = it.emoji.filter(canRender)) }
            .filter { it.emoji.isNotEmpty() }
}
