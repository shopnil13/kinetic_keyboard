package com.kinetic.keyboard.text

import java.text.Normalizer

/**
 * Commit-time text pipeline (SPEC.md §3, P2.3/P2.4).
 *
 * Given the text just before the cursor and the key's output, decides what actually gets
 * committed. Policy is deliberately *permissive* — bare signs (dotted-circle forms) stay legal,
 * as on the original keyboard — but obviously-corrective cases are repaired:
 *  - vowel sign typed after a vowel sign → replaces it (fixes the "double kar" typo);
 *  - vowel sign typed after hasanta → replaces it (cancels an unfinished conjunct);
 *  - hasanta after hasanta → ignored (no double virama);
 *  - hasanta after a vowel sign → replaces it.
 * Everything committed is NFC-normalized.
 */
object BanglaTextValidator {

    /** deleteBefore chars are removed before the cursor, then [text] is committed ("" = no-op). */
    data class Edit(val deleteBefore: Int, val text: String)

    private fun isVowelSign(cp: Int) = when (cp) {
        in 0x09BE..0x09C4, 0x09C7, 0x09C8, 0x09CB, 0x09CC -> true
        else -> false
    }

    fun process(before: CharSequence, keyOutput: String): Edit {
        val text = Normalizer.normalize(keyOutput, Normalizer.Form.NFC)
        if (text.isEmpty()) return Edit(0, "")

        val typedFirst = text.codePointAt(0)
        val prev = if (before.isEmpty()) -1 else Character.codePointBefore(before, before.length)
        if (prev == -1) return Edit(0, text)

        val typedIsVowelSign = isVowelSign(typedFirst) && text.length == Character.charCount(typedFirst)
        val typedIsHasanta = BengaliText.isHasanta(typedFirst) && text.length == 1

        return when {
            // Double vowel sign → replace the old one (কি + া → কা).
            typedIsVowelSign && isVowelSign(prev) -> Edit(Character.charCount(prev), text)
            // Vowel sign after hasanta → the sign wins (ক্ + ি → কি).
            typedIsVowelSign && BengaliText.isHasanta(prev) -> Edit(1, text)
            // No double hasanta.
            typedIsHasanta && BengaliText.isHasanta(prev) -> Edit(0, "")
            // Hasanta replaces a vowel sign (কি + ্ → ক্).
            typedIsHasanta && isVowelSign(prev) -> Edit(Character.charCount(prev), text)
            else -> Edit(0, text)
        }
    }
}
