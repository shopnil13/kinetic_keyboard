package com.kinetic.keyboard.emoji

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** P5.5 gate: the shipped emoji library parses and holds its structural invariants. */
class EmojiCatalogTest {

    private val categories =
        File("src/main/assets/emoji/emoji.tsv").bufferedReader(Charsets.UTF_8)
            .use(EmojiCatalog::parse)

    @Test
    fun `library has the eight ios-style categories in order`() {
        assertEquals(
            listOf("smileys", "animals", "food", "activity", "travel", "objects", "symbols", "flags"),
            categories.map { it.id },
        )
    }

    @Test
    fun `every category is non-empty with a tab icon and no duplicates`() {
        categories.forEach { cat ->
            assertTrue("${cat.id} empty", cat.emoji.isNotEmpty())
            assertTrue("${cat.id} icon", cat.icon.isNotBlank())
            assertEquals("${cat.id} has duplicates", cat.emoji.size, cat.emoji.toSet().size)
        }
        val total = categories.sumOf { it.emoji.size }
        assertTrue("library too small: $total", total > 1000)
    }

    @Test
    fun `entries are emoji, not stray ascii`() {
        categories.flatMap { it.emoji }.forEach { e ->
            // Keycaps (#️⃣ 0️⃣) and ©️ ®️ start with ASCII/Latin-1 but carry VS16/keycap marks.
            val emojiLike = e.first().code > 0x2000 || '️' in e || '⃣' in e
            assertTrue("suspicious entry '$e'", e.isNotEmpty() && emojiLike)
        }
    }

    @Test
    fun `bangladesh flag leads the country flags`() {
        val flags = categories.last { it.id == "flags" }.emoji
        assertTrue("🇧🇩 missing from flags", "🇧🇩" in flags)
        // First country flag (after the special flags) is Bangladesh.
        assertEquals("🇧🇩", flags.first { it.codePointAt(0) in 0x1F1E6..0x1F1FF })
    }

    @Test
    fun `renderability filter drops what the device font lacks`() {
        val filtered = EmojiCatalog.filterRenderable(categories) { it != "😀" }
        val smileys = filtered.first { it.id == "smileys" }
        assertTrue("😀" !in smileys.emoji)
        assertTrue(smileys.emoji.isNotEmpty())
    }
}
