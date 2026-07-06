package com.kinetic.keyboard.emoji

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** P5.5 DoD: recents persist — most-recent-first, deduplicated, capped, file-backed. */
class EmojiRecentsTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun store(file: File, max: Int = 40) = EmojiRecents(file, max)

    @Test
    fun `record keeps most-recent-first and dedupes`() {
        val r = store(tmp.newFile())
        r.record("😀")
        r.record("🐻")
        assertEquals(listOf("😊", "🐻", "😀"), r.record("😊"))
        assertEquals(listOf("🐻", "😊", "😀"), r.record("🐻")) // re-use moves to front
    }

    @Test
    fun `capped at max`() {
        val r = store(tmp.newFile(), max = 3)
        listOf("1", "2", "3", "4").forEach(r::record)
        assertEquals(listOf("4", "3", "2"), r.record("4"))
    }

    @Test
    fun `round-trips through the file`() {
        val file = tmp.newFile()
        val r = store(file)
        r.record("😀")
        r.record("🇧🇩")
        r.save()
        assertEquals(listOf("🇧🇩", "😀"), store(file).load())
    }

    @Test
    fun `loading a missing file yields empty`() {
        assertEquals(emptyList<String>(), store(File(tmp.root, "nope.txt")).load())
    }
}
