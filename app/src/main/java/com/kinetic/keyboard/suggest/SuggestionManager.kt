package com.kinetic.keyboard.suggest

/**
 * Merges candidate sources into one ranked strip list (SPEC.md P4.9, current sources: user
 * dictionary > corpus dictionary; autocorrect and n-gram prediction are P4.6/P4.7).
 */
class SuggestionManager(
    @Volatile var bangla: Dictionary = Dictionary.empty(),
    @Volatile var english: Dictionary = Dictionary.empty(),
    val userDict: UserDictionary? = null,
) {
    /** Ranked suggestions for [prefix]; [useBangla] picks the dictionary. */
    fun suggest(prefix: String, useBangla: Boolean, limit: Int = MAX_SUGGESTIONS): List<String> {
        if (prefix.isEmpty()) return emptyList()
        val dict = if (useBangla) bangla else english
        val merged = LinkedHashSet<String>()
        userDict?.byPrefix(prefix, limit)?.forEach { merged.add(it) }
        dict.byPrefix(prefix, limit).forEach { merged.add(it) }
        return merged.take(limit).toList()
    }

    companion object {
        const val MAX_SUGGESTIONS = 3

        /** Trailing letters+combining-marks run before the cursor = the word being typed. */
        fun currentWord(beforeCursor: CharSequence): String {
            var i = beforeCursor.length
            while (i > 0) {
                val cp = Character.codePointBefore(beforeCursor, i)
                val type = Character.getType(cp)
                val isWordChar = Character.isLetter(cp) ||
                    type == Character.NON_SPACING_MARK.toInt() ||       // ি ু ঁ …
                    type == Character.COMBINING_SPACING_MARK.toInt() || // া ে ো …
                    cp == 0x09CD || cp == 0x200C || cp == 0x200D
                if (isWordChar) i -= Character.charCount(cp) else break
            }
            return beforeCursor.subSequence(i, beforeCursor.length).toString()
        }
    }
}
