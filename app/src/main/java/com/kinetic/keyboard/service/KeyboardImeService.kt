package com.kinetic.keyboard.service

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import android.view.inputmethod.InputConnection
import com.kinetic.keyboard.engine.InputMode
import com.kinetic.keyboard.engine.KeyboardStateMachine
import com.kinetic.keyboard.engine.LayoutRepository
import com.kinetic.keyboard.input.PhoneticComposer
import com.kinetic.keyboard.text.BanglaTextValidator
import com.kinetic.keyboard.ui.KeyAction
import com.kinetic.keyboard.ui.KeyboardScreen

/**
 * Kinetic Keyboard IME entry point (SPEC.md §4).
 * Phase 1: data-driven layouts (assets/layouts) rendered in Compose, wired to the InputConnection.
 * Input processors (§5) and Bengali correctness (§3) arrive in Phase 2/3.
 */
class KeyboardImeService : InputMethodService() {

    private val lifecycleOwner = ImeLifecycleOwner()
    private lateinit var stateMachine: KeyboardStateMachine
    private val phonetic = PhoneticComposer()

    override fun onCreate() {
        super.onCreate()
        lifecycleOwner.onCreate()
        stateMachine = KeyboardStateMachine(LayoutRepository(this))
    }

    override fun onCreateInputView(): View {
        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val ui by stateMachine.state.collectAsState()
                KeyboardScreen(ui = ui, onAction = ::handleAction)
            }
        }
        // Compose resolves its Recomposer by walking up from the WINDOW'S ROOT view, not from the
        // ComposeView itself — the owners must be visible from the IME dialog's decorView or
        // composition dies with "ViewTreeLifecycleOwner not found" (found the hard way on-device).
        window?.window?.decorView?.let { lifecycleOwner.attachTo(it) }
        lifecycleOwner.attachTo(composeView)
        return composeView
    }

    private fun handleAction(action: KeyAction) {
        val ic = currentInputConnection ?: return
        when (action) {
            is KeyAction.Text -> {
                // P3.5: in phonetic mode, Roman letters stream through the transliterator.
                if (stateMachine.state.value.inputMode == InputMode.PHONETIC && isRomanLetter(action.text)) {
                    ic.setComposingText(phonetic.append(action.text), 1)
                    stateMachine.onCharCommitted()
                    return
                }
                commitComposing(ic)
                // P2.3/P2.4: NFC + repair of illegal sign sequences, based on cursor context.
                val before = ic.getTextBeforeCursor(8, 0) ?: ""
                val edit = BanglaTextValidator.process(before, action.text)
                if (edit.deleteBefore > 0) ic.deleteSurroundingText(edit.deleteBefore, 0)
                if (edit.text.isNotEmpty()) ic.commitText(edit.text, 1)
                stateMachine.onCharCommitted()
            }
            KeyAction.Space -> {
                commitComposing(ic)
                ic.commitText(" ", 1)
                stateMachine.onCharCommitted()
            }
            KeyAction.Delete -> handleDelete()
            KeyAction.Enter -> {
                commitComposing(ic)
                handleEnter()
            }
            KeyAction.Shift -> stateMachine.onShift()
            is KeyAction.LayerSwitch -> {
                commitComposing(ic)
                stateMachine.switchLayer(action.target)
            }
            KeyAction.CycleLanguage -> {
                commitComposing(ic)
                stateMachine.cycleLanguage()
            }
        }
    }

    private fun isRomanLetter(s: String) = s.length == 1 && (s[0] in 'a'..'z' || s[0] in 'A'..'Z')

    /** Finalize any pending phonetic composition (word boundary). */
    private fun commitComposing(ic: InputConnection) {
        if (phonetic.isComposing) {
            ic.finishComposingText()
            phonetic.reset()
        }
    }

    /**
     * P2.2 (revised per user decision): backspace removes the LAST KEYSTROKE, not the whole
     * grapheme cluster — কা ⌫ → ক (only the া goes). One code point per press, surrogate-safe,
     * matching the original Ridmik feel. Cluster segmentation (BengaliText) stays available for
     * cursor logic later.
     */
    private fun handleDelete() {
        val ic = currentInputConnection ?: return
        // P3.5: while composing Banglish, backspace edits the Roman buffer, not the Bangla text.
        if (phonetic.isComposing) {
            val text = phonetic.deleteLast()
            ic.setComposingText(text, 1)
            if (text.isEmpty()) ic.finishComposingText()
            return
        }
        val selected = ic.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            ic.commitText("", 1)
            return
        }
        val before = ic.getTextBeforeCursor(8, 0) ?: ""
        if (before.isEmpty()) return
        val n = Character.charCount(Character.codePointBefore(before, before.length))
        ic.deleteSurroundingText(n, 0)
    }

    /** Action-aware enter: trigger the field's IME action (search/send/go) when one is set. */
    private fun handleEnter() {
        val ic = currentInputConnection ?: return
        val options = currentInputEditorInfo?.imeOptions ?: 0
        val action = options and EditorInfo.IME_MASK_ACTION
        val hasAction = action != EditorInfo.IME_ACTION_NONE &&
            action != EditorInfo.IME_ACTION_UNSPECIFIED &&
            (options and EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0
        if (hasAction) ic.performEditorAction(action) else ic.commitText("\n", 1)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        lifecycleOwner.onResume()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        phonetic.reset() // never carry a half-typed word across fields
        lifecycleOwner.onPause()
        super.onFinishInputView(finishingInput)
    }

    override fun onDestroy() {
        lifecycleOwner.onDestroy()
        super.onDestroy()
    }
}
