package com.kinetic.keyboard.service

import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import com.kinetic.keyboard.data.KeyboardPrefs
import com.kinetic.keyboard.data.PrefsRepository
import com.kinetic.keyboard.engine.InputMode
import com.kinetic.keyboard.engine.KeyboardStateMachine
import com.kinetic.keyboard.engine.LayoutRepository
import com.kinetic.keyboard.input.PhoneticComposer
import com.kinetic.keyboard.suggest.Dictionary
import com.kinetic.keyboard.suggest.SuggestionManager
import com.kinetic.keyboard.suggest.UserDictionary
import com.kinetic.keyboard.text.BanglaTextValidator
import com.kinetic.keyboard.ui.KeyAction
import com.kinetic.keyboard.ui.KeyboardScreen
import com.kinetic.keyboard.ui.theme.KbTheme
import com.kinetic.keyboard.ui.theme.ThemeMode
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Kinetic Keyboard IME entry point (SPEC.md §4).
 * P1: data-driven layouts · P2: Bengali commit pipeline · P3: phonetic composing ·
 * P4: dictionary suggestions in the strip.
 */
class KeyboardImeService : InputMethodService() {

    private val lifecycleOwner = ImeLifecycleOwner()
    private lateinit var stateMachine: KeyboardStateMachine
    private val phonetic = PhoneticComposer()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var suggestionManager: SuggestionManager
    private val suggestions = MutableStateFlow<List<String>>(emptyList())

    private lateinit var prefsRepo: PrefsRepository
    @Volatile private var currentPrefs = KeyboardPrefs()
    private var keyboardView: View? = null

    override fun onCreate() {
        super.onCreate()
        lifecycleOwner.onCreate()
        stateMachine = KeyboardStateMachine(LayoutRepository(this))

        prefsRepo = PrefsRepository(this)
        scope.launch { prefsRepo.prefs.collect { currentPrefs = it } }

        val userDict = UserDictionary(File(filesDir, "user_dict.tsv"))
        suggestionManager = SuggestionManager(userDict = userDict)
        scope.launch(Dispatchers.IO) {
            userDict.load()
            suggestionManager.bangla =
                assets.open("dict/bn.tsv").bufferedReader().use(Dictionary.Companion::load)
            suggestionManager.english =
                assets.open("dict/en.tsv").bufferedReader().use(Dictionary.Companion::load)
        }
    }

