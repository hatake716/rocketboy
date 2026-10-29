package io.github.hatake716.rocketboy

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Capture choreography only. The shipping APK has no promotional gameplay or debug controls. */
@RunWith(AndroidJUnit4::class)
class PromoCaptureTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)
    private lateinit var activity: MainActivity
    private val folder get() = File(context.getExternalFilesDir(null), "promo").apply { mkdirs() }
    private val clips = JSONArray()
    private var recordingStart = 0L
    private var cues = JSONArray()
    private fun main(action: (MainActivity) -> Unit) = instrumentation.runOnMainSync { action(activity) }
    private fun await(predicate: (MainActivity) -> Boolean) {
        repeat(200) {
            var done = false
            main { done = predicate(it) }
            if (done) return
            SystemClock.sleep(40)
        }
        fail("Capture state timeout")
    }
    private fun show(method: String) = main {
        MainActivity::class.java.getDeclaredMethod(method).apply { isAccessible = true }.invoke(it)
    }
    private fun cue(name: String) { cues.put(JSONObject().put("event", name).put("seconds", (SystemClock.elapsedRealtime() - recordingStart) / 1000.0)) }
    private fun record(name: String, seconds: Int, action: () -> Unit = {}) {
        val path = "/sdcard/Download/rocketboy-promo-$name.mp4"
        recordingStart = SystemClock.elapsedRealtime()
        cues = JSONArray()
        val stream = instrumentation.uiAutomation.executeShellCommand("screenrecord --size 1080x1920 --bit-rate 9000000 --time-limit $seconds $path")
        val reader = Thread { ParcelFileDescriptor.AutoCloseInputStream(stream).use { it.readBytes() } }.apply { start() }
        SystemClock.sleep(650)
        cue("ready")
        action()
        reader.join((seconds + 5L) * 1000)
        check(!reader.isAlive)
        clips.put(JSONObject().put("name", name).put("path", path).put("cues", cues))
        File(folder, "capture.json").writeText(clips.toString(2))
    }
    private fun type(word: String, delay: Long) {
        word.forEach { kana ->
            main { it.input.onCreateInputConnection(EditorInfo())!!.commitText(kana.toString(), 1) }
            SystemClock.sleep(delay)
        }
        // End-of-keystroke cue; the editor subtracts the known final key delay.
        cue("launch")
        await { a -> a.engine.meteors.none { it.targeted } }
        cue("intercept")
    }
    private fun begin(mode: GameMode) {
        show("showMenu")
        SessionStore(context).apply { playOptions = PlayOptions(mode); clearMission() }
        show("showHome")
        val start = requireNotNull(device.wait(Until.findObject(By.text("▶  ミッション開始")), 5000))
        main { a ->
            fun click(view: View): Boolean {
                if (view is Button && view.text.toString() == start.text) { view.performClick(); return true }
                if (view is android.view.ViewGroup) for (i in 0 until view.childCount) if (click(view.getChildAt(i))) return true
                return false
            }
            check(click(a.window.decorView))
        }
        await { it.engine.phase == Phase.RUNNING }
    }
    private fun prepare(meteors: List<Meteor>) = main { a ->
        a.engine.restore(a.engine.snapshot().copy(elapsed = 0.0, damage = 0, spawnIn = 100.0, nextQuoteAt = 100.0, meteors = meteors))
        a.engine.resume()
    }

    @Test fun recordTrailer() {
        check(android.os.Build.HARDWARE in listOf("ranchu", "goldfish")) { "Promotional capture is emulator-only" }
        Configurator.getInstance().waitForIdleTimeout = 0
        context.getSharedPreferences("rocketboy", Context.MODE_PRIVATE).edit().clear().commit()
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity = it }
        try {
            await { it.window.decorView.hasWindowFocus() }
            SystemClock.sleep(650)
            main { a ->
                val scene = MainActivity::class.java.getDeclaredField("scene").apply { isAccessible = true }.get(a) as View
                val bitmap = Bitmap.createBitmap(scene.width, scene.height, Bitmap.Config.ARGB_8888)
                scene.draw(Canvas(bitmap))
                File(folder, "title-art.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
                val xy = IntArray(2); scene.getLocationOnScreen(xy)
                File(folder, "title-bounds.json").writeText(JSONObject().put("x",xy[0]).put("y",xy[1]).put("width",scene.width).put("height",scene.height).toString())
            }
            record("title", 5)

            begin(GameMode.NORMAL)
            prepare(listOf(Meteor(600,"そら",0,.25,.0625), Meteor(601,"ほし",1,.34,.0625)))
            record("typing", 8) {
                SystemClock.sleep(350); type("そら", 240)
                SystemClock.sleep(750); type("ほし", 220)
                main { it.engine.meteors += Meteor(602,"ちきゅう",0,.2,.0625) }
                SystemClock.sleep(600); type("ちきゅう", 160)
            }

            val quote = Quotes.find("descartes-think")!!
            prepare(listOf(Meteor(700,quote.reading,0,.18,.04,quoteId=quote.id)))
            record("quote", 7) {
                SystemClock.sleep(600); type(quote.reading, 115)
                assertTrue(SessionStore(context).collectedQuoteIds.contains(quote.id))
            }

            show("showMenu")
            // Show category browsing with a small sample collection; no completed collection claim.
            context.getSharedPreferences("rocketboy", Context.MODE_PRIVATE).edit().putStringSet("collectedQuotes",
                QuoteCategory.entries.map { Quotes.inCategory(it).first().id }.toSet() + quote.id).commit()
            show("showQuotes")
            record("collection", 5) {
                SystemClock.sleep(1200)
                device.swipe(900,1560,900,920,35)
            }

            begin(GameMode.COMPETITIVE)
            prepare(listOf(Meteor(800,"ほし",0,.25,.09375), Meteor(801,"そら",1,.70,.09375)))
            record("competitive", 9) {
                SystemClock.sleep(250); type("ほし", 200)
                await { it.engine.phase == Phase.GAME_OVER }
                cue("game-over")
                await { it.findViewById<Button>(R.id.share_x_button)?.isEnabled == true }
                SystemClock.sleep(750)
                cue("result")
            }

            show("showMusicRoom")
            main { a ->
                fun find(view: View): Boolean {
                    if (view.contentDescription == "地球のBGMを再生") { view.performClick(); return true }
                    if (view is android.view.ViewGroup) for (i in 0 until view.childCount) if (find(view.getChildAt(i))) return true
                    return false
                }
                check(find(a.window.decorView))
            }
            await { it.audio.isMusicPlaying }
            record("music", 5) { SystemClock.sleep(1000); device.swipe(950,1560,950,1150,30) }
            show("showHome")
        } finally { device.pressHome(); scenario.close() }
    }
}
