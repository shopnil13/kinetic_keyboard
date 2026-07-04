package com.kinetic.keyboard.input

/**
 * Banglish → Bangla phonetic transliterator, ported from the recovered reference
 * (reference/RidmikParser.java, decompiled; SPEC.md P3.2).
 *
 * DELIBERATELY LITERAL. The decompiled source is not directly compilable (boolean/int confusion,
 * lost loop structure), so this is a faithful reconstruction of its logic; the golden corpus in
 * RidmikParserTest locks the behavior (each expectation hand-traced through the original control
 * flow). Readability refactor is P3.6 — until then, keep the original variable roles:
 *
 *   v1  = previous char        v9 = char before that     v12 = char before that
 *   v5  = "conjunct just formed" flag                    v8  = saved v5
 *   v7  = "second char of a two-char consonant was consumed" flag
 *
 * Sliding-window rules of note:
 *   consonant+consonant auto-joins with ্ when the jkt tables allow (rokto → রক্ত);
 *   two-char consonants (kh gh ch …) replace their first char (kokhono → কখন);
 *   'o' after a consonant is the inherent vowel (silent); rr+consonant → reph (korrmo → কর্ম);
 *   rri → ঋ/ৃ; kkh → ক্ষ; word-initial x → এক্স; w is ব-phala inside a cluster.
 */
class RidmikParser {

    private val u = BanglaUnicodeTable()

    private fun isVowel(cp: Int): Boolean = cp > 0 && "AEIOUaeiou".indexOf(cp.toChar()) != -1

    private fun isConsonant(cp: Int): Boolean =
        cp > 0 && !isVowel(cp) && Character.isLetter(cp)

    private fun isCharInString(cp: Int, s: String): Boolean = s.indexOf(cp.toChar()) != -1

    /** Whether current char does NOT auto-join the previous consonant with ্. */
    private fun notJukta(v12: Int, v9: Int, v1: Int, v6: Int): Boolean {
        if (v9 == 'n'.code && v1 == 'g'.code && v6 == 'r'.code) return true
        if (v6 == 'r'.code || v6 == 'z'.code || v6 == 'w'.code) return false
        val dual = u.getDualJkt(v9, v1)
        return if (dual != null) !isCharInString(v6, dual)
        else {
            val single = u.getJkt(v1)
            single == null || !isCharInString(v6, single)
        }
    }

    /** Whether a two-char consonant (v1+v6) sits under the consonant(s) before it. */
    private fun dualSitsUnder(v12: Int, v9: Int, v1: Int, v6: Int): Boolean {
        if (v9 == 'r'.code && v12 == 'r'.code) return true
        if (v9 == 'r'.code) return false
        val d = u.getDjkt(v1, v6)
        if (d != null && isCharInString(v9, d)) return true
        val dd = u.getDjktt(v1, v6)
        return dd != null && dd.contains("${v12.toChar()}${v9.toChar()}")
    }

