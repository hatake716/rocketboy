package io.github.hatake716.rocketboy

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.ProgressBar
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class CelestialPaletteTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)
    private val folder get() = File(context.getExternalFilesDir(null), "palettes").apply { mkdirs() }
    private fun save(image: Bitmap, name: String) {
        File(folder, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    private fun screenColor(color: Int) = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565).let {
        it.eraseColor(color); val result = it.getPixel(0, 0); it.recycle(); result
    }
    private fun render(scene: PixelScene) = Bitmap.createBitmap(320, 260, Bitmap.Config.ARGB_8888).also { scene.draw(Canvas(it)) }

    @Test fun all26PalettesRenderWordsQuotesAndIndependentScoreCards() {
        instrumentation.runOnMainSync {
            val selected = setOf("CADET", "EARTH", "MARS", "SATURN", "NEPTUNE", "PLUTO")
            val preview = Bitmap.createBitmap(960, 604, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(preview)
            val title = Paint().apply { typeface = context.resources.getFont(R.font.dot_gothic); textSize = 16f }
            var tile = 0
            val quote = Quotes.all.first { it.reading.length > 32 }
            for (colors in CelestialPalettes.all) {
                val game = GameEngine(101).apply {
                    restore(snapshot().copy(score = colors.rank.threshold, elapsed = 225.0,
                        meteors = listOf(Meteor(1, "ぼうけんのはじまり", 0, 0.0, 0.0), Meteor(2, "そら", 1, .3, 0.0))))
                }
                val scene = PixelScene(context).apply {
                    engine = game; typed = "ぼうけんのはじ"; layout(0, 0, 320, 260)
                }
                val image = render(scene)
                assertEquals(colors.rank.name, screenColor(colors.ink), image.getPixel(5, 5))
                assertEquals("${colors.rank.name}: second-row underline", screenColor(colors.light), image.getPixel(51, 122))
                if (colors.rank.english in selected) {
                    val x = (tile % 3) * 320f; val y = (tile / 3) * 302f
                    title.color = colors.ink; canvas.drawRect(x, y, x + 320, y + 42, title)
                    title.color = colors.light
                    canvas.drawText("${colors.rank.name}  ${colors.rank.threshold} PT", x + 12, y + 19, title)
                    title.color = colors.muted; canvas.drawText(colors.motif, x + 12, y + 37, title)
                    canvas.drawBitmap(image, x, y + 42, null)
                    tile++
                }
                image.recycle()
                game.meteors.clear()
                game.meteors += Meteor(3, quote.reading, 0, 0.0, 0.0, quoteId = quote.id)
                scene.typed = quote.reading.take(17)
                val quoted = render(scene)
                assertEquals("${colors.rank.name}: quote underline", screenColor(colors.light), quoted.getPixel(33, 90))
                quoted.recycle()

                val result = CompetitiveResult("palette", colors.rank.threshold, 10, 225, 20, 3, false)
                val card = CompetitiveScoreCard(context, result)
                val shot = card.screenshot()
                assertEquals(screenColor(colors.ink), shot.getPixel(15, 15))
                val other = CompetitiveScoreCard(context, result.copy(score = 0)).screenshot()
                val again = card.screenshot()
                assertTrue("Another card cannot recolor this result", shot.sameAs(again))
                shot.recycle(); other.recycle(); again.recycle()
            }
            assertEquals(6, tile)
            save(preview, "palette-preview"); preview.recycle()
        }
    }

    private lateinit var activity: MainActivity
    private fun main(action: (MainActivity) -> Unit) = instrumentation.runOnMainSync { action(activity) }
    private fun await(message: String, condition: (MainActivity) -> Boolean) {
        repeat(180) {
            var done = false
            main { done = condition(it) }
            if (done) return
            SystemClock.sleep(40)
        }
        fail(message)
    }
    private fun click(text: String) {
        val bounds = requireNotNull(device.wait(Until.findObject(By.text(text)), 5000)).visibleBounds
        device.click(bounds.centerX(), bounds.centerY())
    }
    private fun shot(name: String) { SystemClock.sleep(180); assertTrue(device.takeScreenshot(File(folder, "$name.png"))) }
    private fun start() {
        click("▶  ミッション開始")
        await("Mission starts") { it.engine.phase == Phase.RUNNING }
        if (device.hasObject(By.text("次へ"))) { click("次へ"); SystemClock.sleep(500) }
        if (device.hasObject(By.text("完了"))) click("完了")
    }
    private fun allViews(view: View): List<View> = listOf(view) +
        if (view is ViewGroup) (0 until view.childCount).flatMap { allViews(view.getChildAt(it)) } else emptyList()
    private fun assertGameColors(a: MainActivity, expected: PixelPalette) {
        assertEquals(expected, a.palette)
        assertEquals(expected, a.findViewById<PixelScene>(R.id.game_scene).palette)
        assertEquals(expected.light, a.input.currentTextColor)
        assertEquals(expected.mid, a.input.currentHintTextColor)
        assertEquals(expected.ink, a.findViewById<Button>(R.id.fire_button).currentTextColor)
        assertEquals(expected.light, a.findViewById<Button>(R.id.pause_button).currentTextColor)
        val progress = allViews(a.window.decorView).filterIsInstance<ProgressBar>().single()
        assertEquals(expected.light, progress.progressTintList!!.defaultColor)
        val root = a.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
        assertEquals(expected.ink, (root.background as ColorDrawable).color)
    }

    @Test fun everyRankRecolorsTheLiveUiWithoutLosingComposingText() {
        Configurator.getInstance().waitForIdleTimeout = 0
        context.getSharedPreferences("rocketboy", Context.MODE_PRIVATE).edit().clear().commit()
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity = it }
        try {
            start()
            lateinit var originalInput: KanaInput
            main { a ->
                originalInput = a.input
                a.input.onCreateInputConnection(EditorInfo())!!.setComposingText("ぼうけんのはじ", 1)
            }
            for (colors in CelestialPalettes.all) {
                main { a ->
                    a.engine.restore(a.engine.snapshot().copy(score = colors.rank.threshold, spawnIn = 100.0, nextQuoteAt = 1000.0,
                        meteors = listOf(Meteor(1, "ぼうけんのはじまり", 0, .15, 0.0))))
                    a.engine.resume()
                }
                await("Palette ${colors.rank.name}") { it.palette == colors }
                main { a ->
                    assertGameColors(a, colors)
                    assertSame(originalInput, a.input)
                    assertEquals("ぼうけんのはじ", a.input.text.toString())
                    assertEquals(0, BaseInputConnection.getComposingSpanStart(a.input.text))
                    assertTrue(a.input.hasFocus())
                }
                if (colors.rank.english in setOf("CADET", "EARTH", "MARS", "SATURN", "NEPTUNE", "PLUTO")) shot("game-${colors.rank.english.lowercase()}")
            }
        } finally { device.pressHome(); scenario.close() }
    }

    @Test fun rankUpPauseRestoreMultiRankJumpAndRetryKeepTheCorrectColors() {
        Configurator.getInstance().waitForIdleTimeout = 0
        context.getSharedPreferences("rocketboy", Context.MODE_PRIVATE).edit().clear().putLong("bestScore", 100_000).commit()
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity = it }
        try {
            main { a -> assertEquals("PLUTO", a.palette.rank.english) }
            shot("home-pluto")
            start()
            main { a ->
                assertGameColors(a, CelestialPalettes.cadet)
                a.engine.restore(a.engine.snapshot().copy(score = 3990, spawnIn = 100.0, nextQuoteAt = 1000.0,
                    meteors = listOf(Meteor(1, "そら", 0, .2, 0.0))))
                a.engine.resume()
                a.input.onCreateInputConnection(EditorInfo())!!.commitText("そら", 1)
            }
            await("Cross the 4000-point rank boundary") { it.engine.score == 4010L && it.palette.rank.english == "MERCURY" }
            main { a -> assertGameColors(a, CelestialPalettes.forScore(4010)); a.findViewById<Button>(R.id.pause_button).performClick() }
            await("Pause") { it.engine.phase == Phase.PAUSED }
            main { assertEquals("MERCURY", it.palette.rank.english) }
            scenario.recreate(); scenario.onActivity { activity = it }
            main { assertEquals("PLUTO", it.palette.rank.english) }
            click("▶  ミッションを再開")
            await("Restore the mission palette") { it.engine.phase == Phase.RUNNING && it.palette.rank.english == "MERCURY" }
            val quote = Quotes.all.maxBy { it.reading.length }
            val targetScore = 11990L + Scoring.earned(quote.points, 1)
            main { a ->
                a.engine.restore(a.engine.snapshot().copy(score = 11990, spawnIn = 100.0, nextQuoteAt = 1000.0,
                    meteors = listOf(Meteor(2, quote.reading, 0, .2, 0.0, quoteId = quote.id))))
                a.engine.resume()
            }
            await("Venus before a multi-rank jump") { it.palette.rank.english == "VENUS" }
            main { it.input.onCreateInputConnection(EditorInfo())!!.commitText(quote.reading, 1) }
            await("Quote crosses multiple ranks") { it.engine.score == targetScore && it.palette == CelestialPalettes.forScore(targetScore) }
            main { a -> assertGameColors(a, CelestialPalettes.forScore(targetScore)); a.findViewById<Button>(R.id.pause_button).performClick() }
            click("この挑戦を終了")
            main { assertEquals(CelestialPalettes.forScore(targetScore), it.palette) }
            shot("result-current-rank")
            click("▶  もう一度")
            await("Retry returns to green") { it.engine.phase == Phase.RUNNING && it.engine.score == 0L }
            main { assertGameColors(it, CelestialPalettes.cadet) }
        } finally { device.pressHome(); scenario.close() }
    }

    @Test fun competitiveResultRestoresItsOwnRankColorAndRetryReturnsToGreen() {
        Configurator.getInstance().waitForIdleTimeout = 0
        context.getSharedPreferences("rocketboy", Context.MODE_PRIVATE).edit().clear()
            .putLong("bestScore", 100_000).putString("mode", "COMPETITIVE").commit()
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity = it }
        try {
            start()
            main { a ->
                a.engine.restore(a.engine.snapshot().copy(score = 20_000, spawnIn = 100.0, nextQuoteAt = 1000.0,
                    meteors = listOf(Meteor(1, "あ", 0, .999, 1.0))))
                a.engine.resume()
            }
            await("Competitive result") { it.findViewById<Button>(R.id.share_x_button)?.isEnabled == true }
            fun checkCard() = main { a ->
                assertEquals("MARS", a.palette.rank.english)
                val image = a.findViewById<CompetitiveScoreCard>(R.id.competitive_score_card).screenshot()
                assertEquals(screenColor(CelestialPalettes.forScore(20_000).ink), image.getPixel(15, 15))
                save(image, "competitive-mars-card"); image.recycle()
            }
            checkCard()
            scenario.recreate(); scenario.onActivity { activity = it }
            await("Recreated competitive result") { it.findViewById<Button>(R.id.share_x_button)?.isEnabled == true }
            checkCard()
            shot("competitive-mars")
            main { it.findViewById<Button>(R.id.retry_button).performClick() }
            await("Competitive retry") { it.engine.phase == Phase.RUNNING && it.engine.score == 0L }
            main { assertGameColors(it, CelestialPalettes.cadet) }
        } finally { device.pressHome(); scenario.close() }
    }
}
