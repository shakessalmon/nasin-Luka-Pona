package com.shakessalmon.nasinlukapona

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager

/** The keyboard service. KeyboardView handles the keys; this class edits the text. */
class NasinIME : InputMethodService() {
    lateinit var data: KeyboardData
        private set

    override fun onCreate() {
        super.onCreate()
        data = KeyboardData(this)
    }

    override fun onCreateInputView(): View = KeyboardView(this, this)

    /** Keep the keyboard below the app in landscape instead of the full-screen text box. */
    override fun onEvaluateFullscreenMode() = false

    private fun before(): String = currentInputConnection?.getTextBeforeCursor(64, 0)?.toString() ?: ""

    private fun commit(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    fun insertText(text: String) = commit(text)

    /** Types a word followed by a space, adding a space before it if the text does not end in one. */
    fun insertWord(word: String) {
        val b = before()
        val needsSpace = b.isNotEmpty() && !b.last().isWhitespace()
        commit((if (needsSpace) " " else "") + word + " ")
    }

    /** ABC: a letter that starts a word is capitalized, since names in toki pona always are (jan Sonja). */
    fun insertLetter(s: String) {
        val b = before()
        val startsWord = b.isEmpty() || b.last().isWhitespace()
        val isLetter = s.length == 1 && s[0].isLowerCase()
        commit(if (startsWord && isLetter) s.uppercase() else s)
    }

    /** Punctuation goes right after the last word: drop the auto-inserted space, then add one after the mark. */
    fun insertPunctuation(mark: String) {
        trimTrailingSpaces()
        commit("$mark ")
    }

    private fun trimTrailingSpaces() {
        val n = before().takeLastWhile { it == ' ' }.length
        if (n > 0) currentInputConnection?.deleteSurroundingText(n, 0)
    }

    fun deleteChar() {
        sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
    }

    /** Deletes the last word and the spaces after it. */
    fun deleteWord() {
        val ic = currentInputConnection ?: return
        if (!ic.getSelectedText(0).isNullOrEmpty()) {
            deleteChar()
            return
        }
        val b = before()
        val trimmed = b.trimEnd()
        val n = b.length - trimmed.length + trimmed.takeLastWhile { !it.isWhitespace() }.length
        if (n == 0) deleteChar() else ic.deleteSurroundingText(n, 0)
    }

    /** Switches the case of the last letter (the a/A key in ABC). */
    fun toggleCase() {
        val ic = currentInputConnection ?: return
        val b = before()
        if (b.isEmpty()) return
        val s = String(Character.toChars(b.codePointBefore(b.length)))
        ic.deleteSurroundingTextInCodePoints(1, 0)
        commit(if (s == s.lowercase()) s.uppercase() else s.lowercase())
    }

    /** Return: the field's action (send, search, go…) when it has one, otherwise a new line. */
    fun enter() {
        trimTrailingSpaces()
        val options = currentInputEditorInfo?.imeOptions ?: 0
        val action = options and EditorInfo.IME_MASK_ACTION
        val hasAction = (options and EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0 &&
            action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED
        if (hasAction) currentInputConnection?.performEditorAction(action) else commit("\n")
    }

    fun nextKeyboard() {
        if (!(shouldOfferSwitchingToNextInputMethod() && switchToNextInputMethod(false))) {
            getSystemService(InputMethodManager::class.java)?.showInputMethodPicker()
        }
    }
}
