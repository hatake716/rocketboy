package io.github.hatake716.rocketboy

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Check the pixels drawn by the real View, including the RGB_565 game buffer. */
@RunWith(AndroidJUnit4::class)
class TypingUnderlineTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private fun screenColor(color: Int): Int = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565).let {
        it.eraseColor(color)
        val result = it.getPixel(0, 0)
        it.recycle()
        result
    }

    private fun render(scene: PixelScene): Bitmap =
        Bitmap.createBitmap(scene.width, scene.height, Bitmap.Config.ARGB_8888).also {
            scene.draw(Canvas(it))
        }

    private fun checkRows(scene: PixelScene, word: String, firstUnderlineY: Int, center: Int) {
        val rows = if (word.length > 8) word.chunked(5) else listOf(word)
        val light = screenColor(CelestialPalettes.cadet.light)
        scene.typed = ""
        val empty = render(scene)
        // Advance through every boundary, then backspace through them again.
        for (length in (0..word.length) + (word.length - 1 downTo 0)) {
            scene.typed = word.take(length)
            val actual = render(scene)
            var remaining = length
            rows.forEachIndexed { row, line ->
                val matched = remaining.coerceIn(0, line.length)
                val left = center - line.length * 8
                val y = firstUnderlineY + row * 16
                for (dy in 0..1) for (dx in -1..line.length * 16) {
                    val x = left + dx
                    val expected = if (dx in 0 until matched * 16) light else empty.getPixel(x, y + dy)
                    assertEquals("word=$word height=${scene.height} prefix=$length row=${row + 1} x=$x y=${y + dy}",
                        expected, actual.getPixel(x, y + dy))
                }
                remaining -= line.length
            }
            actual.recycle()
        }
        // Kana normalization must keep the same progress across line breaks.
        scene.typed = word.take(word.length - 1)
        val hiragana = render(scene)
        scene.typed = scene.typed.map { (it.code + 0x60).toChar() }.joinToString("")
        val katakana = render(scene)
        assertEquals(true, hiragana.sameAs(katakana))
        hiragana.recycle(); katakana.recycle()

        // A typo or empty input must clear all of the previous row highlights.
        for (prefix in listOf(word.dropLast(1) + "ん", "")) {
            scene.typed = prefix
            val actual = render(scene)
            assertEquals("Stale highlight after '$prefix'", true, empty.sameAs(actual))
            actual.recycle()
        }
        scene.engine!!.meteors.single().targeted = true
        scene.typed = ""
        val locked = render(scene)
        scene.typed = word
        val lockedWithInput = render(scene)
        assertEquals("An already targeted meteor must not highlight", true, locked.sameAs(lockedWithInput))
        locked.recycle(); lockedWithInput.recycle(); empty.recycle()
    }

    @Test fun wrappedWordsUnderlineEveryRowAndClearWhenEditing() {
        instrumentation.runOnMainSync {
            // Real 9-character word: the final four-character row is centered separately.
            // Also exercise the supported 16-character limit, including a short fourth row.
            val cases = listOf(
                Triple("ぼうけんのはじまり", 106, 88),
                Triple("うちゅうぼうえいたい", 106, 88),
                Triple("あいうえおかきくけこさし", 106, 107),
                Triple("あいうえおかきくけこさしすせそた", 116, 126),
            )
            for ((word, tallY, compactY) in cases) for (height in listOf(400, 220)) for (lane in 0..1) {
                val scene = PixelScene(instrumentation.targetContext).apply {
                    engine = GameEngine(101).apply { meteors += Meteor(1, word, lane, 0.0, 0.0) }
                    layout(0, 0, 320, height)
                }
                checkRows(scene, word, if (height == 400) tallY else compactY, if (lane == 0) 82 else 238)
            }
        }
    }

    @Test fun singleLineUnderlineStillTracksInput() {
        instrumentation.runOnMainSync {
            for (height in listOf(400, 220)) {
                val scene = PixelScene(instrumentation.targetContext).apply {
                    engine = GameEngine(101).apply { meteors += Meteor(1, "ちきゅう", 0, 0.0, 0.0) }
                    layout(0, 0, 320, height)
                }
                checkRows(scene, "ちきゅう", if (height == 400) 86 else 69, 82)
            }
        }
    }
}
