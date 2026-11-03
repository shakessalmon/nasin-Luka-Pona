package com.shakessalmon.nasinlukapona

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** The app itself: how to turn the keyboard on, and a place to try it. */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        fun line(s: String, sp: Float = 15f) = column.addView(TextView(this).apply {
            text = s
            textSize = sp
            setPadding(0, pad / 2, 0, pad / 4)
        })
        fun button(label: String, onClick: () -> Unit) = column.addView(Button(this).apply {
            text = label
            setOnClickListener { onClick() }
        })

        line("Turn on the keyboard", 18f)
        line("1. Turn on nasin Luka Pona in the keyboard settings. The app has no internet access, so nothing you type leaves the phone.")
        button("Open keyboard settings") { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        line("2. Choose nasin Luka Pona as your keyboard.")
        button("Choose keyboard") { getSystemService(InputMethodManager::class.java)?.showInputMethodPicker() }

        line("Try it", 18f)
        column.addView(EditText(this).apply {
            minLines = 3
            gravity = Gravity.TOP
            hint = "toki!"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        })

        line("How to type", 18f)
        listOf(
            "Tap a key for the word in the middle, or flick left, up, right or down for the words around it. A space is added after each word.",
            "Most words sit in the direction their glyph points: mi ↓, sina ↑, ona ←, ike ↓, lili ← / suli →.",
            "Left column: ABC letters · layer 2 · layer 3 · globe. Tap the globe for the next keyboard; flick it to switch the keys between glyphs and Latin letters.",
            "Tap layer 2 or 3 to type one word from it; flick the key instead to keep the layer open.",
            "In ABC, a letter at the start of a word is capitalized for you, since names are. Tap a/A to switch the case of the last letter.",
            "Right column: delete (tap for a letter, flick for a word, hold to repeat) · space (flick for :) · punctuation (tap . ← , ↑ ? ↓ !) · return.",
        ).forEach { line(it) }

        setContentView(ScrollView(this).apply { addView(column) })
    }
}
