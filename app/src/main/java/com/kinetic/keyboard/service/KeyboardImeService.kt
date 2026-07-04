package com.kinetic.keyboard.service

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.kinetic.keyboard.engine.KeyboardStateMachine
import com.kinetic.keyboard.engine.LayoutRepository
import com.kinetic.keyboard.text.BanglaTextValidator
import com.kinetic.keyboard.text.BengaliText
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
        lifecycleOwner.attachTo(composeView)
        return composeView
    }

    private fun handleAction(action: KeyAction) {
        val ic = currentInputConnection ?: return
        when (action) {
            is KeyAction.Text -> {
                // P2.3/P2.4: NFC + repair of illegal sign sequences, based on cursor context.
                val before = ic.getTextBeforeCursor(8, 0) ?: ""
                val edit = BanglaTextValidator.process(before, action.text)
                if (edit.deleteBefore > 0) ic.deleteSurroundingText(edit.deleteBefore, 0)
                if (edit.text.isNotEmpty()) ic.commitText(edit.text, 1)
                stateMachine.onCharCommitted()
            }
            KeyAction.Space -> {
                ic.commitText(" ", 1)
                stateMachine.onCharCommitted()
            }
            KeyAction.Delete -> handleDelete()
            KeyAction.Enter -> handleEnter()
            KeyAction.Shift -> stateMachine.onShift()
            is KeyAction.LayerSwitch -> stateMachine.switchLayer(action.target)
            KeyAction.CycleLanguage -> stateMachine.cycleLanguage()
        }
    }

    /** P2.2: delete a full grapheme cluster (ক্তি in one press), or the selection if one exists. */
    private fun handleDelete() {
        val ic = currentInputConnection ?: return
        val selected = ic.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            ic.commitText("", 1)
            return
        }
        val before = ic.getTextBeforeCursor(16, 0) ?: ""
        val n = BengaliText.lastClusterLength(before)
        if (n > 0) ic.deleteSurroundingText(n, 0)
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
        lifecycleOwner.onPause()
        super.onFinishInputView(finishingInput)
    }

    override fun onDestroy() {
        lifecycleOwner.onDestroy()
        super.onDestroy()
    }
}
