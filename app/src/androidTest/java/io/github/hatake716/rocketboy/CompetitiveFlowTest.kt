package io.github.hatake716.rocketboy

import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class CompetitiveFlowTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)
    private val pkg = context.packageName
    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var activity: MainActivity

    @Before fun launch() {
        Configurator.getInstance().waitForIdleTimeout = 0
        context.getSharedPreferences("rocketboy", Context.MODE_PRIVATE).edit().clear().commit()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity = it }
        assertTrue(device.wait(Until.hasObject(By.res(pkg, "play_options_button")), 5000))
    }

    @After fun close() { device.pressHome(); scenario.close() }
    private fun main(action: (MainActivity) -> Unit) = instrumentation.runOnMainSync { action(activity) }
    private fun await(condition: (MainActivity) -> Boolean) {
        repeat(200) {
            var done = false
            main { done = condition(it) }
            if (done) return
            SystemClock.sleep(40)
        }
        fail("Game state did not settle")
    }
    private fun tap(id: String) {
        await { it.window.decorView.hasWindowFocus() }
        val resource = "$pkg:id/$id"
        UiScrollable(UiSelector().scrollable(true)).let {
            if (device.hasObject(By.scrollable(true))) it.scrollIntoView(UiSelector().resourceId(resource))
        }
        device.waitForIdle(1000)
        repeat(10) {
            val bounds = requireNotNull(device.wait(Until.findObject(By.res(resource)), 3000)).visibleBounds
            SystemClock.sleep(350)
            if (device.findObject(By.res(resource))?.visibleBounds == bounds) {
                assertTrue(device.click(bounds.centerX(), bounds.centerY()))
                return
            }
        }
        fail("Control moved during tap: $id")
    }
    private fun shot(name: String) {
        val path = File(context.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        assertTrue(device.takeScreenshot(File(path, "$name.png")))
    }
    private fun startCompetition() {
        tap("play_options_button"); tap("competitive_mode_button")
        main { a ->
            assertEquals("WAVE 1 固定", a.findViewById<TextView>(R.id.start_level_label).text.toString())
            assertFalse(a.findViewById<Button>(R.id.level_plus_button).isEnabled)
            assertFalse(a.findViewById<Button>(R.id.level_minus_button).isEnabled)
        }
        shot("70-competitive-settings")
        tap("start_configured_button")
        await { it.engine.phase == Phase.RUNNING }
        main { a ->
            assertEquals(PlayOptions(GameMode.COMPETITIVE), a.engine.options)
            assertEquals(1, a.engine.lives); assertEquals(0L, a.engine.score)
        }
    }

    @Suppress("DEPRECATION")
    @Test fun quoteBurstOneHitGameOverImageShareAndResultRecreation() {
        startCompetition()
        await { it.engine.meteors.isNotEmpty() }
        main { assertEquals(.09375, it.engine.meteors.first().speed, 0.0) }
        // Prepare a reproducible quote to record the actual renderer and IME-to-rocket path.
        val quote = Quotes.find("descartes-think")!!
        main { a ->
            a.engine.restore(a.engine.snapshot().copy(spawnIn = 100.0,
                meteors = listOf(Meteor(900, quote.reading, 0, .35, .04, quoteId = quote.id))))
            a.engine.resume()
        }
        SystemClock.sleep(450); shot("71-quote-before-hit")
        main { it.input.onCreateInputConnection(EditorInfo())!!.commitText(quote.reading, 1) }
        await { it.engine.score == quote.reading.length * 303L }
        SystemClock.sleep(200); shot("72-quote-supernova")
        SystemClock.sleep(1200)
        main { a ->
            assertEquals(1, a.engine.lives)
            a.engine.meteors += Meteor(901, "そら", 0, .999, a.engine.difficulty.fallSpeed)
        }
        await { it.findViewById<View>(R.id.competitive_score_card) != null }
        await { it.findViewById<Button>(R.id.share_x_button).isEnabled }
        assertEquals(GameMode.COMPETITIVE, SessionStore(context).topScores.single().mode)
        assertNull(SessionStore(context).load())
        main { assertEquals(Phase.GAME_OVER, it.engine.phase); assertEquals(0, it.engine.lives) }
        SystemClock.sleep(450); shot("73-competitive-result")
        val beforeCaption = activity.findViewById<TextView>(R.id.share_caption).text.toString()
        scenario.recreate(); scenario.onActivity { activity = it }
        await { it.findViewById<Button>(R.id.share_x_button)?.isEnabled == true }
        main { assertEquals(beforeCaption, it.findViewById<TextView>(R.id.share_caption).text.toString()) }
        assertEquals(1, SessionStore(context).topScores.size)

        var captured: Intent? = null
        val monitor = object : Instrumentation.ActivityMonitor() {
            override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
                captured = intent
                return Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null)
            }
        }
        instrumentation.addMonitor(monitor)
        try { tap("share_x_button"); SystemClock.sleep(200) }
        finally { instrumentation.removeMonitor(monitor) }
        val outer = requireNotNull(captured)
        val payload = if (outer.action == Intent.ACTION_CHOOSER) outer.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!! else outer
        assertEquals(Intent.ACTION_SEND, payload.action)
        assertEquals("image/png", payload.type)
        assertEquals(beforeCaption, payload.getStringExtra(Intent.EXTRA_TEXT))
        assertTrue(beforeCaption.contains("#rocketboy"))
        val uri = requireNotNull(payload.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
        assertEquals("content", uri.scheme)
        assertEquals("$pkg.score-images", uri.authority)
        assertEquals(uri, payload.clipData!!.getItemAt(0).uri)
        assertTrue(payload.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        context.contentResolver.openInputStream(uri)!!.use { stream ->
            File(context.getExternalFilesDir(null), "screenshots/74-shared-score.png").outputStream().use { stream.copyTo(it) }
        }
        // No X post: deliver the identical payload to a separate test application instead.
        val receiver = Intent(payload).setPackage(null).setComponent(ComponentName("$pkg.test", ScoreShareReceiverActivity::class.java.name))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(receiver)
        val received = requireNotNull(device.wait(Until.findObject(By.textStartsWith("IMAGE RECEIVED 960x1080")), 5000))
        assertTrue(received.text.contains(beforeCaption))
        assertFalse(received.text.contains("UID ${android.os.Process.myUid()}\n"))
        SystemClock.sleep(700); shot("75-external-image-receiver")
        device.pressBack()
        assertTrue(device.wait(Until.hasObject(By.res(pkg, "share_x_button")), 5000))
        tap("share_x_button")
        assertTrue(device.wait(Until.gone(By.res(pkg, "share_x_button")), 5000))
        SystemClock.sleep(800); shot("76-android-share-sheet")
        device.pressBack()
        tap("retry_button")
        await { it.engine.phase == Phase.RUNNING }
        main { assertEquals(1, it.engine.lives); assertEquals(0L, it.engine.score); assertEquals(1, it.engine.wave) }
    }

    @Test fun competitivePauseRestoreAndSwitchingModesKeepTheirOwnRules() {
        startCompetition()
        main { a ->
            a.engine.meteors.clear()
            a.engine.meteors += Meteor(500, "ほし", 0, .2, a.engine.difficulty.fallSpeed)
            a.findViewById<Button>(R.id.pause_button).performClick()
        }
        val saved = requireNotNull(SessionStore(context).load())
        assertEquals(PlayOptions(GameMode.COMPETITIVE), saved.options)
        assertEquals(0, saved.damage)
        scenario.recreate(); scenario.onActivity { activity = it }
        val resume = requireNotNull(device.wait(Until.findObject(By.text("▶  ミッションを再開")), 5000))
        device.click(resume.visibleBounds.centerX(), resume.visibleBounds.centerY())
        await { it.engine.phase == Phase.RUNNING }
        main { assertEquals(1, it.engine.lives); assertEquals(saved.meteors.single().speed, it.engine.meteors.single().speed, 0.0) }
        tap("pause_button")
        val home = device.findObject(By.text("保存してタイトルへ"))
        device.click(home.visibleBounds.centerX(), home.visibleBounds.centerY())
        tap("play_options_button"); tap("normal_mode_button"); tap("level_plus_button")
        main { assertEquals("WAVE 2", it.findViewById<TextView>(R.id.start_level_label).text.toString()) }
        tap("competitive_mode_button"); tap("quote_mode_button")
        main { assertEquals("WAVE 2", it.findViewById<TextView>(R.id.start_level_label).text.toString()) }
        tap("start_configured_button")
        await { it.engine.phase == Phase.RUNNING }
        main { assertEquals(PlayOptions(GameMode.QUOTES, 2), it.engine.options); assertEquals(3, it.engine.lives) }
        await { it.engine.meteors.isNotEmpty() }
        main { assertNotNull(it.engine.meteors.single().quote) }
    }
}
