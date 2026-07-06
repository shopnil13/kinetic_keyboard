package com.kinetic.keyboard.service

import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.view.HapticFeedbackConstants
import android.view.View
import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
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
import com.kinetic.keyboard.suggest.UserBigrams
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

    /** Last word committed by space/enter/candidate — feeds bigram learning + prediction. */
    private var lastCommittedWord: String = ""

    /** Set right after an autocorrect so one backspace can undo it (original, corrected). */
    private var pendingUndo: Pair<String, String>? = null

    override fun onCreate() {
        super.onCreate()
        lifecycleOwner.onCreate()
        stateMachine = KeyboardStateMachine(LayoutRepository(this))

        prefsRepo = PrefsRepository(this)
        scope.launch { prefsRepo.prefs.collect { currentPrefs = it } }

        val userDict = UserDictionary(File(filesDir, "user_dict.tsv"))
        val bigrams = UserBigrams(File(filesDir, "user_bigrams.tsv"))
        suggestionManager = SuggestionManager(userDict = userDict, bigrams = bigrams)
        scope.launch(Dispatchers.IO) {
            userDict.load()
            bigrams.load()
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
        // An autocorrect is only undoable by the immediately following backspace.
        if (action != KeyAction.Delete) pendingUndo = null
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
                val word = wordBeforeCursor(ic)
                commitComposing(ic)
                var committed = word
                // P4.7: conservative autocorrect on word commit (not in phonetic mode — its
                // output is deterministic transliteration, not typos; never in private fields).
                if (word.isNotEmpty() && !isPrivateField() &&
                    stateMachine.state.value.inputMode != InputMode.PHONETIC
                ) {
                    val useBangla = stateMachine.state.value.inputMode != InputMode.LATIN
                    val corrected = suggestionManager.corrector(useBangla).correct(word)
                    if (corrected != null) {
                        ic.deleteSurroundingText(word.length, 0)
                        ic.commitText(corrected, 1)
                        pendingUndo = word to corrected
                        committed = corrected
                    }
                }
                if (committed.length >= 2 && !isPrivateField()) {
                    suggestionManager.userDict?.learn(committed)
                    if (lastCommittedWord.isNotEmpty()) {
                        suggestionManager.bigrams?.learn(lastCommittedWord, committed) // P4.6
                    }
                }
                if (committed.isNotEmpty()) lastCommittedWord = committed
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
            KeyAction.ShowImePicker -> {
                commitComposing(ic)
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
            }
        }
        updateSuggestions(ic)
        // A manual shift tap is the user overriding caps — don't immediately fight it.
        if (action != KeyAction.Shift) updateAutoCaps(ic)
    }

    /**
     * Gboard-style auto-capitalization: at a sentence start (per the field's declared caps
     * mode) engage one-shot shift. English only — Bangla has no case, and in phonetic mode
     * capital Roman letters are distinct phonemes the user must control manually.
     */
    private fun updateAutoCaps(ic: InputConnection) {
        if (stateMachine.state.value.inputMode != InputMode.LATIN) return
        val inputType = currentInputEditorInfo?.inputType ?: return
        if (inputType == InputType.TYPE_NULL) return
        stateMachine.setAutoShift(ic.getCursorCapsMode(inputType) != 0)
    }

    private fun isRomanLetter(s: String) = s.length == 1 && (s[0] in 'a'..'z' || s[0] in 'A'..'Z')

    /**
     * P6.4 (privacy): password / no-suggestion fields must never feed the user dictionary or
     * bigrams, never get autocorrected, and never show suggestions — see PRIVACY.md.
     */
    private fun isPrivateField(): Boolean {
        val type = currentInputEditorInfo?.inputType ?: return false
        val cls = type and InputType.TYPE_MASK_CLASS
        val variation = type and InputType.TYPE_MASK_VARIATION
        return when (cls) {
            InputType.TYPE_CLASS_TEXT ->
                variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                    variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                    variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
                    (type and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0
            InputType.TYPE_CLASS_NUMBER ->
                variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
            else -> false
        }
    }

    /** Finalize any pending phonetic composition (word boundary). */
    private fun commitComposing(ic: InputConnection) {
        if (phonetic.isComposing) {
            ic.finishComposingText()
            phonetic.reset()
        }
    }

    /** The word being typed at the cursor (composing transliteration in phonetic mode). */
    private fun wordBeforeCursor(ic: InputConnection): String = if (phonetic.isComposing) {
        phonetic.current()
    } else {
        SuggestionManager.currentWord(ic.getTextBeforeCursor(48, 0) ?: "")
    }

    /** P4: strip tap — replace the word being typed and learn it. */
    private fun commitSuggestion(word: String) {
        val ic = currentInputConnection ?: return
        if (phonetic.isComposing) {
            phonetic.reset() // commitText below replaces the composing region
        } else {
            val current = SuggestionManager.currentWord(ic.getTextBeforeCursor(48, 0) ?: "")
            if (current.isNotEmpty()) ic.deleteSurroundingText(current.length, 0)
        }
        ic.commitText("$word ", 1)
        suggestionManager.userDict?.learn(word)
        if (lastCommittedWord.isNotEmpty()) suggestionManager.bigrams?.learn(lastCommittedWord, word)
        lastCommittedWord = word
        stateMachine.onCharCommitted()
        updateSuggestions(ic)
    }

    /** P4.8: words the user finishes with enter feed the user dictionary. */
    private fun learnCurrentWord(ic: InputConnection) {
        if (isPrivateField()) return
        val word = wordBeforeCursor(ic)
        if (word.length >= 2) suggestionManager.userDict?.learn(word)
    }

    /** P4.5/P4.10: recompute strip candidates for the word at the cursor. */
    private fun updateSuggestions(ic: InputConnection) {
        if (isPrivateField()) {
            suggestions.value = emptyList()
            return
        }
        val mode = stateMachine.state.value.inputMode
        val prefix = wordBeforeCursor(ic)
        suggestions.value = if (prefix.isEmpty()) {
            // P4.6: just finished a word? Offer likely next words from learned bigrams.
            val before = ic.getTextBeforeCursor(48, 0) ?: ""
            if (before.isNotEmpty() && before.last() == ' ') {
                val prev = SuggestionManager.currentWord(before.toString().trimEnd())
                suggestionManager.predictNext(prev)
            } else {
                emptyList()
            }
        } else {
            suggestionManager.suggest(prefix, useBangla = mode != InputMode.LATIN)
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
        // P4.7: one backspace right after an autocorrect reverts it (corrected+space → original).
        pendingUndo?.let { (original, corrected) ->
            pendingUndo = null
            val tail = ic.getTextBeforeCursor(corrected.length + 1, 0) ?: ""
            if (tail.toString() == "$corrected " || tail.toString().endsWith("$corrected ")) {
                ic.deleteSurroundingText(corrected.length + 1, 0)
                ic.commitText(original, 1)
                lastCommittedWord = ""
                updateSuggestions(ic)
                return
            }
        }
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
        currentInputConnection?.let {
            if (!phonetic.isComposing) updateSuggestions(it)
            updateAutoCaps(it)
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        lifecycleOwner.onResume()
        lastCommittedWord = ""
        pendingUndo = null
        currentInputConnection?.let {
            updateSuggestions(it)
            updateAutoCaps(it)
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        phonetic.reset() // never carry a half-typed word across fields
        suggestions.value = emptyList()
        scope.launch(Dispatchers.IO) {
            suggestionManager.userDict?.saveIfDirty()
            suggestionManager.bigrams?.saveIfDirty()
        }
        lifecycleOwner.onPause()
        super.onFinishInputView(finishingInput)
    }

    override fun onDestroy() {
        scope.cancel()
        lifecycleOwner.onDestroy()
        super.onDestroy()
    }
}
