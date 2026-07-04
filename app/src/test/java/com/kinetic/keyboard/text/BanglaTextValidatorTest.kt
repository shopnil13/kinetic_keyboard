package com.kinetic.keyboard.text

import com.kinetic.keyboard.text.BanglaTextValidator.Edit
import org.junit.Assert.assertEquals
import org.junit.Test

/** P2.3/P2.4/P2.6 golden corpus: what actually lands in the editor for each (context, key) pair. */
class BanglaTextValidatorTest {

    /** Applies the edit so tests read as input → final text. */
    private fun type(before: String, vararg keys: String): String {
        var text = before
        keys.forEach { key ->
            val e = BanglaTextValidator.process(text, key)
            text = text.dropLast(e.deleteBefore) + e.text
        }
        return text
    }

    @Test
    fun `plain typing passes through`() {
        assertEquals("ক", type("", "ক"))
        assertEquals("কি", type("", "ক", "ি"))
        assertEquals("আমার", type("", "আ", "ম", "া", "র"))
    }

    @Test
    fun `conjunct formation via hasanta`() {
        assertEquals("ক্ত", type("", "ক", "্", "ত"))
        assertEquals("শক্তি", type("", "শ", "ক", "্", "ত", "ি"))
    }

    @Test
    fun `special conjunct keys`() {
        // ্র r-phala, ্য ya-phala, র্ reph — as committed by the layout keys (P2.6).
        assertEquals("ক্র", type("", "ক", "্র"))
        assertEquals("ক্য", type("", "ক", "্য"))
        assertEquals("র্ক", type("", "র্", "ক"))
    }

    @Test
    fun `double vowel sign is replaced`() {
        assertEquals("কা", type("", "ক", "ি", "া"))
        assertEquals("কো", type("", "ক", "ে", "ো"))
    }

    @Test
    fun `vowel sign cancels pending hasanta`() {
        assertEquals("কি", type("", "ক", "্", "ি"))
    }

    @Test
    fun `hasanta rules`() {
        assertEquals("ক্", type("", "ক", "্", "্"))   // no double hasanta
        assertEquals("ক্", type("", "ক", "ি", "্"))   // hasanta replaces vowel sign
    }

    @Test
    fun `nfc normalization`() {
        // ো typed as ে + া decomposed → canonical single code point U+09CB.
        val e: Edit = BanglaTextValidator.process("ক", "ো")
        assertEquals("ো", e.text)
    }

    @Test
    fun `non-bengali unaffected`() {
        assertEquals("hi!", type("", "h", "i", "!"))
        assertEquals("a,", type("a", ","))
    }
}
