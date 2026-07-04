package com.kinetic.keyboard.text

/**
 * Bengali-aware grapheme-cluster utilities (SPEC.md §3, P2.1).
 *
 * Implemented as a purpose-built backward scanner rather than ICU BreakIterator:
 *  - pure JVM → unit-testable without an Android runtime;
 *  - deterministic across OS versions;
 *  - tuned for *typing*: a whole conjunct chain (ক্ষ্ম) is one deletion unit, matching the
 *    original Ridmik behavior and SPEC P2.2.
 * Non-Bengali text falls back to one code point per press (surrogate-safe). Full UAX#29 emoji
 * segmentation is out of scope for now (tracked as a P5 refinement).
 */
object BengaliText {

    private const val ZWNJ = 0x200C
    private const val ZWJ = 0x200D

    fun isBengali(cp: Int) = cp in 0x0980..0x09FF

    /** Dependent signs that extend a cluster: vowel signs, candrabindu, anusvara, visarga, nukta, hasanta, length marks. */
    fun isDependent(cp: Int) = when (cp) {
        0x0981, 0x0982, 0x0983, // ঁ ং ঃ
        0x09BC,                  // nukta
        in 0x09BE..0x09C4,       // া ি ী ু ূ ৃ ৄ
        0x09C7, 0x09C8,          // ে ৈ
        0x09CB, 0x09CC,          // ো ৌ
        0x09CD,                  // ্ hasanta
        0x09D7,                  // au length mark
        0x09E2, 0x09E3,          // vocalic marks
        -> true
        else -> false
    }

    fun isHasanta(cp: Int) = cp == 0x09CD
    private fun isNukta(cp: Int) = cp == 0x09BC
    private fun isZw(cp: Int) = cp == ZWNJ || cp == ZWJ

    /** Cluster-forming bases: independent vowels + consonants (incl. ড়ঢ়য়, khanda-ta, ৰৱ). */
    fun isBase(cp: Int) = when (cp) {
        in 0x0985..0x0994,       // অ..ঔ
        in 0x0995..0x09B9,       // ক..হ (block gaps are unassigned; harmless)
        0x09CE,                  // ৎ
        in 0x09DC..0x09DF,       // ড় ঢ় য়
        0x09E0, 0x09E1,          // ৠ ৡ
        0x09F0, 0x09F1,          // ৰ ৱ
        -> true
        else -> false
    }

    /** Skip ZWJ/ZWNJ backwards from [i]; returns new index. */
    private fun skipZw(text: CharSequence, start: Int): Int {
        var i = start
        while (i > 0) {
            val cp = Character.codePointBefore(text, i)
            if (isZw(cp)) i -= Character.charCount(cp) else break
        }
        return i
    }

    /**
     * Length in UTF-16 units of the last grapheme cluster of [text] — i.e. how many chars one
     * backspace press should remove. 0 for empty input.
     */
    fun lastClusterLength(text: CharSequence): Int {
        if (text.isEmpty()) return 0
        val end = text.length
        var i = skipZw(text, end)
        if (i == 0) return end // only joiners

        var cp = Character.codePointBefore(text, i)

        // Non-Bengali tail: one code point (plus any trailing joiners already skipped).
        if (!isBengali(cp)) return end - (i - Character.charCount(cp))

        var sawBase = false
        if (isDependent(cp)) {
            // Consume the run of dependent signs (joiners may sit between them).
            while (i > 0) {
                cp = Character.codePointBefore(text, i)
                if (isDependent(cp)) {
                    i -= Character.charCount(cp)
                    i = skipZw(text, i)
                } else break
            }
            // Then the base they attach to, if any.
            if (i > 0) {
                cp = Character.codePointBefore(text, i)
                if (isBase(cp)) {
                    i -= Character.charCount(cp)
                    sawBase = true
                }
            }
        } else {
            // Base letter, Bengali digit, ৳, etc. — consume one code point.
            i -= Character.charCount(cp)
            sawBase = isBase(cp)
        }

        // Absorb the conjunct chain behind a base: … base [nukta] hasanta [ZW] | base
        while (sawBase && i > 0) {
            var j = skipZw(text, i)
            if (j == 0) break
            var c = Character.codePointBefore(text, j)
            if (!isHasanta(c)) break
            j -= Character.charCount(c)
            if (j > 0) {
                c = Character.codePointBefore(text, j)
                if (isNukta(c)) {
                    j -= Character.charCount(c)
                    c = if (j > 0) Character.codePointBefore(text, j) else -1
                }
                if (c != -1 && isBase(c)) j -= Character.charCount(c)
            }
            i = j
        }
        return end - i
    }
}
