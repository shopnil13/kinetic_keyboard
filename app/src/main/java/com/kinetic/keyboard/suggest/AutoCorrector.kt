package com.kinetic.keyboard.suggest

/**
 * Conservative autocorrect (SPEC.md P4.7).
 *
 * Candidates are all dictionary words within edit distance 1 of the typed word, found by
 * *generating* the ED1 neighborhood (deletes, transpositions, substitutions, insertions over the
 * dictionary's own alphabet) and probing the dictionary — a few hundred O(log n) lookups, no scan.
 *
 * Policy (deliberately timid; Bengali users hate aggressive autocorrect):
 *  - never touches a word that exists in the dictionary;
 *  - only corrects words of length >= 3;
 *  - requires the best candidate to be common ([MIN_FREQ]) and clearly better than the runner-up.
 */
class AutoCorrector(private val dictionary: Dictionary) {

    private val alphabet: CharArray get() = dictionary.alphabet

    /** Dictionary words at edit distance 1, most frequent first. */
    fun candidates(word: String, limit: Int = 3): List<String> {
        if (word.isEmpty()) return emptyList()
        val seen = HashMap<String, Int>() // word -> freq
        fun probe(w: String) {
            if (w != word && w !in seen) {
                val f = dictionary.frequencyOf(w)
                if (f > 0) seen[w] = f
            }
        }

        val sb = StringBuilder(word)
        // deletions
        for (i in word.indices) probe(StringBuilder(word).deleteCharAt(i).toString())
        // transpositions
        for (i in 0 until word.length - 1) {
            sb.setLength(0); sb.append(word)
            val t = sb[i]; sb[i] = sb[i + 1]; sb[i + 1] = t
            probe(sb.toString())
        }
        // substitutions
        for (i in word.indices) for (c in alphabet) {
            if (c == word[i]) continue
            sb.setLength(0); sb.append(word); sb[i] = c
            probe(sb.toString())
        }
        // insertions
        for (i in 0..word.length) for (c in alphabet) {
            sb.setLength(0); sb.append(word); sb.insert(i, c)
            probe(sb.toString())
        }

        return seen.entries.sortedByDescending { it.value }.take(limit).map { it.key }
    }

    /** The correction to apply on word commit, or null to leave the word alone. */
    fun correct(word: String): String? {
        if (word.length < MIN_LENGTH) return null
        if (dictionary.frequencyOf(word) > 0) return null // real word — hands off
        val ranked = candidates(word, 2)
        val best = ranked.firstOrNull() ?: return null
        val bestFreq = dictionary.frequencyOf(best)
        if (bestFreq < MIN_FREQ) return null
        val second = ranked.getOrNull(1)?.let { dictionary.frequencyOf(it) } ?: 0
        return if (second == 0 || bestFreq >= second * DOMINANCE) best else null
    }

    companion object {
        private const val MIN_LENGTH = 3
        private const val MIN_FREQ = 500
        private const val DOMINANCE = 3
    }
}
