package com.shakessalmon.nasinlukapona

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.SoundEffectConstants
import android.view.View
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** The keyboard: a 3×4 flick pad of word keys with a side column on each edge, like the 12-key flick pad. */
@SuppressLint("ViewConstructor")
class KeyboardView(context: Context, private val ime: NasinIME) : View(context) {
    companion object {
        /** A move shorter than this (in dp) is a tap, as in the web and iOS versions. */
        const val TAP_RADIUS_DP = 18f

        /**
         * sronhatie (Thumb-Key's English layout) with 4-way flicks: the 9 most common letters are taps,
         * the others are flicks toward the middle key, and h keeps the letters around it.
         */
        val ABC = listOf(
            listOf("s", "", "", "w", "j"), listOf("r", "", "", "", "g"), listOf("o", "u", "", "", "b"),
            listOf("n", "", "", "m", ""), listOf("h", "k", "q", "p", "x"), listOf("a", "l", "", "", ""),
            listOf("t", "", "v", "c", ""), listOf("i", "", "f", "z", ""), listOf("e", "d", "y", "", ""),
            listOf("a/A", "-", "~", "*", "%"), listOf("'", "\"", "(", ")", ""), listOf("@", "#", "/", "&", "_"),
        )

        /** The ABC slot that switches the case of the last letter */
        const val SHIFT = "a/A"

        /** [tap, left, up, right, down]. Right is left empty: this key is at the screen edge. The colon is on the space key. */
        val PUNCTUATION = listOf(".", ",", "?", "", "!")
        val LAYER_ICONS = listOf("toki", "soweli", "poki")

        /** 0 = tap, 1 = left, 2 = up, 3 = right, 4 = down */
        fun direction(dx: Float, dy: Float, radius: Float): Int {
            if (hypot(dx, dy) < radius) return 0
            return if (abs(dx) > abs(dy)) (if (dx < 0) 1 else 3) else (if (dy < 0) 2 else 4)
        }
    }

    /** KEY: a word or letter key. SIDE: a side key. SOFT: a layer open for one word. STRONG: a layer kept open, or ABC. */
    private enum class Look { KEY, SIDE, SOFT, STRONG }

    private enum class Icon { GLOBE, DELETE, RETURN }

    private class Key(
        /** what is drawn: [tap, left, up, right, down]; "" leaves that place blank */
        val faces: List<String>,
        /** the faces are sitelen pona glyphs */
        val glyph: Boolean,
        val look: Look,
        /** while held, show all five slots as a cross (word and letter keys); otherwise a bubble with the current one */
        val guide: Boolean,
        val mainSp: Float,
        val edgeSp: Float,
        val preview: (Int) -> Pair<String?, String>?,
        val pick: (Int) -> Unit,
    ) {
        val rect = RectF()
        var icon: Icon? = null
        var repeat: (() -> Unit)? = null
        var dim = false
    }

    private class Press(val key: Key, val x0: Float, val y0: Float) {
        var slot = 0
        var repeating = false
    }

    private val data = ime.data
    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density
    private val compact get() = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    /** Size in px for the current keyboard height. */
    private fun size(sp: Float) = dp(if (compact) sp * 0.78f else sp)

    private val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    private fun color(light: Long, dark: Long) = (if (night) dark else light).toInt()
    private val cBoard = color(0xFFD1D5DB, 0xFF1E2225)
    private val cKey = color(0xFFFFFFFF, 0xFF5A6066)
    private val cSide = color(0xFFACB3BC, 0xFF34393D)
    private val cPressed = color(0xFFE2F0EB, 0xFF1D3530)
    private val cAccent = color(0xFF1F6F5C, 0xFF5FC4A6)
    private val cOnAccent = color(0xFFFFFFFF, 0xFF0F1A17)
    private val cText = color(0xFF1D2226, 0xFFE9EDF0)
    private val cMuted = color(0xFF66707A, 0xFF98A3AC)
    private val cShadow = color(0x40000000, 0x66000000)

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val path = Path()

    /** 0, 1, 2 = word layers, 3 = ABC */
    private var mode = 0

    /** false: layer 2 or 3 goes back to layer 1 after one word. true: it stays open. */
    private var layerLocked = false
    private val prefs = context.getSharedPreferences("keyboard", Context.MODE_PRIVATE)
    private var showGlyphs = prefs.getBoolean("showGlyphs", true)
    private var keys: List<Key> = emptyList()
    private val presses = LinkedHashMap<Int, Press>()

