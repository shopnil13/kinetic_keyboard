package com.kinetic.keyboard.input

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * P3.3 golden corpus for the ported phonetic engine.
 *
 * The decompiled Java reference is not directly compilable (decompiler artifacts), so equivalence
 * is locked by this corpus instead: every expectation below was hand-traced through the original
 * decompiled control flow (reference/RidmikParser.java). Treat failures as port regressions, not
 * test bugs — do not "fix" an expectation without re-tracing it.
 */
class RidmikParserTest {

    private val p = RidmikParser()

    private fun assertBangla(expected: String, input: String) =
        assertEquals("toBangla(\"$input\")", expected, p.toBangla(input))

    @Test
    fun `simple words`() {
        assertBangla("আমি", "ami")
        assertBangla("আমার", "amar")
        assertBangla("তুমি", "tumi")
        assertBangla("কি", "ki")
        assertBangla("বই", "boi")
    }

    @Test
    fun `inherent vowel o is silent after consonant`() {
        assertBangla("ভাল", "bhalo")
        assertBangla("কখন", "kokhono")
        assertBangla("ক", "ko")
    }

    @Test
    fun `auto conjuncts from jkt tables`() {
        assertBangla("ক্ক", "kk")
        assertBangla("রক্ত", "rokto")
    }

    @Test
    fun `two-char consonants replace their first half`() {
        assertBangla("খ", "kh")
        assertBangla("ভ", "bh")
        assertBangla("বাংলা", "bangla")
    }

    @Test
    fun `rr forms reph`() {
        assertBangla("কর্ম", "korrmo")
    }

    @Test
    fun `rri vocalic r`() {
        assertBangla("ঋ", "rri")
        assertBangla("কৃ", "krri")
    }

    @Test
    fun `two-char vowels`() {
        assertBangla("ঐ", "OI")
        assertBangla("কু", "koo")
    }

    @Test
    fun `special tokens`() {
        assertBangla("এক্স", "x")   // word-initial x → এক্স
        assertBangla("ৎ", "TH")
        assertBangla("ও", "w")      // w at word start → ও
    }

    @Test
    fun `non-letters pass through and reset context`() {
        assertBangla("কি?", "ki?")
        assertBangla("আমি তুমি", "ami tumi")
        assertBangla("", "")
    }

    @Test
    fun `composer streams and backspaces by roman letter`() {
        val c = PhoneticComposer()
        assertEquals("আ", c.append("a"))
        assertEquals("আম", c.append("m"))
        assertEquals("আমি", c.append("i"))
        assertEquals("আম", c.deleteLast())   // removes 'i', not a Bangla char
        assertEquals("আমা", c.append("a"))
        c.reset()
        assertEquals(false, c.isComposing)
    }
}
