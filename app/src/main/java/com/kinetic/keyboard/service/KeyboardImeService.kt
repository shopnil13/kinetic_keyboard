package com.kinetic.keyboard.service

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.kinetic.keyboard.ui.MinimalKeyboard

/**
 * Kinetic Keyboard IME entry point (SPEC.md §4).
 *
 * Phase 0 scope: stand up Compose inside the IME window and prove typing works end-to-end via
 * [android.view.inputmethod.InputConnection]. The real layout engine, input processors, and
 * suggestion strip arrive in Phase 1+.
 */
class KeyboardImeService : InputMethodService() {

    private val lifecycleOwner = ImeLifecycleOwner()

    override fun onCreate() {
        super.onCreate()
        lifecycleOwner.onCreate()
    }

    override fun onCreateInputView(): View {
        val composeView = ComposeView(this).apply {
            // We own the lifecycle, so dispose the composition when that lifecycle is destroyed.
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MinimalKeyboard(
                    onText = { text -> currentInputConnection?.commitText(text, 1) },
                    onDelete = { currentInputConnection?.deleteSurroundingText(1, 0) },
                    onEnter = { currentInputConnection?.commitText("\n", 1) },
                )
            }
        }
        lifecycleOwner.attachTo(composeView)
        return composeView
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
