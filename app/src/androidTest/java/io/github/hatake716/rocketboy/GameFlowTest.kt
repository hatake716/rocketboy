package io.github.hatake716.rocketboy

import android.content.Context
import android.os.SystemClock
import android.view.inputmethod.EditorInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class GameFlowTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)
    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var activity: MainActivity
    private val pkg = "io.github.hatake716.rocketboy"

    @Before fun launch() {
        // A moving game is intentionally not idle. Synchronize on game state, not animations.
        Configurator.getInstance().waitForIdleTimeout = 0
        context.getSharedPreferences("rocketboy", Context.MODE_PRIVATE).edit().clear().commit()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity = it }
        device.wait(Until.hasObject(By.text("▶  ミッション開始")), 5000)
    }

    private fun onActivity(action: (MainActivity) -> Unit) {
        instrumentation.runOnMainSync { action(activity) }
    }

    @After fun close() {
        // Close the real IME and pause the frame loop before ActivityScenario waits for idle.
        device.pressHome()
        scenario.close()
    }

    private fun start() {
        device.findObject(By.text("▶  ミッション開始")).click()
        await { it.engine.phase == Phase.RUNNING }
    }

    private fun await(timeout: Long = 6000, predicate: (MainActivity) -> Boolean) {
        val until = SystemClock.uptimeMillis() + timeout
        while (SystemClock.uptimeMillis() < until) {
            var passed = false
            onActivity { passed = predicate(it) }
            if (passed) return
            SystemClock.sleep(40)
        }
        onActivity { assertTrue("Timed out waiting for game state", predicate(it)) }
    }

    private fun seed(word: String, score: Long = 0) {
        onActivity { a ->
            val old = a.engine.snapshot()
            a.engine.restore(old.copy(score = score, damage = 0,
                meteors = listOf(Meteor(300, word, 0, .3, .037))))
            a.engine.resume()
        }
    }

    private fun screenshot(name: String) {
        // Let the updated score and burst reach the display compositor before capture.
        SystemClock.sleep(350)
        val folder = File(context.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        val file = File(folder, "$name.png")
        assertTrue(device.takeScreenshot(file))
        // Gradle removes test APKs after the run; preserve evidence in the emulator's Downloads.
        device.executeShellCommand("mkdir -p /sdcard/Download/rocketboy-validation")
        device.executeShellCommand("cp ${file.absolutePath} /sdcard/Download/rocketboy-validation/$name.png")
    }

    @Test fun homeAndKeyboardRemainVisibleInPortrait() {
        screenshot("01-title")
        start()
        onActivity { a ->
            assertEquals(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, a.requestedOrientation)
            assertTrue(a.input.isShown)
            assertTrue(a.input.hasFocus())
            val visible = android.graphics.Rect()
            assertTrue(a.input.getGlobalVisibleRect(visible))
            assertTrue(visible.height() >= a.input.height)
            assertTrue(a.findViewById<android.view.View>(R.id.game_scene).height > 150)
        }
        screenshot("02-keyboard")
    }

    @Test fun composingDakutenDoesNotFireUntilCommitted() {
        start()
        seed("が")
        onActivity { a ->
            val connection = a.input.onCreateInputConnection(EditorInfo())!!
            connection.setComposingText("か", 1)
            connection.setComposingText("が", 1)
        }
        SystemClock.sleep(200)
        onActivity { a ->
            assertEquals(0L, a.engine.score)
            assertFalse(a.engine.meteors.first().targeted)
            a.input.onCreateInputConnection(EditorInfo())!!.finishComposingText()
        }
        await { it.engine.score == 1L }
        onActivity { assertTrue(it.input.text.isEmpty()) }
    }

    @Test fun explicitFireAcceptsCompositionAndDoesNotRewardMistakes() {
        start()
        seed("ちきゅう")
        onActivity { a -> a.input.onCreateInputConnection(EditorInfo())!!.setComposingText("ち", 1) }
        onActivity { it.findViewById<android.widget.Button>(R.id.fire_button).performClick() }
        onActivity { assertEquals(0L, it.engine.score); assertEquals(3, it.engine.lives) }
        onActivity { a -> a.input.onCreateInputConnection(EditorInfo())!!.setComposingText("ちきゅう", 1) }
        onActivity { it.findViewById<android.widget.Button>(R.id.fire_button).performClick() }
        await { it.engine.score == 4L }
        onActivity { assertTrue(it.input.text.isEmpty()) }
        screenshot("03-interception")
    }

    @Test fun commitTextAutoFiresAndRepeatedFinishDoesNotDuplicateScore() {
        start()
        seed("ぎゃく")
        onActivity { a ->
            val connection = a.input.onCreateInputConnection(EditorInfo())!!
            connection.commitText("ｷﾞｬｸ", 1)
            connection.finishComposingText()
            connection.finishComposingText()
        }
        await { it.engine.score == 3L }
        SystemClock.sleep(300)
        onActivity { assertEquals(3L, it.engine.score); assertEquals(1, it.engine.destroyed) }
    }

    @Test fun thirdImpactShowsGameOverAndRetryResetsMission() {
        start()
        seed("そら", 36)
        repeat(3) { index ->
            onActivity { a ->
                a.engine.meteors.clear()
                a.engine.meteors += Meteor(500 + index, "あ", 0, .9999, .1)
            }
            await { it.engine.damage == index + 1 }
        }
        assertTrue(device.wait(Until.hasObject(By.text("GAME OVER")), 3000))
        screenshot("04-game-over")
        assertNull(SessionStore(context).load())
        assertEquals(36L, SessionStore(context).bestScore)
        device.findObject(By.res(pkg, "retry_button")).click()
        await { it.engine.phase == Phase.RUNNING }
        onActivity { assertEquals(0L, it.engine.score); assertEquals(3, it.engine.lives) }
    }

    @Test fun pauseFreezesTimeAndRecreatesASuspendedMission() {
        start()
        seed("りゅうせい", 48)
        device.findObject(By.res(pkg, "pause_button")).click()
        await { it.engine.phase == Phase.PAUSED }
        var elapsed = 0.0
        onActivity { elapsed = it.engine.elapsed }
        SystemClock.sleep(400)
        onActivity { assertEquals(elapsed, it.engine.elapsed, 0.0) }
        screenshot("05-pause")
        scenario.recreate()
        scenario.onActivity { activity = it }
        assertTrue(device.wait(Until.hasObject(By.text("▶  ミッションを再開")), 3000))
        device.findObject(By.text("▶  ミッションを再開")).click()
        await { it.engine.phase == Phase.RUNNING }
        onActivity { a ->
            assertEquals(48L, a.engine.score)
            assertEquals("りゅうせい", a.engine.meteors.first().word)
            assertEquals(3, a.engine.lives)
        }
    }

    @Test fun backgroundStopsMissionUntilExplicitResume() {
        start()
        device.pressHome()
        await { it.engine.phase == Phase.PAUSED }
        var elapsed = 0.0
        onActivity { elapsed = it.engine.elapsed }
        SystemClock.sleep(300)
        context.startActivity(context.packageManager.getLaunchIntentForPackage(pkg))
        assertTrue(device.wait(Until.hasObject(By.text("MISSION PAUSED")), 4000))
        onActivity { assertEquals(elapsed, it.engine.elapsed, 0.0) }
        device.findObject(By.res(pkg, "resume_button")).click()
        await { it.engine.phase == Phase.RUNNING }
    }

    @Test fun musicActuallyPlaysAndStopsOnPause() {
        start()
        val manager = context.getSystemService(android.media.AudioManager::class.java)
        await { manager.activePlaybackConfigurations.isNotEmpty() }
        seed("あ")
        onActivity { a -> a.input.onCreateInputConnection(EditorInfo())!!.commitText("あ", 1) }
        await { it.engine.score == 1L }
        device.findObject(By.res(pkg, "pause_button")).click()
        await { manager.activePlaybackConfigurations.isEmpty() }
    }

    @Test fun gboardTwelveKeyTapCommitsKanaAndLaunches() {
        val ime = android.provider.Settings.Secure.getString(context.contentResolver, "default_input_method")
        org.junit.Assume.assumeTrue("Requires Gboard configured for Japanese 12-key input", ime.contains("com.google.android.inputmethod.latin"))
        start()
        seed("あ")
        onActivity { a ->
            a.engine.meteors += Meteor(301, "ほし", 1, .45, .037)
        }
        var keyboardTop = 0
        onActivity { a ->
            val location = IntArray(2)
            a.input.getLocationOnScreen(location)
            keyboardTop = location[1] + a.input.height
        }
        val keyboardHeight = device.displayHeight - keyboardTop
        // Real touch input to Gboard's 12-key 'あ', then its send key. No text injection.
        device.click((device.displayWidth * .30).toInt(), keyboardTop + (keyboardHeight * .25).toInt())
        await { it.input.text.toString() == "あ" || it.engine.score == 1L }
        device.click((device.displayWidth * .90).toInt(), keyboardTop + (keyboardHeight * .77).toInt())
        await { it.engine.score == 1L }
        screenshot("09-gboard-real-input")
    }

    @Test fun rankUpAtExactly100000AndBestRecordDoesNotAccumulateRuns() {
        start()
        seed("あ", 99999)
        onActivity { a -> a.input.onCreateInputConnection(EditorInfo())!!.commitText("あ", 1) }
        await { it.engine.score == 100000L }
        assertTrue(device.wait(Until.hasObject(By.text("RANK  冥王星  /  SOLAR SYSTEM COMPLETE")), 2000))
        onActivity { SessionStore(context).save(it.engine) }
        assertEquals(100000L, SessionStore(context).bestScore)
        val nextRun = GameEngine().apply { restore(snapshot().copy(score = 5000)) }
        SessionStore(context).save(nextRun)
        assertEquals(100000L, SessionStore(context).bestScore)
        assertEquals("水星", Ranks.forScore(nextRun.score).name)
    }

    @Test fun lateWaveWordsFitAboveKeyboard() {
        start()
        onActivity { a ->
            a.engine.restore(a.engine.snapshot().copy(elapsed = 260.0, score = 1560,
                meteors = listOf(
                    Meteor(1, "うちゅうぼうえいたい", 0, .18, .055),
                    Meteor(2, "ちきゅうをまもれ", 1, .56, .055),
                )))
            a.engine.resume()
        }
        SystemClock.sleep(300)
        screenshot("06-late-wave")
        onActivity { a ->
            a.input.onCreateInputConnection(EditorInfo())!!.commitText("ちきゅうをまもれ", 1)
        }
        await { it.engine.score == 1568L }
    }

    @Test fun settingsAndRanksAreAccessibleAndPersist() {
        device.findObject(By.text("称号一覧")).click()
        assertTrue(device.wait(Until.hasObject(By.text("THE SOLAR LADDER")), 2000))
        screenshot("07-ranks")
        device.pressBack()
        device.findObject(By.text("遊び方・設定")).click()
        val scroll = device.findObject(By.clazz("android.widget.ScrollView"))
        repeat(3) { if (!device.hasObject(By.text("BGM   ON"))) scroll.scroll(androidx.test.uiautomator.Direction.DOWN, .75f) }
        device.findObject(By.text("BGM   ON")).click()
        assertFalse(SessionStore(context).musicEnabled)
        device.findObject(By.text("効果音   ON")).click()
        assertFalse(SessionStore(context).effectsEnabled)
        screenshot("08-settings")
        scenario.recreate()
        scenario.onActivity { activity = it }
        assertFalse(SessionStore(context).musicEnabled)
        assertFalse(SessionStore(context).effectsEnabled)
    }
}
