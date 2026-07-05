package com.kinetic.keyboard.suggest

import java.io.BufferedReader

/**
 * Word-frequency dictionary with prefix lookup (SPEC.md P4.1/P4.3).
 *
 * Storage is two parallel arrays sorted lexicographically — compact, allocation-free lookups, and
 * a binary search finds the prefix range; a small top-k selection scans it. 30k Bangla words scan
 * in well under a millisecond; a serialized trie/FST is a later optimization if ever needed.
 *
 * Asset format: `word<TAB>frequency` lines, sorted by word (see assets/dict/, built from
 * hermitdave/FrequencyWords 2018 OpenSubtitles lists, CC-BY-SA — provenance per SPEC §7).
 */
class Dictionary private constructor(
    private val words: Array<String>,
    private val freqs: IntArray,
) {
    val size: Int get() = words.size

    /** Distinct characters used by this dictionary's words (drives ED1 candidate generation). */
    val alphabet: CharArray by lazy {
        words.asSequence().flatMap { it.asSequence() }.distinct().toList().toCharArray()
    }

    fun frequencyOf(word: String): Int {
        val i = words.binarySearch(word)
        return if (i >= 0) freqs[i] else 0
    }

    /** Top [limit] words starting with [prefix], most frequent first. Empty prefix → empty. */
    fun byPrefix(prefix: String, limit: Int): List<String> {
        if (prefix.isEmpty() || words.isEmpty()) return emptyList()
        var lo = words.binarySearch(prefix)
        if (lo < 0) lo = -(lo + 1)

        // Collect the best `limit` by frequency with a simple selection over the prefix range.
        val bestIdx = IntArray(limit) { -1 }
        var i = lo
        while (i < words.size && words[i].startsWith(prefix)) {
            var candidate = i
            for (s in 0 until limit) {
                val cur = bestIdx[s]
                if (cur == -1) {
                    bestIdx[s] = candidate; break
                }
                if (freqs[candidate] > freqs[cur]) {
                    bestIdx[s] = candidate; candidate = cur
                }
            }
            i++
        }
        return bestIdx.filter { it >= 0 }.map { words[it] }
    }

    companion object {
        /** Parses the TSV format; input must be pre-sorted by word. */
        fun load(reader: BufferedReader): Dictionary {
            val w = ArrayList<String>(32_000)
            val f = ArrayList<Int>(32_000)
            reader.forEachLine { line ->
                val tab = line.indexOf('\t')
                if (tab > 0) {
                    w.add(line.substring(0, tab))
                    f.add(line.substring(tab + 1).trim().toIntOrNull() ?: 0)
                }
            }
            return Dictionary(w.toTypedArray(), f.toIntArray())
        }

        fun empty(): Dictionary = Dictionary(emptyArray(), IntArray(0))
    }
}

private fun Array<String>.binarySearch(key: String): Int {
    var low = 0
    var high = size - 1
    while (low <= high) {
        val mid = (low + high) ushr 1
        val cmp = this[mid].compareTo(key)
        when {
            cmp < 0 -> low = mid + 1
            cmp > 0 -> high = mid - 1
            else -> return mid
        }
    }
    return -(low + 1)
}
