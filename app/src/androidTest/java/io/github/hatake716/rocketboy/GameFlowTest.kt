package io.github.hatake716.rocketboy

import android.content.Context
import android.os.SystemClock
import android.view.inputmethod.EditorInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
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

    private fun tapSettled(selector: BySelector) {
        // Re-query bounds after ScrollView movement and use UiDevice's
        // synchronous tap so the complete press reaches the settled control.
        val deadline = SystemClock.uptimeMillis() + 4000
        while (SystemClock.uptimeMillis() < deadline) {
            val target = requireNotNull(device.wait(Until.findObject(selector), 1000))
            val bounds = target.visibleBounds
            SystemClock.sleep(300)
            val settled = device.findObject(selector) ?: continue
            if (settled.visibleBounds == bounds) {
                assertTrue("Touch injection failed: $selector", device.click(bounds.centerX(), bounds.centerY()))
                return
            }
        }
        fail("Control did not settle: $selector")
    }

    private fun await(timeout: Long = 6000, reason: String = "game state", predicate: (MainActivity) -> Boolean) {
        val until = SystemClock.uptimeMillis() + timeout
        while (SystemClock.uptimeMillis() < until) {
            var passed = false
            onActivity { passed = predicate(it) }
            if (passed) return
            SystemClock.sleep(40)
        }
        var passed = false
        var state = ""
        onActivity {
            passed = predicate(it)
            state = "${it.audio.currentTrack.id}, playing=${it.audio.isMusicPlaying}, phase=${it.engine.phase}"
        }
        if (!passed) screenshot("failure-await")
        // Assert on the test thread so a failure retains the calling test line.
        assertTrue("Timed out waiting for $reason ($state)", passed)
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

    @Test fun composingWrappedWordShowsProgressAndStillIntercepts() {
        start()
        // Finish Gboard's first-launch layout chooser on a fresh emulator.
        if (device.hasObject(By.text("次へ"))) tapSettled(By.text("次へ"))
        if (device.hasObject(By.text("完了"))) tapSettled(By.text("完了"))
        val word = "ぼうけんのはじまり"
        onActivity { a ->
            a.engine.restore(a.engine.snapshot().copy(elapsed = 225.0, spawnIn = 100.0, nextQuoteAt = 1000.0,
                meteors = listOf(Meteor(300, word, 0, .15, 0.0))))
            a.engine.resume()
            a.input.onCreateInputConnection(EditorInfo())!!.setComposingText(word.take(7), 1)
            assertEquals(word.take(7), a.findViewById<PixelScene>(R.id.game_scene).typed)
            assertFalse(a.engine.meteors.single().targeted)
            assertEquals(0L, a.engine.score)
        }
        screenshot("underline-second-row")
        onActivity { a -> a.input.onCreateInputConnection(EditorInfo())!!.setComposingText(word, 1) }
        screenshot("underline-final-row")
        onActivity { a -> a.input.onCreateInputConnection(EditorInfo())!!.finishComposingText() }
        await { it.engine.score == 90L }
        onActivity { assertEquals(1, it.engine.destroyed); assertTrue(it.input.text.isEmpty()) }
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
        await { it.engine.score == 10L }
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
        await { it.engine.score == 40L }
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
        await { it.engine.score == 30L }
        SystemClock.sleep(300)
        onActivity { assertEquals(30L, it.engine.score); assertEquals(1, it.engine.destroyed) }
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
        await { it.engine.score == 10L }
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
        await { it.input.text.toString() == "あ" || it.engine.score == 10L }
        device.click((device.displayWidth * .90).toInt(), keyboardTop + (keyboardHeight * .77).toInt())
        await { it.engine.score == 10L }
        screenshot("09-gboard-real-input")
    }

    @Test fun rankUpAtExactly100000AndBestRecordDoesNotAccumulateRuns() {
        start()
        seed("あ", 99990)
        onActivity { a -> a.input.onCreateInputConnection(EditorInfo())!!.commitText("あ", 1) }
        await { it.engine.score == 100000L }
        assertTrue(device.wait(Until.hasObject(By.text("RANK  冥王星  /  SOLAR SYSTEM COMPLETE · W1 +1%")), 2000))
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
        await { it.engine.score == 1640L }
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

    private fun openCategory(category: QuoteCategory) {
        val count = Quotes.inCategory(category).count { it.id in SessionStore(context).collectedQuoteIds }
        val description = "${category.title}、収集済み$count 件、全${Quotes.inCategory(category).size}件"
        val selector = UiSelector().description(description)
        assertTrue(UiScrollable(UiSelector().scrollable(true)).scrollIntoView(selector))
        device.findObject(By.desc(description)).click()
        assertTrue(device.wait(Until.hasObject(By.text(category.title)), 2000))
    }

    @Test fun categorizedCollectionSearchPaginationAndExplanations() {
        // Seed only the collection; these screens are UI evidence, not gameplay rewards.
        val ids = Quotes.all.map { it.id }.toSet()
        context.getSharedPreferences("rocketboy", Context.MODE_PRIVATE).edit()
            .putStringSet("collectedQuotes", ids).commit()
        device.findObject(By.res(pkg, "menu_button")).click()
        device.findObject(By.res(pkg, "quotes_button")).click()
        screenshot("16-categories")
        QuoteCategory.entries.forEach { category ->
            openCategory(category)
            val count = Quotes.inCategory(category).size
            assertTrue(device.hasObject(By.text("COLLECTION  $count / $count")))
            device.pressBack()
        }
        openCategory(QuoteCategory.HYAKUNIN)
        assertTrue(device.hasObject(By.text("100 件  ·  1 / 4 ページ")))
        device.findObject(By.res(pkg, "quote_next_button")).click()
        assertTrue(device.wait(Until.hasObject(By.text("100 件  ·  2 / 4 ページ")), 2000))
        device.findObject(By.res(pkg, "quote_previous_button")).click()
        val first = Quotes.find("hyakunin-001")!!
        val search = device.findObject(By.res(pkg, "quote_search_input"))
        search.text = "アキノタノ"
        device.findObject(By.res(pkg, "quote_search_button")).click()
        assertTrue(device.wait(Until.hasObject(By.text("1 件  ·  1 / 1 ページ")), 2000))
        screenshot("17-hyakunin-search")
        device.findObject(By.text("${first.text}\n${first.author.name}  ›")).click()
        assertTrue(UiScrollable(UiSelector().scrollable(true)).scrollIntoView(UiSelector().text(first.explanation)))
        screenshot("18-hyakunin-explanation")
        device.pressBack()
        assertEquals("アキノタノ", device.findObject(By.res(pkg, "quote_search_input")).text)
        assertTrue(device.hasObject(By.text("1 件  ·  1 / 1 ページ")))
        device.findObject(By.text("検索を解除")).click()
        assertTrue(device.wait(Until.hasObject(By.text("100 件  ·  1 / 4 ページ")), 2000))
        assertEquals(ids, SessionStore(context).collectedQuoteIds)
    }

    @Test fun quotePanelStaysAboveGroundWithFullLengthReadingAndSmallViewport() {
        onActivity { a ->
            val view = PixelScene(a).apply { engine = GameEngine() }
            // The layout must also fit future readings up to the supported 64 characters.
            for (height in listOf(120, 140, 179, 180, 220, 230, 320)) {
                view.layout(0, 0, 320, height)
                for (length in listOf(12, 33, Quotes.MAX_READING_LENGTH)) {
                    for (position in listOf(0.0, .5, 1.0)) {
                        val bounds = view.quoteBounds(Meteor(1, "あ".repeat(length), 0, position, .01))
                        assertTrue("$bounds in $height", bounds.top >= 0 && bounds.bottom <= height - 20)
                    }
                }
            }
        }
    }

    @Test fun gameQuoteInterceptionUnlocksOnlyAfterHitAndSurvivesRestart() {
        start()
        val quote = Quotes.inCategory(QuoteCategory.GAME).first()
        onActivity { a ->
            a.engine.restore(a.engine.snapshot().copy(spawnIn = 100.0,
                meteors = listOf(Meteor(700, quote.reading, 0, .20, .025, quoteId = quote.id))))
            a.engine.resume()
        }
        screenshot("33-game-quote-meteor")
        onActivity { a ->
            a.input.onCreateInputConnection(EditorInfo())!!.commitText(quote.reading.dropLast(1), 1)
            assertEquals(0L, a.engine.score)
            assertTrue(SessionStore(context).collectedQuoteIds.isEmpty())
            a.input.onCreateInputConnection(EditorInfo())!!.commitText(quote.reading.takeLast(1), 1)
            assertEquals(0L, a.engine.score)
            assertTrue(SessionStore(context).collectedQuoteIds.isEmpty())
        }
        await { it.engine.score == quote.reading.length * 303L }
        assertEquals(setOf(quote.id), SessionStore(context).collectedQuoteIds)
        screenshot("34-game-quote-hit")
        device.findObject(By.res(pkg, "pause_button")).click()
        scenario.recreate()
        scenario.onActivity { activity = it }
        await { it.window.decorView.hasWindowFocus() && it.findViewById<android.view.View>(R.id.menu_button).isLaidOut }
        device.findObject(By.res(pkg, "menu_button")).click()
        requireNotNull(device.wait(Until.findObject(By.res(pkg, "quotes_button")), 3000)).click()
        openCategory(QuoteCategory.GAME)
        assertTrue(device.hasObject(By.text("COLLECTION  1 / ${Quotes.inCategory(QuoteCategory.GAME).size}")))
        assertTrue(device.hasObject(By.textContains(quote.text)))
        assertEquals(setOf(quote.id), SessionStore(context).collectedQuoteIds)
        assertEquals(quote.reading.length * 303L, SessionStore(context).bestScore)
        screenshot("35-game-quote-collected")
    }

    @Test fun menuShowsEmptyCollectionWithoutUnlockingQuotes() {
        device.findObject(By.res(pkg, "menu_button")).click()
        device.findObject(By.res(pkg, "quotes_button")).click()
        assertTrue(device.wait(Until.hasObject(By.text("COLLECTION  0 / ${Quotes.all.size}")), 2000))
        openCategory(QuoteCategory.HISTORY)
        assertTrue(device.hasObject(By.text("まだ収集されていません")))
        assertFalse(device.hasObject(By.text(Quotes.all.first().text)))
        assertTrue(SessionStore(context).collectedQuoteIds.isEmpty())
        screenshot("10-empty-collection")
        device.pressBack()
        device.pressBack()
        assertTrue(device.wait(Until.hasObject(By.text("MENU / メニュー")), 2000))
    }

    @Test fun longestQuoteScoresReadingLengthTimes300AndCollectionSurvivesRecreationAndNewMission() {
        start()
        val quote = Quotes.all.maxBy { it.reading.length }
        onActivity { a ->
            a.engine.restore(a.engine.snapshot().copy(spawnIn = 100.0,
                meteors = listOf(Meteor(600, quote.reading, 0, .15, .04, quoteId = quote.id))))
            a.engine.resume()
            val view = a.findViewById<PixelScene>(R.id.game_scene)
            val logicalHeight = view.height * 320f / view.width
            for (position in listOf(0.0, .5, 1.0)) {
                val bounds = view.quoteBounds(a.engine.meteors.single().copy(y = position))
                assertTrue("Quote must fit vertically: $bounds in $logicalHeight", bounds.top >= 0 && bounds.bottom <= logicalHeight - 20)
                assertTrue(bounds.left >= 0 && bounds.right <= 320)
            }
        }
        screenshot("11-quote-meteor")
        onActivity { a -> a.input.onCreateInputConnection(EditorInfo())!!.commitText(quote.reading.take(16), 1) }
        SystemClock.sleep(200)
        screenshot("19-quote-continued-lines")
        onActivity { a ->
            assertEquals(0L, a.engine.score)
            assertTrue(SessionStore(context).collectedQuoteIds.isEmpty())
            a.input.onCreateInputConnection(EditorInfo())!!.commitText(quote.reading.drop(16), 1)
        }
        await { it.engine.score == quote.reading.length * 303L }
        assertEquals(setOf(quote.id), SessionStore(context).collectedQuoteIds)
        screenshot("12-quote-hit")
        device.findObject(By.res(pkg, "pause_button")).click()
        await { it.engine.phase == Phase.PAUSED }
        scenario.recreate()
        scenario.onActivity { activity = it }
        device.findObject(By.res(pkg, "menu_button")).click()
        device.findObject(By.res(pkg, "quotes_button")).click()
        assertTrue(device.wait(Until.hasObject(By.text("COLLECTION  1 / ${Quotes.all.size}")), 2000))
        screenshot("13-collection")
        openCategory(quote.category)
        device.findObject(By.textContains(quote.text)).click()
        assertTrue(device.wait(Until.hasObject(By.text(quote.text)), 2000))
        screenshot("14-quote-detail")
        device.findObject(By.res(pkg, "quote_author_button")).click()
        assertTrue(device.wait(Until.hasObject(By.text(quote.author.name)), 2000))
        assertTrue(device.hasObject(By.text(quote.author.biography)) ||
            UiScrollable(UiSelector().scrollable(true)).scrollIntoView(UiSelector().text(quote.author.biography)))
        screenshot("15-quote-author")
        device.pressBack()
        assertTrue(device.wait(Until.hasObject(By.text(quote.text)), 2000))
        val store = SessionStore(context)
        store.clearMission()
        store.save(GameEngine())
        assertEquals(setOf(quote.id), SessionStore(context).collectedQuoteIds)
        assertEquals(quote.reading.length * 303L, store.bestScore)
        assertEquals(0L, store.load()!!.score)
    }

    @Test fun legacyMissionMigratesWithoutLosingProgressOrSettings() {
        val store = SessionStore(context)
        store.musicEnabled = false
        val game = GameEngine().apply {
            restore(snapshot().copy(score = 42, elapsed = 30.0,
                meteors = listOf(Meteor(300, "ほし", 0, .2, .037))))
        }
        store.save(game)
        val prefs = context.getSharedPreferences("rocketboy", Context.MODE_PRIVATE)
        val json = org.json.JSONObject(prefs.getString("mission", null)!!)
        json.put("version", 1); json.remove("collectedQuotes"); json.remove("nextQuoteAt")
        prefs.edit().putString("mission", json.toString()).commit()
        val saved = store.load()!!
        assertEquals(42L, saved.score)
        assertEquals("ほし", saved.meteors.single().word)
        assertTrue(saved.collectedQuotes.isEmpty())
        assertEquals(35.0, saved.nextQuoteAt, 0.0)
        assertFalse(store.musicEnabled)
        assertEquals(42L, store.bestScore)
    }

    @Test fun storeRestoresFlyingQuoteAndRejectsUnknownQuoteIdentity() {
        val quote = Quotes.all.maxBy { it.reading.length }
        assertTrue(quote.reading.length > 32)
        val store = SessionStore(context)
        val game = GameEngine().apply {
            resume()
            meteors += Meteor(1, quote.reading, 0, .5, .05, quoteId = quote.id)
            fire(quote.reading)
            tick(.1)
        }
        store.save(game)
        assertTrue(store.collectedQuoteIds.isEmpty())
        val restored = GameEngine().apply { restore(store.load()!!); resume() }
        repeat(8) { restored.tick(.1) }
        store.save(restored)
        assertEquals(quote.reading.length * 303L, restored.score)
        assertEquals(setOf(quote.id), store.collectedQuoteIds)
        store.save(game)
        val prefs = context.getSharedPreferences("rocketboy", Context.MODE_PRIVATE)
        val json = org.json.JSONObject(prefs.getString("mission", null)!!)
        json.getJSONArray("meteors").getJSONObject(0).put("quoteId", "not-in-catalog")
        prefs.edit().putString("mission", json.toString()).commit()
        assertNull(store.load())
        assertEquals(setOf(quote.id), store.collectedQuoteIds)
    }

    @Test fun rankMusicFollowsInterceptionJumpPauseRestoreAndNewMission() {
        start()
        await { it.audio.isMusicPlaying }
        onActivity { assertEquals("cadet", it.audio.currentTrack.id) }
        seed("あ", 3990)
        onActivity { it.input.onCreateInputConnection(EditorInfo())!!.commitText("あ", 1) }
        await { it.engine.score == 4000L && it.audio.currentTrack.id == "mercury" && it.audio.isMusicPlaying }
        screenshot("39-mercury-rank-music")
        val quote = Quotes.all.maxBy { it.reading.length }
        onActivity { a ->
            a.engine.restore(a.engine.snapshot().copy(spawnIn = 100.0,
                meteors = listOf(Meteor(902, quote.reading, 0, .2, .01, quoteId = quote.id))))
            a.engine.resume()
            a.input.onCreateInputConnection(EditorInfo())!!.commitText(quote.reading, 1)
        }
        val expectedScore = 4000 + quote.reading.length * 303L
        val expectedTrack = CelestialMusic.forScore(expectedScore)
        await { it.engine.score == expectedScore && it.audio.currentTrack == expectedTrack && it.audio.isMusicPlaying }
        screenshot("40-quote-rank-jump")
        device.findObject(By.res(pkg, "pause_button")).click()
        await { it.engine.phase == Phase.PAUSED && !it.audio.isMusicPlaying }
        var position = 0
        onActivity { position = it.audio.musicPosition }
        SystemClock.sleep(300)
        onActivity { assertEquals(position, it.audio.musicPosition) }
        scenario.recreate()
        scenario.onActivity { activity = it }
        requireNotNull(device.wait(Until.findObject(By.text("▶  ミッションを再開")), 3000)).click()
        await { it.engine.phase == Phase.RUNNING && it.audio.currentTrack == expectedTrack && it.audio.isMusicPlaying }
        screenshot("41-restored-rank-music")
        onActivity { a ->
            a.engine.restore(a.engine.snapshot().copy(damage = 2,
                meteors = listOf(Meteor(903, "あ", 0, .9999, .1))))
            a.engine.resume()
        }
        assertTrue(device.wait(Until.hasObject(By.text("GAME OVER")), 3000))
        await { !it.audio.isMusicPlaying }
        device.findObject(By.res(pkg, "retry_button")).click()
        await { it.engine.phase == Phase.RUNNING && it.audio.currentTrack.id == "cadet" && it.audio.isMusicPlaying }
    }

    @Test fun mutedRankChangesStaySilentAndEnablingMusicStartsTheLatestTrack() {
        start()
        await { it.audio.isMusicPlaying }
        onActivity { a ->
            SessionStore(context).musicEnabled = false
            a.audio.settingsChanged()
        }
        await { !it.audio.isMusicPlaying }
        seed("あ", 99990)
        onActivity { it.input.onCreateInputConnection(EditorInfo())!!.commitText("あ", 1) }
        await { it.engine.score == 100000L && it.audio.currentTrack.id == "pluto" }
        onActivity { a ->
            assertFalse(a.audio.isMusicPlaying)
            SessionStore(context).musicEnabled = true
            a.audio.settingsChanged()
        }
        await { it.audio.isMusicPlaying }
        SystemClock.sleep(350)
        var position = 0
        onActivity { a -> position = a.audio.musicPosition; a.audio.selectScore(100010L) }
        SystemClock.sleep(120)
        onActivity { a -> assertTrue(a.audio.musicPosition >= position) }
        device.pressHome()
        await { it.engine.phase == Phase.PAUSED && !it.audio.isMusicPlaying }
    }

    @Test fun wikipediaSourcesAndExplanationsAreVisibleInEveryCategory() {
        val selected = QuoteCategory.entries.map { Quotes.inCategory(it).first() }
        val ids = selected.map { it.id }.toSet()
        context.getSharedPreferences("rocketboy", Context.MODE_PRIVATE).edit()
            .putStringSet("collectedQuotes", ids).commit()
        device.findObject(By.res(pkg, "menu_button")).click()
        requireNotNull(device.wait(Until.findObject(By.res(pkg, "quotes_button")), 3000)).click()
        screenshot("42-wikipedia-categories")
        selected.forEach { quote ->
            openCategory(quote.category)
            device.findObject(By.res(pkg, "quote_search_input")).text = quote.author.name
            device.findObject(By.res(pkg, "quote_search_button")).click()
            assertTrue(device.wait(Until.hasObject(By.text("1 件  ·  1 / 1 ページ")), 2000))
            device.findObject(By.textContains(quote.text)).click()
            assertTrue(device.wait(Until.hasObject(By.text(quote.text)), 2000))
            val scroll = UiScrollable(UiSelector().scrollable(true))
            assertTrue(device.hasObject(By.text(quote.explanation)) || scroll.scrollIntoView(UiSelector().text(quote.explanation)))
            screenshot("43-wikipedia-${quote.category.key}-explanation")
            scroll.scrollToEnd(10)
            assertTrue(device.hasObject(By.text(quote.sourceUrl)))
            assertTrue(quote.sourceUrl.startsWith("https://ja.wikipedia.org/") && quote.sourceUrl.contains("oldid="))
            assertTrue(device.hasObject(By.textContains("CC BY-SA 4.0")))
            screenshot("44-wikipedia-${quote.category.key}-source")
            device.pressBack()
            assertEquals(quote.author.name, device.findObject(By.res(pkg, "quote_search_input")).text)
            device.pressBack()
        }
        assertEquals(ids, SessionStore(context).collectedQuoteIds)
    }

    @Test fun removingAQuotePreservesSavedScoreAndOtherMeteorsWithoutAwardingPoints() {
        // The removed legacy quote remains only as an ID/reading for safe migration.
        val retired = Quotes::class.java.getResourceAsStream("/quotes/retired.tsv")!!
            .bufferedReader().useLines { lines -> lines.first { !it.startsWith("#") }.split('\t') }
        val retained = Quotes.all.first()
        val game = GameEngine().apply {
            restore(snapshot().copy(score = 9876, elapsed = 55.0, damage = 1,
                meteors = listOf(Meteor(10, "ほし", 1, .3, .04),
                    Meteor(11, retired[1], 0, .5, .02, targeted = true, rocket = .7, quoteId = retired[0])),
                collectedQuotes = setOf(retained.id, retired[0])))
        }
        val store = SessionStore(context)
        store.musicEnabled = false
        store.save(game)
        val loaded = store.load()!!
        assertEquals(9876L, loaded.score)
        assertEquals(55.0, loaded.elapsed, 0.0)
        assertEquals(1, loaded.damage)
        assertEquals(listOf("ほし"), loaded.meteors.map { it.word })
        assertEquals(setOf(retained.id), loaded.collectedQuotes)
        assertEquals(setOf(retained.id), store.collectedQuoteIds)
        assertEquals(9876L, store.bestScore)
        assertFalse(store.musicEnabled)
        val resumed = GameEngine().apply { restore(loaded); resume() }
        repeat(5) { resumed.tick(.1) }
        assertEquals(9876L, resumed.score)
        assertEquals(2, resumed.lives)
        assertNull(Quotes.find(retired[0]))
        assertFalse(Quotes.isRetired(retired[0], "invalid reading"))
    }

    @Test fun revisedPoemReadingKeepsItsCollectionAndRemovesOnlyTheOldFlyingVersion() {
        val quote = Quotes.find("hyakunin-049")!!
        val oldReading = "みかきもりえじのたくひのよるはもえひるはきえつつものをこそおもへ"
        assertNotEquals(oldReading, quote.reading)
        val store = SessionStore(context)
        store.save(GameEngine().apply {
            restore(snapshot().copy(score = 10000, collectedQuotes = setOf(quote.id),
                meteors = listOf(Meteor(91, oldReading, 0, .4, .03, quoteId = quote.id))))
        })
        assertTrue(store.load()!!.meteors.isEmpty())
        assertEquals(10000L, store.load()!!.score)
        assertEquals(setOf(quote.id), store.collectedQuoteIds)
        val updated = GameEngine().apply {
            restore(store.load()!!.copy(meteors = listOf(Meteor(92, quote.reading, 0, .4, .03, quoteId = quote.id))))
        }
        store.save(updated)
        assertEquals(quote.reading, store.load()!!.meteors.single().word)
    }
    @Test fun configureQuoteModeAppliesFixedBonusAndRestoresTheOriginalSettings() {
        device.findObject(By.res(pkg, "play_options_button")).click()
        device.findObject(By.res(pkg, "quote_mode_button")).click()
        repeat(9) { device.findObject(By.res(pkg, "level_plus_button")).click() }
        assertEquals("WAVE 10", device.findObject(By.res(pkg, "start_level_label")).text)
        assertTrue(device.findObject(By.res(pkg, "bonus_label")).text.contains("+10％"))
        screenshot("50-play-options")
        val scroller = UiScrollable(UiSelector().scrollable(true))
        if (!device.hasObject(By.res(pkg, "start_configured_button"))) scroller.scrollIntoView(UiSelector().resourceId("$pkg:id/start_configured_button"))
        device.findObject(By.res(pkg, "start_configured_button")).click()
        await { it.engine.phase == Phase.RUNNING }
        onActivity {
            assertEquals(0L, it.engine.score)
            assertEquals(10, it.engine.wave)
            assertEquals(PlayOptions(GameMode.QUOTES, 10), it.engine.options)
        }
        await { it.engine.meteors.isNotEmpty() }
        lateinit var reading: String
        onActivity { a ->
            assertNotNull(a.engine.meteors.single().quote)
            reading = a.engine.meteors.single().word
        }
        screenshot("51-quote-only-meteor")
        onActivity { it.input.onCreateInputConnection(EditorInfo())!!.commitText(reading, 1) }
        await { it.engine.score == reading.length * 330L }
        screenshot("52-bonus-hit")
        device.findObject(By.res(pkg, "pause_button")).click()
        val store = SessionStore(context)
        assertEquals(reading.length * 330L, store.topScores.single().score)
        assertEquals(10, store.topScores.single().startLevel)
        assertEquals(GameMode.QUOTES, store.topScores.single().mode)
        store.playOptions = PlayOptions() // A different next-run setting must not alter the saved mission.
        scenario.recreate(); scenario.onActivity { activity = it }
        requireNotNull(device.wait(Until.findObject(By.text("▶  ミッションを再開")), 3000)).click()
        await { it.engine.phase == Phase.RUNNING }
        onActivity { assertEquals(PlayOptions(GameMode.QUOTES, 10), it.engine.options) }
        assertEquals(1, store.topScores.size)
    }

    @Test fun scoreTopTenMigratesTheOldBestUpdatesRunsAndRemainsVisibleAfterRestart() {
        val prefs = context.getSharedPreferences("rocketboy", Context.MODE_PRIVATE)
        prefs.edit().putLong("bestScore", 5000).commit()
        val store = SessionStore(context)
        assertTrue(store.topScores.single().legacy)
        assertEquals(5000L, store.topScores.single().score)
        for (i in 1..12) {
            val game = GameEngine(options = PlayOptions(if (i % 2 == 0) GameMode.QUOTES else GameMode.NORMAL, i)).apply {
                restore(snapshot().copy(id = "ranking-$i", score = i * 1000L, elapsed = 30.0))
            }
            repeat(3) { store.save(game, finished = true) }
        }
        assertEquals(10, store.topScores.size)
        assertEquals(12000L, store.topScores.first().score)
        assertEquals(2, store.topScores.count { it.score == 5000L })
        assertEquals(10, store.topScores.map { it.missionId }.distinct().size)
        assertEquals(12, store.topScores.first().startLevel)
        assertEquals(13, store.topScores.first().wave)
        scenario.recreate(); scenario.onActivity { activity = it }
        requireNotNull(device.wait(Until.findObject(By.res(pkg, "menu_button")), 3000)).click()
        val scroller = UiScrollable(UiSelector().scrollable(true))
        if (!device.hasObject(By.res(pkg, "scores_button"))) scroller.scrollIntoView(UiSelector().resourceId("$pkg:id/scores_button"))
        device.findObject(By.res(pkg, "scores_button")).click()
        assertTrue(device.wait(Until.hasObject(By.text("自己ベスト TOP 10")), 3000))
        assertTrue(device.hasObject(By.textContains("012000 PT")))
        screenshot("53-top-ten")
        scroller.scrollIntoView(UiSelector().textContains("旧版の自己ベスト"))
        screenshot("54-old-best-migration")
        assertEquals(10, SessionStore(context).topScores.size)
    }

    @Test fun musicRoomPlaysLockedRanksWhileMutedAndStopsOnNavigationAndBackground() {
        val store = SessionStore(context)
        store.musicEnabled = false; store.effectsEnabled = false
        val original = GameEngine().apply { restore(snapshot().copy(score = 1234)) }
        store.save(original)
        device.findObject(By.res(pkg, "menu_button")).click()
        val scroller = UiScrollable(UiSelector().scrollable(true))
        if (!device.hasObject(By.res(pkg, "music_room_button"))) scroller.scrollIntoView(UiSelector().resourceId("$pkg:id/music_room_button"))
        device.findObject(By.res(pkg, "music_room_button")).click()
        assertTrue(device.wait(Until.hasObject(By.text("BGM試聴室")), 3000))
        screenshot("55-music-room")
        scroller.scrollToEnd(20)
        // Game tests disable global idle waits for animation. This static list
        // must finish scrolling before a tap, or ScrollView consumes the tap.
        device.waitForIdle(1000)
        tapSettled(By.desc("冥王星のBGMを再生"))
        await(reason = "Pluto preview to start") { it.audio.currentTrack.id == "pluto" && it.audio.isMusicPlaying }
        assertEquals(1234L, store.bestScore)
        assertEquals(original.missionId, store.load()!!.id)
        assertFalse(store.musicEnabled)
        screenshot("56-pluto-preview")
        scroller.scrollToBeginning(20)
        device.waitForIdle(1000)
        tapSettled(By.res(pkg, "preview_stop_button"))
        await(reason = "preview stop button") { !it.audio.isMusicPlaying }
        device.waitForIdle(1000)
        tapSettled(By.res(pkg, "preview_play_button"))
        await(reason = "preview play button") { it.audio.isMusicPlaying }
        screenshot("57-music-transport")
        device.pressHome()
        await(reason = "preview background pause") { !it.audio.isMusicPlaying }
        context.startActivity(android.content.Intent(context, MainActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP))
        SystemClock.sleep(300)
        onActivity { assertFalse(it.audio.isMusicPlaying) }
        device.pressBack()
        onActivity { assertFalse(it.audio.isMusicPlaying) }
        assertFalse(SessionStore(context).musicEnabled)
    }

    @Test fun playOptionDraftCanBeCancelledAndOldMissionDefaultsRemainNormalWaveOne() {
        val store = SessionStore(context)
        val original = GameEngine().apply { restore(snapshot().copy(score = 42)) }
        store.save(original)
        val prefs = context.getSharedPreferences("rocketboy", Context.MODE_PRIVATE)
        val json = org.json.JSONObject(prefs.getString("mission", null)!!)
        json.put("version", 2); json.remove("mode"); json.remove("startLevel")
        prefs.edit().putString("mission", json.toString()).commit()
        device.findObject(By.res(pkg, "play_options_button")).click()
        device.findObject(By.res(pkg, "quote_mode_button")).click()
        device.findObject(By.res(pkg, "level_plus_button")).click()
        device.pressBack()
        assertEquals(PlayOptions(), store.playOptions)
        assertEquals(PlayOptions(), store.load()!!.options)
        assertEquals(42L, store.load()!!.score)
    }

}
