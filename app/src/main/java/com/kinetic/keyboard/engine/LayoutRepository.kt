package com.kinetic.keyboard.engine

import android.content.Context
import com.kinetic.keyboard.engine.model.KeyTypes
import com.kinetic.keyboard.engine.model.LayoutDef
import kotlinx.serialization.json.Json

/**
 * Parses layout JSON (SPEC.md §6). Pure JVM — no Android dependencies — so unit tests can validate
 * the asset files directly from disk.
 */
object LayoutParser {
    private val json = Json { ignoreUnknownKeys = false }

    fun parse(text: String): LayoutDef {
        val layout = json.decodeFromString<LayoutDef>(text)
        validate(layout)
        return layout
    }

    /** Structural validation (P1.2): fail fast on malformed layouts instead of at render time. */
    private fun validate(layout: LayoutDef) {
        require(layout.id.isNotBlank()) { "layout id missing" }
        require(layout.rows.isNotEmpty()) { "${layout.id}: no rows" }
        layout.rows.forEachIndexed { r, row ->
            require(row.keys.isNotEmpty()) { "${layout.id} row $r: empty" }
            var total = 0f
            row.keys.forEach { key ->
                total += key.gap + key.widthOrDefault()
                when (key.type) {
                    KeyTypes.CHAR -> require((key.output ?: key.label).isNotEmpty()) {
                        "${layout.id} row $r: char key with no output"
                    }
                    KeyTypes.LAYER_SWITCH -> require(!key.target.isNullOrBlank()) {
                        "${layout.id} row $r: layerSwitch without target"
                    }
                    KeyTypes.SHIFT, KeyTypes.BACKSPACE, KeyTypes.SPACE, KeyTypes.ENTER, KeyTypes.TAB -> Unit
                    else -> error("${layout.id} row $r: unknown key type '${key.type}'")
                }
            }
            require(total <= 100.5f) { "${layout.id} row $r: widths+gaps sum to $total%" }
        }
    }
}

/** Loads and caches layouts from assets/layouts/<id>.json (P1.7). */
class LayoutRepository(private val context: Context) {
    private val cache = mutableMapOf<String, LayoutDef>()

    fun get(id: String): LayoutDef = cache.getOrPut(id) {
        val text = context.assets.open("layouts/$id.json").bufferedReader().use { it.readText() }
        LayoutParser.parse(text).also {
            require(it.id == id) { "layout file $id.json declares id '${it.id}'" }
        }
    }
}
