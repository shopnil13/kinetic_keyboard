package com.kinetic.keyboard.text

import org.junit.Assert.assertEquals
import org.junit.Test

/** P2.1/P2.2 golden corpus: one backspace press == one grapheme cluster. */
class BengaliTextTest {

    private fun clusterOf(s: String) = s.substring(s.length - BengaliText.lastClusterLength(s))

    @Test
    fun `empty and ascii`() {
        assertEquals(0, BengaliText.lastClusterLength(""))
        assertEquals("a", clusterOf("banglA a"))
        assertEquals(" ", clusterOf("আমি "))
        assertEquals("1", clusterOf("ক1"))
    }

    @Test
    fun `single letters and digits`() {
        assertEquals("ক", clusterOf("ক"))
        assertEquals("অ", clusterOf("আমঅ"))
        assertEquals("৩", clusterOf("১২৩"))   // Bengali digits are standalone
        assertEquals("।", clusterOf("আমি।"))  // danda standalone
    }

    @Test
    fun `consonant plus vowel sign`() {
        assertEquals("কি", clusterOf("আমকি"))
        assertEquals("মা", clusterOf("আমা"))
        assertEquals("রু", clusterOf("রু"))
        assertEquals("সো", clusterOf("সো"))   // two-part sign, single code point
    }

    @Test
    fun `signs and marks`() {
        assertEquals("মাং", clusterOf("মাং")) // anusvara extends
        assertEquals("চাঁ", clusterOf("চাঁ")) // candrabindu extends
        assertEquals("ক্", clusterOf("যাক্")) // trailing hasanta joins its consonant
    }

    @Test
    fun `conjuncts delete as one unit`() {
        assertEquals("ক্ত", clusterOf("শক্ত"))
        assertEquals("ন্দ", clusterOf("আনন্দ"))
        assertEquals("ক্ষ", clusterOf("ক্ষ"))
        assertEquals("ক্ষ্ম", clusterOf("লক্ষ্ম"))       // three-consonant chain
        assertEquals("ক্তি", clusterOf("শক্তি"))         // conjunct + vowel sign
        assertEquals("র্ক", clusterOf("তর্ক"))           // reph
        assertEquals("ক্র", clusterOf("বক্র"))           // ra-phala
        assertEquals("ড়্গ", clusterOf("খড়্গ"))          // nukta consonant inside chain
    }

    @Test
    fun `joiners stay attached`() {
        assertEquals("র্‍", clusterOf("র্‍"))       // reph key display form
        assertEquals("ক্‌ত", clusterOf("ক্‌ত"))    // explicit ZWNJ conjunct-breaker
    }

    @Test
    fun `bare signs without a base`() {
        assertEquals("ি", clusterOf("aি"))  // sign after Latin deletes alone
        assertEquals("্র", clusterOf("্র")) // r-phala committed at start
    }
}