    /** the press whose guide or bubble is drawn */
    private var shown: Press? = null
    private val handler = Handler(Looper.getMainLooper())
    private var repeatTask: Runnable? = null

    init {
        isSoundEffectsEnabled = true
        isHapticFeedbackEnabled = true
        contentDescription = "nasin Luka Pona"
        rebuild()
    }

    // ---- Building the keys

    private fun rebuild() {
        presses.clear()
        shown = null
        stopRepeat()
        val groups = if (mode < 3) data.layers.getOrElse(mode) { emptyList() } else ABC
        val left = listOf(if (mode == 3) backKey() else lettersKey(), layerKey(1), layerKey(2), globeKey())
        val right = listOf(deleteKey(), spaceKey(), punctuationKey(), returnKey())
        val list = ArrayList<Key>()
        for (row in 0 until 4) {
            list += left[row]
            for (col in 0 until 3) list += mainKey(groups.getOrElse(row * 3 + col) { emptyList() }, word = mode < 3)
            list += right[row]
        }
        keys = list
        if (width > 0) layoutKeys()
        invalidate()
    }

    private fun go(newMode: Int, locked: Boolean = false) {
        mode = newMode
        layerLocked = locked
        rebuild()
    }

    private fun mainKey(words: List<String>, word: Boolean): Key {
        val slots = List(5) { words.getOrElse(it) { "" } }
        val glyphFace = word && showGlyphs
        val faces = slots.map { if (glyphFace) data.glyphs[it] ?: it else it }
        val mainSp = if (glyphFace) 24f else if (word) 15f else 18f
        val edgeSp = if (glyphFace) 12f else if (word) 9f else 11f
        return Key(faces, glyphFace, Look.KEY, guide = true, mainSp = mainSp, edgeSp = edgeSp,
            preview = { i ->
                val s = slots[i]
                when {
                    s.isEmpty() -> null
                    word -> Pair(data.glyphs[s], s)
                    s == SHIFT -> Pair(null, "Aa")
                    else -> Pair(null, s)
                }
            },
            pick = pick@{ i ->
                val s = slots[i]
                if (s.isEmpty()) return@pick
                if (word) {
                    ime.insertWord(s)
                    // layers 2 and 3 are for one word unless they were opened with a flick
                    if (mode != 0 && !layerLocked) go(0)
                } else if (s == SHIFT) {
                    ime.toggleCase()
                } else {
                    ime.insertLetter(s)
                }
            }).also { it.dim = slots.all(String::isEmpty) }
    }

    private fun sideKey(face: String, glyph: Boolean = false, look: Look = Look.SIDE,
                        preview: (Int) -> Pair<String?, String>?, pick: (Int) -> Unit) =
        Key(listOf(face, "", "", "", ""), glyph, look, guide = false,
            mainSp = if (glyph) 28f else 15f, edgeSp = 13f, preview = preview, pick = pick)

    /** A side key that shows what it types: the tap in the middle, the flicks around it. */
    private fun symbolKey(faces: List<String>, preview: (Int) -> Pair<String?, String>?, pick: (Int) -> Unit) =
        Key(faces, false, Look.SIDE, guide = false, mainSp = 20f, edgeSp = 13f, preview = preview, pick = pick)

    /**
     * A layer key shows its layer's icon word; while that layer is open it becomes the way back to layer 1.
     * Tap: open the layer for one word. Flick: keep it open until the key is tapped again.
     */
    private fun layerKey(n: Int): Key {
        val open = mode == n
        val locked = open && layerLocked
        val word = LAYER_ICONS[if (open) 0 else n]
        val glyph = if (showGlyphs) data.glyphs[word] else null
        val look = if (!open) Look.SIDE else if (locked) Look.STRONG else Look.SOFT
        return sideKey(glyph ?: word, glyph != null, look,
            preview = { i -> if (i == 0) Pair(data.glyphs[word], word) else if (locked) null else Pair(null, "Keep open") },
            pick = { i -> if (i == 0) go(if (open) 0 else n) else if (!locked) go(n, locked = true) })
    }

    private fun backKey(): Key {
        val glyph = if (showGlyphs) data.glyphs["toki"] else null
        return sideKey(glyph ?: "toki", glyph != null, Look.STRONG,
            preview = { i -> if (i == 0) Pair(data.glyphs["toki"], "toki") else null },
            pick = { i -> if (i == 0) go(0) })
    }

    private fun lettersKey() = sideKey("ABC",
        preview = { i -> if (i == 0) Pair(null, "ABC") else null },
        pick = { i -> if (i == 0) go(3) })