    override fun onCreateInputView(): View {
        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val ui by stateMachine.state.collectAsState()
                val words by suggestions.collectAsState()
                val prefs by prefsRepo.prefs.collectAsState(initial = currentPrefs)
                val theme = when (prefs.themeMode) {
                    ThemeMode.LIGHT -> KbTheme.Light
                    ThemeMode.DARK -> KbTheme.Dark
                    ThemeMode.SYSTEM -> if (isSystemInDarkTheme()) KbTheme.Dark else KbTheme.Light
                }
                KeyboardScreen(
                    ui = ui,
                    suggestions = words,
                    theme = theme,
                    keyHeight = prefs.keyHeightDp.dp,
                    longPressMs = prefs.longPressMs,
                    onAction = ::handleAction,
                    onSuggestion = ::commitSuggestion,
                )
            }
        }
        keyboardView = composeView
        // Compose resolves its Recomposer by walking up from the WINDOW'S ROOT view, not from the
        // ComposeView itself — the owners must be visible from the IME dialog's decorView or
        // composition dies with "ViewTreeLifecycleOwner not found" (found the hard way on-device).
        window?.window?.decorView?.let { lifecycleOwner.attachTo(it) }
        lifecycleOwner.attachTo(composeView)
        return composeView
    }

    /** P5.4: key feedback, honoring the user's toggles. */
    private fun feedback(action: KeyAction) {
        if (currentPrefs.haptics) {
            keyboardView?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        if (currentPrefs.sound) {
            val am = getSystemService(AUDIO_SERVICE) as AudioManager
            val fx = when (action) {
                KeyAction.Delete -> AudioManager.FX_KEYPRESS_DELETE
                KeyAction.Space -> AudioManager.FX_KEYPRESS_SPACEBAR
                KeyAction.Enter -> AudioManager.FX_KEYPRESS_RETURN
                else -> AudioManager.FX_KEYPRESS_STANDARD
            }
            am.playSoundEffect(fx, 1.0f)
        }
    }

    private fun handleAction(action: KeyAction) {
        val ic = currentInputConnection ?: return
        feedback(action)
        when (action) {
            is KeyAction.Text -> {
                // P3.5: in phonetic mode, Roman letters stream through the transliterator.
                if (stateMachine.state.value.inputMode == InputMode.PHONETIC && isRomanLetter(action.text)) {
                    ic.setComposingText(phonetic.append(action.text), 1)
                    stateMachine.onCharCommitted()
                } else {
                    commitComposing(ic)
                    // P2.3/P2.4: NFC + repair of illegal sign sequences, based on cursor context.
                    val before = ic.getTextBeforeCursor(8, 0) ?: ""
                    val edit = BanglaTextValidator.process(before, action.text)
                    if (edit.deleteBefore > 0) ic.deleteSurroundingText(edit.deleteBefore, 0)
                    if (edit.text.isNotEmpty()) ic.commitText(edit.text, 1)
                    stateMachine.onCharCommitted()
                }
            }
            KeyAction.Space -> {
                learnCurrentWord(ic)
                commitComposing(ic)
                ic.commitText(" ", 1)
                stateMachine.onCharCommitted()
            }
            KeyAction.Delete -> handleDelete()
            KeyAction.Enter -> {
                learnCurrentWord(ic)
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
        updateSuggestions(ic)
    }

    private fun isRomanLetter(s: String) = s.length == 1 && (s[0] in 'a'..'z' || s[0] in 'A'..'Z')

    /** Finalize any pending phonetic composition (word boundary). */
    private fun commitComposing(ic: InputConnection) {
        if (phonetic.isComposing) {
            ic.finishComposingText()
            phonetic.reset()
        }
    }

    /** P4: strip tap — replace the word being typed and learn it. */
    private fun commitSuggestion(word: String) {
        val ic = currentInputConnection ?: return
        if (phonetic.isComposing) {
            phonetic.reset() // commitText below replaces the composing region
        } else {
            val before = ic.getTextBeforeCursor(48, 0) ?: ""
            val current = SuggestionManager.currentWord(before)
            if (current.isNotEmpty()) ic.deleteSurroundingText(current.length, 0)
        }
        ic.commitText("$word ", 1)
        suggestionManager.userDict?.learn(word)
        stateMachine.onCharCommitted()
        updateSuggestions(ic)
    }

    /** P4.8: words the user finishes (space/enter) feed the user dictionary. */
    private fun learnCurrentWord(ic: InputConnection) {
        val word = if (phonetic.isComposing) {
            phonetic.current()
        } else {
            SuggestionManager.currentWord(ic.getTextBeforeCursor(48, 0) ?: "")
        }
        if (word.length >= 2) suggestionManager.userDict?.learn(word)
    }

    /** P4.5/P4.10: recompute strip candidates for the word at the cursor. */
    private fun updateSuggestions(ic: InputConnection) {
        val mode = stateMachine.state.value.inputMode
        val prefix = if (phonetic.isComposing) {
            phonetic.current()
        } else {
            SuggestionManager.currentWord(ic.getTextBeforeCursor(48, 0) ?: "")
        }
        suggestions.value = suggestionManager.suggest(prefix, useBangla = mode != InputMode.LATIN)
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

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int,
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        // Cursor moved (tap, selection): refresh the strip for the word now at the cursor.
        currentInputConnection?.let { if (!phonetic.isComposing) updateSuggestions(it) }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        lifecycleOwner.onResume()
        currentInputConnection?.let { updateSuggestions(it) }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        phonetic.reset() // never carry a half-typed word across fields
        suggestions.value = emptyList()
        scope.launch(Dispatchers.IO) { suggestionManager.userDict?.saveIfDirty() }
        lifecycleOwner.onPause()
        super.onFinishInputView(finishingInput)
    }

    override fun onDestroy() {
        scope.cancel()
        lifecycleOwner.onDestroy()
        super.onDestroy()
    }
}
