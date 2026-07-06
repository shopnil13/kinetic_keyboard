package com.kinetic.keyboard.engine

import com.kinetic.keyboard.engine.model.KeyTypes
import com.kinetic.keyboard.engine.model.LayoutDef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * P1.2/P1.3–P1.6 gate: every shipped layout parses, validates, and matches the structure extracted
 * from the original APK (reference/EXTRACTED_LAYOUTS.md).
 */
class LayoutIntegrityTest {

    private val layoutDir = File("src/main/assets/layouts")
    private val allIds = listOf("bn_unijoy", "bn_unijoy_shift", "symbols", "symbols_shift", "en_qwerty")

    private fun load(id: String): LayoutDef =
        LayoutParser.parse(File(layoutDir, "$id.json").readText(Charsets.UTF_8))

    @Test
    fun `all layouts parse and validate`() {
        allIds.forEach { id -> assertEquals(id, load(id).id) }
    }

    @Test
    fun `row structure matches the extraction`() {
        // rows: 10, 9, 9 (shift+7+bksp), 5 (bottom)
        listOf("bn_unijoy", "bn_unijoy_shift").forEach { id ->
            val rows = load(id).rows
            assertEquals("$id row count", 4, rows.size)
            assertEquals("$id row1", 10, rows[0].keys.size)
            assertEquals("$id row2", 9, rows[1].keys.size)
            assertEquals("$id row3", 9, rows[2].keys.size)
            assertEquals("$id bottom", 5, rows[3].keys.size)
        }
        val qwerty = load("en_qwerty").rows
        assertEquals(10, qwerty[0].keys.size)
        assertEquals(9, qwerty[1].keys.size)
        assertEquals(9, qwerty[2].keys.size)
    }

    @Test
    fun `unijoy key glyphs match ground truth`() {
        val normal = load("bn_unijoy")
        assertEquals(
            listOf("ঙ", "য", "ড", "প", "ট", "চ", "জ", "হ", "গ", "ড়"),
            normal.rows[0].keys.map { it.label },
        )
        // Conjunct keys commit without the display-only ZWJ.
        val rPhola = normal.rows[2].keys[1]
        assertEquals("্র", rPhola.output) // ্র
        assertTrue(rPhola.label.contains('‍'))

        val shift = load("bn_unijoy_shift")
        assertEquals(
            listOf("ং", "য়", "ঢ", "ফ", "ঠ", "ছ", "ঝ", "ঞ", "ঘ", "ঢ়"),
            shift.rows[0].keys.map { it.label },
        )
        assertEquals("র্", shift.rows[1].keys[0].output) // র্ reph
        assertEquals("্য", shift.rows[2].keys[1].output) // ্য ya-phola
    }

    @Test
    fun `popups carry bengali digit first then shift-consonant`() {
        // User decision (2026-07-05): digit first so the hint superscript shows the digit.
        val row1 = load("bn_unijoy").rows[0].keys
        assertEquals(listOf("৩", "ঢ"), row1[2].popup) // ড
        assertEquals(listOf("৪", "ফ"), row1[3].popup) // প
        assertEquals(listOf("০", "ঢ়"), row1[9].popup) // ড়
    }

    @Test
    fun `layer switch targets resolve`() {
        val valid = allIds.toSet() + "letters"
        allIds.forEach { id ->
            load(id).rows.flatMap { it.keys }
                .filter { it.type == KeyTypes.LAYER_SWITCH }
                .forEach { key ->
                    assertTrue("$id → ${key.target}", key.target in valid)
                }
        }
    }

    @Test
    fun `every letter layer has the standard bottom row`() {
        listOf("bn_unijoy", "bn_unijoy_shift", "en_qwerty").forEach { id ->
            val bottom = load(id).rows.last().keys
            assertEquals(KeyTypes.LAYER_SWITCH, bottom[0].type)
            assertEquals(KeyTypes.EMOJI, bottom[1].type) // P5.5: dedicated emoji key
            assertEquals(KeyTypes.SPACE, bottom[2].type)
            assertEquals(KeyTypes.ENTER, bottom[4].type)
        }
    }

    @Test
    fun `comma stays reachable after the emoji key took its spot`() {
        // P5.5 moved "," off the bottom row; it must live in the punctuation key's popup.
        listOf("bn_unijoy", "bn_unijoy_shift", "en_qwerty").forEach { id ->
            val punctuation = load(id).rows.last().keys[3]
            assertTrue("$id: ',' not in ${punctuation.label} popup", "," in punctuation.popup)
        }
    }
}