    fun toBangla(input: String): String {
        val sb = StringBuilder()
        var v1 = 0; var v9 = 0; var v12 = 0
        var v5 = 0; var v8 = 0; var v7 = 0

        for (ch in input) {
            var v6 = ch.code

            // Non-alphanumeric: pass through and reset context.
            if (!(v6 in 97..122 || v6 in 65..90 || v6 in 48..57)) {
                sb.append(ch)
                v1 = 0
                continue
            }

            // Fold uppercase letters whose case carries no meaning.
            when (v6) {
                'A'.code, 'B'.code, 'C'.code, 'E'.code, 'F'.code, 'P'.code, 'X'.code,
                'K'.code, 'L'.code, 'M'.code, 'V'.code, 'Y'.code, 'W'.code, 'Q'.code,
                -> v6 = Character.toLowerCase(v6)
            }
            if (v6 == 'H'.code && v1 != 'T'.code) v6 = 'h'.code
            // w at word start / after a vowel means ও.
            if ((v1 == 0 || isVowel(v1)) && v6 == 'w'.code) v6 = 'O'.code

            if (isVowel(v6)) {
                if (v1 == 'r'.code && v9 == 'r'.code && v6 == 'i'.code) {
                    // rri → ঋ standalone, ৃ after a consonant (the rr chars were already emitted).
                    if (v12 != 0) {
                        sb.delete(sb.length - 3, sb.length)
                        sb.append("ৃ")
                    } else {
                        sb.delete(sb.length - 2, sb.length)
                        sb.append("ঋ")
                    }
                    v1 = 'i'.code
                } else {
                    val v3 = if (v9 == 0) u.getDual(v6, v1) else u.getDualKar(v6, v1)
                    if (v3 == null) {
                        if (v6 == 'o'.code && v1 != 0) {
                            if (!isVowel(v1)) {
                                // Inherent vowel after a consonant: emit nothing, remember 'o'.
                                v12 = v9; v9 = v1; v1 = v6
                                continue
                            } else {
                                sb.append(u.get('O'.code))
                            }
                        } else {
                            if (!isVowel(v1) && v1 != 0) {
                                sb.append(u.getKar(v6)) // dependent sign after consonant
                            } else if (v6 == 'a'.code && v1 != 0) {
                                sb.append(u.get('y'.code)).append(u.getKar('a'.code)) // vowel+a → য়া
                            } else {
                                sb.append(u.get(v6)) // independent vowel
                            }
                        }
                    } else {
                        // Two-char vowel token (OI, OU, oo, …) replaces its first char.
                        if (v1 != 'o'.code) sb.delete(sb.length - 1, sb.length)
                        if (!isVowel(v9)) sb.append(v3)
                        else sb.append(u.get(v1)).append(u.get(v6))
                    }
                }
            }

            if (v6 == 'y'.code || v6 == 'Z'.code || v6 == 'r'.code) v5 = 0
            val v11 = if (v5 == 0 || u.getDual(v6, v1) != null) 0 else 1

            if (!isConsonant(v6) || !isConsonant(v1) || v11 != 0) {
                if (isConsonant(v6)) {
                    v7 = 0
                    if (isVowel(v1) && v6 == 'Z'.code) sb.append("্")
                    if (v1 == 0 && v6 == 'x'.code) sb.append(u.get('e'.code)) // x → এক্স
                    v8 = v5; v5 = 0
                    if (v6 == 'w'.code && isConsonant(v1) && isConsonant(v9)) {
                        sb.append("্"); v8 = 0; v5 = 1 // w = ব-phala inside a cluster
                    }
                    if (v12 == 'k'.code && v9 == 'S'.code && v1 == 'h'.code &&
                        (v6 == 'N'.code || v6 == 'm'.code)
                    ) {
                        sb.append("্"); v8 = 0; v5 = 1 // ক্ষ + ণ/ম
                    }
                    sb.append(u.get(v6))
                }
            } else {
                // consonant + consonant
                if ((v6 == 'y'.code || v6 == 'Z'.code) &&
                    !(v6 == 'y'.code && v1 == 'q'.code && v9 == 'q'.code)
                ) v6 = 'z'.code
                if (v9 == 'k'.code && v1 == 'k'.code && v6 == 'h'.code) v1 = 'S'.code // kkh → ক্ষ

                val v3 = u.getDual(v6, v1)
                if (v3 == null || v7 != 0) {
                    v7 = 0; v8 = v5; v5 = 0
                    if (v9 == 'r'.code || v1 != 'r'.code || v6 != 'z'.code) {
                        if ((v1 != 'r'.code || v9 == 'r'.code) &&
                            (v1 != 'r'.code || v9 != 'r'.code || !isConsonant(v12))
                        ) {
                            if (v1 != 'r'.code || v9 != 'r'.code || (!isVowel(v12) && v12 != 0)) {
                                if (!notJukta(v12, v9, v1, v6)) {
                                    sb.append("্")
                                    v5 = 1
                                }
                            } else {
                                // rr after vowel/word start → reph: drop the 2nd র, add ্.
                                sb.delete(sb.length - 1, sb.length)
                                sb.append("্")
                            }
                        }
                    } else {
                        sb.append("‍্") // r+z → র‍্য (ZWJ keeps the ya-phala form)
                    }
                    sb.append(u.get(v6))
                } else {
                    // Two-char consonant (kh, ch, Th…): replaces its first char.
                    v7 = 1
                    if (v12 == 'g'.code && v9 == 'k'.code && v1 == 'S'.code && v6 == 'h'.code) {
                        v5 = 0; v8 = 0
                    }
                    val v4 = if (isVowel(v9) || v9 == 0 || v8 != 0) 1 else 0
                    if (!dualSitsUnder(v12, v9, v1, v6) || v4 != 0) {
                        if (v5 == 0) sb.delete(sb.length - 1, sb.length)
                        else sb.delete(sb.length - 2, sb.length)
                        sb.append(v3)
                        v8 = v5; v5 = 0
                    } else {
                        sb.delete(sb.length - 1, sb.length)
                        if (v9 == 'r'.code && v12 == 'r'.code) sb.delete(sb.length - 1, sb.length)
                        if (v5 == 0 && v9 != 0 && !isVowel(v9)) sb.append("্")
                        sb.append(v3)
                        v8 = v5; v5 = 1
                    }
                }
            }

            v12 = v9; v9 = v1; v1 = v6
        }
        return sb.toString()
    }
}
