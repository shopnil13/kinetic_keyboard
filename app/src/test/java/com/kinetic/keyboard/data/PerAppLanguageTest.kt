package com.kinetic.keyboard.data

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** P5.8 DoD: reopening an app restores its last mode — the map must round-trip. */
class PerAppLanguageTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `set then get`() {
        val p = PerAppLanguage(tmp.newFile())
        assertNull(p.get("com.whatsapp"))
        p.set("com.whatsapp", "bangla")
        p.set("com.termux", "english")
        assertEquals("bangla", p.get("com.whatsapp"))
        assertEquals("english", p.get("com.termux"))
    }

    @Test
    fun `round-trips through the file`() {
        val file = tmp.newFile()
        PerAppLanguage(file).apply {
            set("com.whatsapp", "phonetic")
            saveIfDirty()
        }
        val reloaded = PerAppLanguage(file).apply { load() }
        assertEquals("phonetic", reloaded.get("com.whatsapp"))
    }

    @Test
    fun `save skipped when nothing changed`() {
        val file = File(tmp.root, "langs.tsv")
        val p = PerAppLanguage(file)
        p.saveIfDirty()
        assertFalse("no write should happen when clean", file.exists())
        p.set("a", "bangla")
        p.set("a", "bangla") // same value twice — still one meaningful state
        p.saveIfDirty()
        assertEquals("a\tbangla", file.readText())
    }

    @Test
    fun `malformed lines are skipped on load`() {
        val file = tmp.newFile()
        file.writeText("com.ok\tbangla\ngarbage-line\n\t\n")
        val p = PerAppLanguage(file).apply { load() }
        assertEquals("bangla", p.get("com.ok"))
        assertNull(p.get("garbage-line"))
    }
}
