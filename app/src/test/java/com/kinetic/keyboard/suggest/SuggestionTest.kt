package com.kinetic.keyboard.suggest

import java.io.BufferedReader
import java.io.File
import java.io.StringReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** P4.3/P4.9 unit tests + shipped-asset sanity checks. */
class SuggestionTest {

    private fun dict(tsv: String): Dictionary =
        Dictionary.load(BufferedReader(StringReader(tsv.trimIndent())))

    @Test
    fun `prefix lookup ranks by frequency`() {
        val d = dict(
            """
            আম	50
            আমরা	300
            আমার	900
            আমি	1000
            কলা	10
            """
        )
        assertEquals(listOf("আমি", "আমার", "আমরা"), d.byPrefix("আম", 3))
        assertEquals(listOf("কলা"), d.byPrefix("ক", 3))
        assertEquals(emptyList<String>(), d.byPrefix("খ", 3))
        assertEquals(emptyList<String>(), d.byPrefix("", 3))
        assertEquals(1000, d.frequencyOf("আমি"))
    }

    @Test
    fun `current word extraction`() {
        assertEquals("আম", SuggestionManager.currentWord("আমি আম"))
        assertEquals("", SuggestionManager.currentWord("আমি "))
        assertEquals("ক্ত", SuggestionManager.currentWord("যুক্ত".substring(2)))
        assertEquals("hello", SuggestionManager.currentWord("say hello"))
        assertEquals("", SuggestionManager.currentWord(""))
        // hasanta and joiners count as part of the word
        assertEquals("ক্", SuggestionManager.currentWord("এখন ক্"))
        // spacing vowel signs (Mc category: া ে ো) are word chars too
        assertEquals("কা", SuggestionManager.currentWord("আমি কা"))
        assertEquals("কো", SuggestionManager.currentWord("কো"))
    }

    @Test
    fun `user dictionary learns and outranks corpus`() {
        val f = File.createTempFile("udict", ".tsv").apply { deleteOnExit() }
        val u = UserDictionary(f)
        repeat(3) { u.learn("আমড়া") }
        val m = SuggestionManager(
            bangla = dict("আমার\t900\nআমি\t1000"),
            userDict = u,
        )
        val s = m.suggest("আম", useBangla = true)
        assertEquals("আমড়া", s.first()) // user word first
        assertTrue(s.contains("আমি"))
        // persistence round-trip
        u.saveIfDirty()
        val u2 = UserDictionary(f).apply { load() }
        assertEquals(listOf("আমড়া"), u2.byPrefix("আম", 3))
    }

    @Test
    fun `shipped dictionary assets load sorted and searchable`() {
        listOf("bn", "en").forEach { lang ->
            val d = File("src/main/assets/dict/$lang.tsv").bufferedReader().use(Dictionary.Companion::load)
            assertTrue("$lang dictionary too small: ${d.size}", d.size > 10_000)
        }
        val bn = File("src/main/assets/dict/bn.tsv").bufferedReader().use(Dictionary.Companion::load)
        assertTrue(bn.byPrefix("আম", 3).isNotEmpty())
        val en = File("src/main/assets/dict/en.tsv").bufferedReader().use(Dictionary.Companion::load)
        assertTrue(en.byPrefix("th", 3).contains("the"))
    }
}
