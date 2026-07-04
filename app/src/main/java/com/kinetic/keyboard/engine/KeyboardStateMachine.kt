package com.kinetic.keyboard.engine

import android.os.SystemClock
import com.kinetic.keyboard.engine.model.LayoutDef
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** A language "set": its base layer and (for Bangla) a dedicated shift layer. */
data class LanguageSet(
    val id: String,
    val displayName: String,
    val baseLayout: String,
    /** null → shift is a case transform on the base layout (English). */
    val shiftLayout: String? = null,
)

data class KeyboardUiState(
    val layout: LayoutDef,
    val shifted: Boolean,
    val capsLock: Boolean,
    val spaceLabel: String,
) {
    /** English-style shift: uppercase labels & output. */
    val uppercase: Boolean get() = layout.caseTransformOnShift && (shifted || capsLock)

    /** Whether the shift key should render as active. */
    val shiftVisual: Boolean get() = shifted || capsLock || layout.id.endsWith("_shift")
}

/**
 * Active layer / shift / language state (SPEC.md P1.8).
 * Shift semantics: tap = one-shot (reverts after next char); double-tap = caps lock; tap again = release.
 */
class KeyboardStateMachine(
    private val repo: LayoutRepository,
    private val sets: List<LanguageSet> = DEFAULT_SETS,
) {
    private var setIndex = 0
    private var capsLock = false
    private var shifted = false
    private var lastShiftAt = 0L

    private val currentSet: LanguageSet get() = sets[setIndex]

    private val _state = MutableStateFlow(buildState(repo.get(currentSet.baseLayout)))
    val state: StateFlow<KeyboardUiState> = _state

    private fun buildState(layout: LayoutDef) = KeyboardUiState(
        layout = layout,
        shifted = shifted,
        capsLock = capsLock,
        spaceLabel = currentSet.displayName,
    )

    private fun show(layoutId: String) {
        _state.value = buildState(repo.get(layoutId))
    }

    fun onShift(now: Long = SystemClock.uptimeMillis()) {
        val set = currentSet
        val current = _state.value.layout
        if (current.id == "symbols" || current.id == "symbols_shift") return // ALT is a layerSwitch

        val doubleTap = now - lastShiftAt < DOUBLE_TAP_MS
        lastShiftAt = now

        if (set.shiftLayout != null) {
            // Bangla: shift is a separate layer.
            when (current.id) {
                set.baseLayout -> {
                    capsLock = false
                    show(set.shiftLayout)
                }
                set.shiftLayout -> {
                    if (doubleTap && !capsLock) {
                        capsLock = true
                        show(set.shiftLayout)
                    } else {
                        capsLock = false
                        show(set.baseLayout)
                    }
                }
            }
        } else {
            // English: case transform.
            if (doubleTap && shifted && !capsLock) {
                capsLock = true
                shifted = true
            } else if (capsLock || shifted) {
                capsLock = false
                shifted = false
            } else {
                shifted = true
            }
            show(current.id)
        }
    }

    /** Call after a character/space commit: releases one-shot shift unless caps-locked. */
    fun onCharCommitted() {
        if (capsLock) return
        val set = currentSet
        val current = _state.value.layout
        if (set.shiftLayout != null && current.id == set.shiftLayout) {
            show(set.baseLayout)
        } else if (shifted) {
            shifted = false
            show(current.id)
        }
    }

    fun switchLayer(target: String) {
        when (target) {
            "letters" -> {
                shifted = false; capsLock = false
                show(currentSet.baseLayout)
            }
            else -> show(target)
        }
    }

    fun cycleLanguage() {
        setIndex = (setIndex + 1) % sets.size
        shifted = false; capsLock = false
        show(currentSet.baseLayout)
    }

    companion object {
        private const val DOUBLE_TAP_MS = 350L
        val DEFAULT_SETS = listOf(
            LanguageSet("bangla", "ইউনিজয়", "bn_unijoy", "bn_unijoy_shift"),
            LanguageSet("english", "English", "en_qwerty"),
        )
    }
}
