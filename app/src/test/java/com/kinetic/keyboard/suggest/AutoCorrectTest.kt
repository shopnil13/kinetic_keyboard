package com.kinetic.keyboard.suggest

import java.io.BufferedReader
import java.io.File
import java.io.StringReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** P4.6/P4.7 unit tests: conservative autocorrect + user-learned next-word bigrams. */
class AutoCorrectTest {

    private fun dict(tsv: String): Dictionary =
        Dictionary.load(BufferedReader(StringReader(tsv.trimIndent())))

    private val en = dict(
        """
        hello	5000
        help	3000
        the	9000000
        they	800000
        there	700000
        """
    )

    @Test
    fun `ed1 candidates found by generation`() {
        val ac = AutoCorrector(en)
        assertTrue(ac.candidates("teh").contains("the"))    // transposition
        assertTrue(ac.candidates("helo").contains("hello")) // insertion fixes deletion typo
        assertTrue(ac.candidates("thx").contains("the"))    // substitution
        assertTrue(ac.candidates("thee").contains("the"))   // deletion
    }

    @Test
    fun `correct fixes clear typos`() {
        val ac = AutoCorrector(en)
        assertEquals("the", ac.correct("teh"))
        assertEquals("hello", ac.correct("helko"))
    }

    @Test
    fun `correct leaves real and risky words alone`() {
        val ac = AutoCorrector(en)
        assertNull("in-dictionary word must not change", ac.correct("hello"))
        assertNull("too short", ac.correct("te"))
        assertNull("nothing close", ac.correct("zqzqzq"))
    }

    @Test
    fun `bangla typo correction`() {
        val bn = dict(
            """
            আমার	900
            আমি	1000
            বাংলা	5000
            """
        )
        val ac = AutoCorrector(bn)
        // missing া sign — one insertion away
        assertEquals("বাংলা", ac.correct("বংলা"))
        assertNull(ac.correct("আমি"))
    }

    @Test
    fun `bigrams learn predict and persist`() {
        val f = File.createTempFile("bigrams", ".tsv").apply { deleteOnExit() }
        val b = UserBigrams(f)
        repeat(3) { b.learn("আমি", "ভালো") }
        b.learn("আমি", "আছি")
        assertEquals(listOf("ভালো", "আছি"), b.predict("আমি", 3))
        assertEquals(emptyList<String>(), b.predict("তুমি", 3))

        b.saveIfDirty()
        val b2 = UserBigrams(f).apply { load() }
        assertEquals("ভালো", b2.predict("আমি", 1).first())
    }

    @Test
    fun `suggestion manager falls back to typo candidates`() {
        val m = SuggestionManager(english = en)
        // "teh" matches nothing by prefix → ED1 fallback kicks in
        assertTrue(m.suggest("teh", useBangla = false).contains("the"))
        // real prefix still wins
        assertEquals(listOf("the", "they", "there"), m.suggest("the", useBangla = false))
    }
}
