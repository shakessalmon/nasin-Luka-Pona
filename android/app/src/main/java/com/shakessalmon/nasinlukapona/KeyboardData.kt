package com.shakessalmon.nasinlukapona

import android.content.Context
import android.graphics.Typeface
import org.json.JSONArray

/** data/layout.json and data/words.json, bundled as assets (see app/build.gradle.kts). */
class KeyboardData(context: Context) {
    /** layers[layer][key] = words as [tap, left, up, right, down] */
    val layers: List<List<List<String>>>

    /** word → its sitelen pona glyph (a UCSUR character) */
    val glyphs: Map<String, String>

    /** The bundled nasin nanpa subset, or null if it could not be loaded. */
    val glyphFont: Typeface?

    init {
        val assets = context.assets
        val layout = JSONArray(assets.open("layout.json").bufferedReader().use { it.readText() })
        layers = (0 until layout.length()).map { l ->
            val keys = layout.getJSONArray(l)
            (0 until keys.length()).map { k ->
                val w = keys.getJSONObject(k).getJSONArray("w")
                (0 until w.length()).map { w.getString(it) }
            }
        }
        val words = JSONArray(assets.open("words.json").bufferedReader().use { it.readText() })
        val map = HashMap<String, String>()
        for (i in 0 until words.length()) {
            val w = words.getJSONObject(i)
            val ucsur = w.optString("ucsur")
            if (!ucsur.startsWith("U+")) continue
            val cp = ucsur.substring(2).toIntOrNull(16) ?: continue
            map[w.getString("word")] = String(Character.toChars(cp))
        }
        glyphs = map
        glyphFont = try {
            Typeface.createFromAsset(assets, "nasin-nanpa-subset.otf")
        } catch (e: RuntimeException) {
            null
        }
    }
}
