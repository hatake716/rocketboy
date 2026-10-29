package io.github.hatake716.rocketboy

import android.content.Context
import android.text.InputFilter
import android.text.InputType
import android.view.KeyEvent
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import android.widget.EditText

/** Retains Android's full IME contract, including Japanese composition and dakuten editing. */
class KanaInput(context: Context) : EditText(context) {
    var onCommitted: (() -> Unit)? = null
    var onKeyboardBack: (() -> Unit)? = null
    var suppressSubmission = false

    init {
        setSingleLine(true)
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        imeOptions = EditorInfo.IME_ACTION_SEND or EditorInfo.IME_FLAG_NO_EXTRACT_UI or
            EditorInfo.IME_FLAG_NO_FULLSCREEN or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
        imeHintLocales = android.os.LocaleList.forLanguageTags("ja-JP")
        filters = arrayOf(InputFilter.LengthFilter(Quotes.MAX_READING_LENGTH))
        importantForAutofill = IMPORTANT_FOR_AUTOFILL_NO
        setSelectAllOnFocus(false)
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val original = super.onCreateInputConnection(outAttrs) ?: return null
        return object : InputConnectionWrapper(original, false) {
            override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
                val accepted = super.commitText(text, newCursorPosition)
                scheduleCommitted()
                return accepted
            }
            override fun finishComposingText(): Boolean {
                val accepted = super.finishComposingText()
                scheduleCommitted()
                return accepted
            }
            override fun sendKeyEvent(event: KeyEvent): Boolean {
                val accepted = super.sendKeyEvent(event)
                if (event.action == KeyEvent.ACTION_UP) scheduleCommitted()
                return accepted
            }
        }
    }

    private val submit = Runnable {
        if (!suppressSubmission && hasFocus() &&
            BaseInputConnection.getComposingSpanStart(text) < 0 && !text.isNullOrBlank()) onCommitted?.invoke()
    }
    private fun scheduleCommitted() {
        removeCallbacks(submit)
        if (!suppressSubmission) post(submit)
    }
    fun clearAfterLaunch() {
        removeCallbacks(submit)
        suppressSubmission = true
        clearComposingText()
        text?.clear()
        suppressSubmission = false
    }
    override fun onDetachedFromWindow() {
        removeCallbacks(submit)
        super.onDetachedFromWindow()
    }
    override fun onKeyPreIme(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) onKeyboardBack?.invoke()
        return super.onKeyPreIme(keyCode, event)
    }
}
