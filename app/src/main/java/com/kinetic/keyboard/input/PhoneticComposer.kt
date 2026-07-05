package com.kinetic.keyboard.input

/**
 * Streaming Banglish composition (SPEC.md P3.4): keeps the Roman buffer for the word being typed
 * and re-transliterates it on every keystroke. The result is shown via the editor's composing
 * region; committing (space/punct/enter/candidate) finalizes it.
 */
class PhoneticComposer(private val parser: RidmikParser = RidmikParser()) {

    private val roman = StringBuilder()

    val isComposing: Boolean get() = roman.isNotEmpty()

    /** Current transliteration of the buffer ("" when not composing). */
    fun current(): String = if (roman.isEmpty()) "" else parser.toBangla(roman.toString())

    /** Append a Roman letter; returns the new Bangla composing text. */
    fun append(s: String): String {
        roman.append(s)
        return parser.toBangla(roman.toString())
    }

    /** Remove the last Roman letter; returns the new composing text ("" when empty). */
    fun deleteLast(): String {
        if (roman.isNotEmpty()) roman.deleteCharAt(roman.length - 1)
        return if (roman.isEmpty()) "" else parser.toBangla(roman.toString())
    }

    fun reset() {
        roman.setLength(0)
    }
}