    /** Tap: next keyboard. Flick: switch the key faces between glyphs and Latin letters. */
    private fun globeKey(): Key {
        val other = if (showGlyphs) "Latin" else "Glyphs"
        return sideKey("",
            preview = { i -> Pair(null, if (i == 0) "Next keyboard" else other) },
            pick = { i ->
                if (i == 0) {
                    ime.nextKeyboard()
                } else {
                    showGlyphs = !showGlyphs
                    prefs.edit().putBoolean("showGlyphs", showGlyphs).apply()
                    rebuild()
                }
            }).also { it.icon = Icon.GLOBE }
    }

    private fun deleteKey() = sideKey("",
        preview = { i -> Pair(null, if (i == 0) "1 letter" else "1 word") },
        pick = { i -> if (i == 0) ime.deleteChar() else ime.deleteWord() }).also {
        it.icon = Icon.DELETE
        it.repeat = ime::deleteChar
    }

    /** Tap: a space. Flick in any direction: a colon (as in "e ni:"). Only the left colon is drawn to keep the key quiet. */
    private fun spaceKey() = symbolKey(listOf("␣", ":", "", "", ""),
        preview = { i -> Pair(null, if (i == 0) "␣" else ":") },
        pick = { i -> if (i == 0) ime.insertText(" ") else ime.insertPunctuation(":") })

    private fun punctuationKey() = symbolKey(PUNCTUATION,
        preview = { i -> if (PUNCTUATION[i].isEmpty()) null else Pair(null, PUNCTUATION[i]) },
        pick = { i -> if (PUNCTUATION[i].isNotEmpty()) ime.insertPunctuation(PUNCTUATION[i]) })

    private fun returnKey() = sideKey("",
        preview = { i -> if (i == 0) Pair(null, "⏎") else null },
        pick = { i -> if (i == 0) ime.enter() }).also { it.icon = Icon.RETURN }

