package com.kinetic.keyboard.engine.model

import kotlinx.serialization.Serializable

/**
 * Data-driven keyboard layout (SPEC.md §6), encoded from reference/EXTRACTED_LAYOUTS.md.
 * One JSON file per layer in assets/layouts/.
 */
@Serializable
data class LayoutDef(
    val id: String,
    /** English-style shift: same layout, labels/output uppercased (no separate shift layer). */
    val caseTransformOnShift: Boolean = false,
    val rows: List<RowDef>,
)

@Serializable
data class RowDef(
    val keys: List<KeyDef>,
)

/** Key types understood by the renderer/state machine. */
object KeyTypes {
    const val CHAR = "char"
    const val SHIFT = "shift"
    const val BACKSPACE = "backspace"
    const val SPACE = "space"
    const val ENTER = "enter"
    const val LAYER_SWITCH = "layerSwitch" // `target` = layout id, or "letters" for current language base
    const val TAB = "tab"
}

@Serializable
data class KeyDef(
    val type: String = KeyTypes.CHAR,
    /** Glyph drawn on the key. May contain ZWJ for display (e.g. "‍্র" for ্র). */
    val label: String = "",
    /** Text committed on tap; defaults to [label]. Conjunct keys set this explicitly (no ZWJ). */
    val output: String? = null,
    /** Long-press alternatives, in popup order (e.g. ড → ["ঢ","৩"]). */
    val popup: List<String> = emptyList(),
    /** Width as % of keyboard width; 0 = default (10%). */
    val width: Float = 0f,
    /** Leading horizontal gap as % (row offsets, e.g. 5% on UniJoy row 2). */
    val gap: Float = 0f,
    /** Target layout for [KeyTypes.LAYER_SWITCH]. */
    val target: String? = null,
) {
    fun widthOrDefault(): Float = if (width > 0f) width else DEFAULT_WIDTH

    /** Committed text, honoring English shift-case. */
    fun outputText(uppercase: Boolean): String {
        val base = output ?: label
        return if (uppercase) base.uppercase() else base
    }

    companion object {
        const val DEFAULT_WIDTH = 10f
    }
}
