package com.kinetic.keyboard.emoji

import java.io.File

/**
 * P5.5: recently used emoji, most-recent first, file-backed in app-private storage
 * (same pattern as UserDictionary — see PRIVACY.md for the data-handling statement).
 */
class EmojiRecents(private val file: File, private val max: Int = 40) {

    private val items = ArrayDeque<String>()

    @Synchronized
    fun load(): List<String> {
        if (file.isFile) {
            items.clear()
            file.readLines().map(String::trim).filter(String::isNotEmpty)
                .take(max).forEach(items::addLast)
        }
        return items.toList()
    }

    /** The current in-memory list, most-recent first. */
    @Synchronized
    fun current(): List<String> = items.toList()

    /** Move [emoji] to the front and return the new list (for the UI state flow). */
    @Synchronized
    fun record(emoji: String): List<String> {
        items.remove(emoji)
        items.addFirst(emoji)
        while (items.size > max) items.removeLast()
        return items.toList()
    }

    @Synchronized
    fun save() {
        file.writeText(items.joinToString("\n"))
    }
}