    // ---- Layout

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), dp(if (compact) 180f else 236f).toInt())
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) = layoutKeys()

    private fun layoutKeys() {
        val pad = dp(5f)
        val gap = dp(5f)
        val weights = floatArrayOf(0.7f, 1f, 1f, 1f, 0.7f)
        val total = min(width.toFloat(), dp(520f))
        val left = (width - total) / 2 + pad
        val unit = (total - pad * 2 - gap * 4) / weights.sum()
        val rowH = (height - pad * 2 - gap * 3) / 4
        keys.forEachIndexed { i, key ->
            val row = i / 5
            val col = i % 5
            var x = left
            for (j in 0 until col) x += weights[j] * unit + gap
            val y = pad + row * (rowH + gap)
            key.rect.set(x, y, x + weights[col] * unit, y + rowH)
        }
    }

    // ---- Touch

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                // Touching a new key while another is still held types the held one first.
                presses.values.toList().forEach { finish(it, commit = true) }
                val i = e.actionIndex
                val key = keyAt(e.getX(i), e.getY(i)) ?: return true
                val press = Press(key, e.getX(i), e.getY(i))
                presses[e.getPointerId(i)] = press
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                playSoundEffect(SoundEffectConstants.CLICK)
                shown = press
                if (key.repeat != null) startRepeat(press)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> for (i in 0 until e.pointerCount) {
                val press = presses[e.getPointerId(i)] ?: continue
                if (press.repeating) continue
                val d = direction(e.getX(i) - press.x0, e.getY(i) - press.y0, dp(TAP_RADIUS_DP))
                if (d != press.slot) {
                    press.slot = d
                    if (d != 0) stopRepeat()
                    shown = press
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP ->
                presses[e.getPointerId(e.actionIndex)]?.let { finish(it, commit = true) }
            MotionEvent.ACTION_CANCEL -> presses.values.toList().forEach { finish(it, commit = false) }
        }
        return true
    }

    /** Touches in the gaps between keys reach the nearest key. */
    private fun keyAt(x: Float, y: Float): Key? {
        val slack = dp(2.5f)
        return keys.firstOrNull { x >= it.rect.left - slack && x <= it.rect.right + slack && y >= it.rect.top - slack && y <= it.rect.bottom + slack }
    }

    private fun finish(press: Press, commit: Boolean) {
        if (!presses.entries.removeAll { it.value === press }) return
        stopRepeat()
        if (shown === press) shown = null
        invalidate()
        if (commit && !press.repeating) press.key.pick(press.slot)
    }

    /** Holding the key still repeats its action (used by delete). */
    private fun startRepeat(press: Press) {
        val action = press.key.repeat ?: return
        val task = object : Runnable {
            override fun run() {
                press.repeating = true
                action()
                handler.postDelayed(this, 80)
            }
        }
        repeatTask = task
        handler.postDelayed(task, 450)
    }

    private fun stopRepeat() {
        repeatTask?.let { handler.removeCallbacks(it) }
        repeatTask = null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopRepeat()
    }

    // ---- Drawing

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(cBoard)
        val held = presses.values.map { it.key }.toSet()
        for (key in keys) drawKey(canvas, key, key in held)
        shown?.let { if (it.key.guide) drawGuide(canvas, it) else drawBubble(canvas, it) }
    }

    private fun drawKey(canvas: Canvas, key: Key, pressed: Boolean) {
        val r = key.rect
        val radius = dp(6f)
        val (bg, fg) = when (key.look) {
            Look.KEY -> cKey to cText
            Look.SIDE -> cSide to cText
            Look.SOFT -> cPressed to cAccent
            Look.STRONG -> cAccent to cOnAccent
        }
        val save = if (key.dim) canvas.saveLayerAlpha(r.left, r.top, r.right, r.bottom + dp(1f), 89) else -1
        fill.color = cShadow
        canvas.drawRoundRect(r.left, r.top + dp(1f), r.right, r.bottom + dp(1f), radius, radius, fill)
        fill.color = if (!pressed) bg else if (key.look == Look.KEY) cPressed else cKey
        canvas.drawRoundRect(r, radius, radius, fill)

        key.icon?.let { drawIcon(canvas, it, r.centerX(), r.centerY(), size(10f), fg) }
        val edges = key.faces.drop(1)
        val hasEdges = edges.any { it.isNotEmpty() }
        val sideW = if (hasEdges) r.width() * 0.24f else dp(4f)
        val edgePx = size(key.edgeSp)
        label(canvas, key.faces[0], r.centerX(), r.centerY(), size(key.mainSp), key.glyph, true, fg, r.width() - sideW * 2)
        val edgeColor = if (key.look == Look.KEY || key.look == Look.SIDE) cMuted else fg
        label(canvas, edges[0], r.left + sideW / 2, r.centerY(), edgePx, key.glyph, false, edgeColor, sideW)
        label(canvas, edges[1], r.centerX(), r.top + edgePx * 0.85f, edgePx, key.glyph, false, edgeColor, r.width() - dp(8f))
        label(canvas, edges[2], r.right - sideW / 2, r.centerY(), edgePx, key.glyph, false, edgeColor, sideW)
        label(canvas, edges[3], r.centerX(), r.bottom - edgePx * 0.85f, edgePx, key.glyph, false, edgeColor, r.width() - dp(8f))
        if (save >= 0) canvas.restoreToCount(save)
    }

    /** Draws one line of text centered on (cx, cy), shrunk (to half size at most) to fit maxW. */
    private fun label(canvas: Canvas, s: String, cx: Float, cy: Float, px: Float, glyph: Boolean, bold: Boolean, color: Int, maxW: Float) {
        if (s.isEmpty()) return
        textPaint.typeface = if (glyph) data.glyphFont ?: Typeface.DEFAULT else if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        textPaint.color = color
        textPaint.textSize = px
        val w = textPaint.measureText(s)
        if (w > maxW && w > 0f) textPaint.textSize = px * max(0.5f, maxW / w)
        val fm = textPaint.fontMetrics
        // the sitelen pona glyphs sit high in their line, so they are nudged down
        val nudge = if (glyph) textPaint.textSize * 0.1f else 0f
        canvas.drawText(s, cx, cy - (fm.ascent + fm.descent) / 2 + nudge, textPaint)
    }

    /** The five slots of the held key as a cross around it, moved as little as needed to stay inside the keyboard. */
    private fun drawGuide(canvas: Canvas, press: Press) {
        val k = press.key.rect
        val cw = max(k.width(), dp(60f))
        val ch = max(k.height(), dp(52f))
        val gap = dp(3f)
        val w = cw * 3 + gap * 2
        val h = ch * 3 + gap * 2
        val x0 = max(min(k.centerX() - w / 2, width - w), 0f)
        val y0 = max(min(k.centerY() - h / 2, height - h), 0f)
        val grid = listOf(1 to 1, 0 to 1, 1 to 0, 2 to 1, 1 to 2)
        for (i in 0 until 5) {
            val content = press.key.preview(i) ?: continue
            val x = x0 + grid[i].first * (cw + gap)
            val y = y0 + grid[i].second * (ch + gap)
            cell(canvas, RectF(x, y, x + cw, y + ch), content, highlighted = i == press.slot, glyphSp = 22f, textSp = 12f)
        }
    }

    /** One bubble with the current slot, one key away in the flick direction (above the key for a tap). */
    private fun drawBubble(canvas: Canvas, press: Press) {
        val k = press.key.rect
        val offsets = listOf(0 to -1, -1 to 0, 0 to -1, 1 to 0, 0 to 1)
        val bw = max(k.width(), dp(64f))
        val bh = max(k.height(), dp(62f))
        val (ox, oy) = offsets[press.slot]
        val x = max(min(k.centerX() + ox * (k.width() + dp(4f)) - bw / 2, width - bw), 0f)
        val y = max(min(k.centerY() + oy * (k.height() + dp(4f)) - bh / 2, height - bh), 0f)
        cell(canvas, RectF(x, y, x + bw, y + bh), press.key.preview(press.slot) ?: Pair(null, "—"),
            highlighted = true, glyphSp = 28f, textSp = 15f)
    }

    /** Highlighted: filled with the accent color. Otherwise a light cell with an accent outline. */
    private fun cell(canvas: Canvas, r: RectF, content: Pair<String?, String>, highlighted: Boolean, glyphSp: Float, textSp: Float) {
        val radius = dp(8f)
        fill.color = if (highlighted) cAccent else cKey
        canvas.drawRoundRect(r, radius, radius, fill)
        if (!highlighted) {
            stroke.color = cAccent
            stroke.strokeWidth = dp(1f)
            canvas.drawRoundRect(r, radius, radius, stroke)
        }
        val color = if (highlighted) cOnAccent else cText
        val (glyph, text) = content
        val maxW = r.width() - dp(8f)
        if (glyph == null) {
            label(canvas, text, r.centerX(), r.centerY(), dp(textSp), false, true, color, maxW)
        } else {
            val textPx = dp(textSp)
            label(canvas, glyph, r.centerX(), r.centerY() - textPx * 0.6f, dp(glyphSp), true, false, color, maxW)
            label(canvas, text, r.centerX(), r.bottom - textPx * 0.9f, textPx, false, true, color, maxW)
        }
    }

    /** Line icons in the key's text color: a globe (not the colored emoji), delete and return. */
    private fun drawIcon(canvas: Canvas, icon: Icon, cx: Float, cy: Float, r: Float, color: Int) {
        stroke.color = color
        stroke.strokeWidth = dp(1.6f)
        path.reset()
        when (icon) {
            Icon.GLOBE -> {
                canvas.drawCircle(cx, cy, r, stroke)
                canvas.drawOval(cx - r * 0.45f, cy - r, cx + r * 0.45f, cy + r, stroke)
                canvas.drawLine(cx - r, cy, cx + r, cy, stroke)
                val half = r * sqrt(1f - 0.25f)   // half chord at 0.5 r from the center
                canvas.drawLine(cx - half, cy - r * 0.5f, cx + half, cy - r * 0.5f, stroke)
                canvas.drawLine(cx - half, cy + r * 0.5f, cx + half, cy + r * 0.5f, stroke)
            }
            Icon.DELETE -> {
                path.moveTo(cx - r, cy)
                path.lineTo(cx - r * 0.45f, cy - r * 0.6f)
                path.lineTo(cx + r, cy - r * 0.6f)
                path.lineTo(cx + r, cy + r * 0.6f)
                path.lineTo(cx - r * 0.45f, cy + r * 0.6f)
                path.close()
                canvas.drawPath(path, stroke)
                val x = cx + r * 0.25f
                val s = r * 0.25f
                canvas.drawLine(x - s, cy - s, x + s, cy + s, stroke)
                canvas.drawLine(x - s, cy + s, x + s, cy - s, stroke)
            }
            Icon.RETURN -> {
                path.moveTo(cx + r * 0.8f, cy - r * 0.6f)
                path.lineTo(cx + r * 0.8f, cy + r * 0.1f)
                path.lineTo(cx - r * 0.8f, cy + r * 0.1f)
                path.moveTo(cx - r * 0.45f, cy - r * 0.25f)
                path.lineTo(cx - r * 0.8f, cy + r * 0.1f)
                path.lineTo(cx - r * 0.45f, cy + r * 0.45f)
                canvas.drawPath(path, stroke)
            }
        }
    }
}
