package io.github.hatake716.rocketboy

import android.content.Context
import android.content.Intent
import android.os.SystemClock
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

/** Black-box release acceptance: platform APIs only; no references to classes renamed by R8.
 * Sign the test APK with the upload key and run this class on the isolated emulator only.
 * Demo saved missions supply repeatable scenes; input, scoring, UI and audio use the release APK.
 */
@RunWith(AndroidJUnit4::class)
class ReleaseStoreCaptureTest {
    private val pkg = "io.github.hatake716.rocketboy"
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val folder get() = File(context.getExternalFilesDir(null), "play-store").apply { mkdirs() }
    private fun click(text: String) {
        val view = requireNotNull(device.wait(Until.findObject(By.text(text)), 5000)) { "Missing: $text" }
        val b = view.visibleBounds
        device.click(b.centerX(), b.centerY())
        SystemClock.sleep(300)
    }
    private fun has(text: String) = device.wait(Until.hasObject(By.textContains(text)), 5000)
    private fun shot(name: String) { assertTrue(device.takeScreenshot(File(folder, "$name.png"))) }
    private fun launch() {
        context.startActivity(requireNotNull(context.packageManager.getLaunchIntentForPackage(pkg))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        assertTrue(has("ミッション")); SystemClock.sleep(900)
    }
    private fun meteor(id: Int, word: String, lane: Int, y: Double, speed: Double, quote: String? = null) = JSONObject()
        .put("id", id).put("word", word).put("lane", lane).put("y", y).put("speed", speed)
        .put("targeted", false).put("rocket", 0.0).put("quoteId", quote ?: JSONObject.NULL)
    private fun seed(mode: String, meteors: JSONArray, score: Long = 0) {
        val mission = JSONObject().put("version",3).put("mode",mode).put("startLevel",1)
            .put("id","store-demo-$mode").put("elapsed",0.0).put("score",score).put("damage",0)
            .put("destroyed",0).put("combo",0).put("bestCombo",0).put("spawnIn",100.0)
            .put("random",101).put("nextId",100).put("nextQuoteAt",100.0)
            .put("meteors",meteors).put("collectedQuotes",JSONArray())
        context.getSharedPreferences("rocketboy",Context.MODE_PRIVATE).edit()
            .putString("mission",mission.toString()).putString("mode",mode).putInt("startLevel",1).commit()
        launch(); click("▶  ミッションを再開"); SystemClock.sleep(4000)
        if (device.hasObject(By.text("次へ"))) { click("次へ"); SystemClock.sleep(600) }
        if (device.hasObject(By.text("完了"))) { click("完了"); SystemClock.sleep(400) }
        assertTrue(device.hasObject(By.res(pkg,"kana_input")))
    }
    private fun home() {
        device.pressBack(); SystemClock.sleep(300)
        // Back dismisses IME and pauses; a second back returns to title.
        device.pressBack(); SystemClock.sleep(300)
        if (!device.hasObject(By.text("メニュー"))) { device.pressBack(); SystemClock.sleep(300) }
        assertTrue(has("メニュー"))
    }
    @Test fun capture150StoreUpdate() {
        check(android.os.Build.HARDWARE in listOf("ranchu", "goldfish"))
        assertEquals(0, context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)
        assertEquals("1.5.0", context.packageManager.getPackageInfo(pkg, 0).versionName)
        Configurator.getInstance().waitForIdleTimeout = 0
        context.getSharedPreferences("rocketboy", Context.MODE_PRIVATE).edit().clear().commit()
        launch()
        val word = "ぼうけんのはじまり"
        for ((score, name) in listOf(12_000L to "01-earth", 20_000L to "02-mars", 100_000L to "03-pluto")) {
            seed("NORMAL", JSONArray().put(meteor(1, word, 0, .15, 0.0)).put(meteor(2, "うちゅう", 1, .38, 0.0)), score)
            device.findObject(By.res(pkg, "kana_input")).text = word.take(7)
            SystemClock.sleep(450)
            assertTrue(device.hasObject(By.text("%06d".format(score))))
            shot(name)
            if (score == 12_000L) {
                device.findObject(By.res(pkg, "kana_input")).text = word
                click("発射")
                assertTrue(device.wait(Until.hasObject(By.text("012090")), 5000))
            }
            home()
        }
        seed("QUOTES", JSONArray().put(meteor(3, "われおもうゆえにわれあり", 0, .16, 0.0, "descartes-think")), 16_000)
        device.findObject(By.res(pkg, "kana_input")).text = "われおもう"
        SystemClock.sleep(350); shot("04-quote-moon")
        device.findObject(By.res(pkg, "kana_input")).text = "われおもうゆえにわれあり"
        click("発射")
        assertTrue(device.wait(Until.hasObject(By.text("019636")), 5000))
        assertTrue(context.getSharedPreferences("rocketboy", 0).getStringSet("collectedQuotes", emptySet())!!.contains("descartes-think"))
        home()
        seed("COMPETITIVE", JSONArray().put(meteor(4, "そら", 0, .2, 0.0)).put(meteor(5, "ほし", 1, .55, .06)), 19_980)
        device.findObject(By.res(pkg, "kana_input")).text = "そら"; click("発射")
        assertTrue(device.wait(Until.hasObject(By.text("020000")), 5000))
        assertTrue(device.wait(Until.hasObject(By.res(pkg, "share_x_button")), 15000))
        assertTrue(has("#rocketboy"))
        device.swipe(540, 1630, 540, 1490, 20); SystemClock.sleep(500)
        shot("05-competitive-result")
        device.pressBack(); assertTrue(has("メニュー"))
        click("メニュー"); click("名言集")
        assertTrue(has("228")); shot("06-collection")
        device.pressBack(); click("BGM試聴室")
        assertTrue(has("26"))
        val earth = requireNotNull(device.wait(Until.findObject(By.textStartsWith("03  ")), 5000)).visibleBounds
        device.click(earth.centerX(), earth.centerY()); assertTrue(has("再生中"))
        SystemClock.sleep(350); shot("07-music")
        device.findObject(By.res(pkg, "preview_stop_button")).click(); assertTrue(has("停止中"))
        device.pressBack(); device.pressBack(); assertTrue(has("メニュー")); shot("08-title")
        click("メニュー"); click("自己ベスト TOP 10")
        assertTrue(has("100000")); shot("verification-top10")
        File(folder, "update-acceptance.txt").writeText("1.5.0 (10), debuggable=false. Captured 8 native screens. Demo saved scores/positions; real input, interception and scoring: 12000->12090, quote 16000->19636, competitive 19980->20000 then one impact. Collection, BGM play/stop, highest-rank title and top10 verified. No external post.\n")
    }

    @Test fun celestialRankColorsChangeOnInterception() {
        check(android.os.Build.HARDWARE in listOf("ranchu", "goldfish"))
        assertEquals(0, context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)
        assertEquals("1.5.0", context.packageManager.getPackageInfo(pkg, 0).versionName)
        Configurator.getInstance().waitForIdleTimeout = 0
        context.getSharedPreferences("rocketboy", Context.MODE_PRIVATE).edit().clear().putLong("bestScore", 100_000).commit()
        fun color(expected: Int, name: String) {
            SystemClock.sleep(250)
            shot(name)
            val image = android.graphics.BitmapFactory.decodeFile(File(folder, "$name.png").absolutePath)
            assertEquals(name, expected, image.getPixel(5, 100))
            image.recycle()
        }
        launch()
        color(0xFF20132F.toInt(), "palette-home-pluto")
        val word = "ぼうけんのはじまり"
        seed("NORMAL", JSONArray().put(meteor(1, word, 0, .15, 0.0)), score = 15_990)
        val input = requireNotNull(device.findObject(By.res(pkg, "kana_input")))
        input.text = word.take(7)
        color(0xFF0B1929.toInt(), "palette-game-earth")
        assertEquals(word.take(7), input.text)
        input.text = word; click("発射")
        assertTrue(device.wait(Until.hasObject(By.text("016080")), 5000))
        color(0xFF1B1C24.toInt(), "palette-rankup-moon")
        home()
        color(0xFF20132F.toInt(), "palette-home-best")
    }

    @Test fun wrappedWordProgressAndInterception() {
        check(android.os.Build.HARDWARE in listOf("ranchu", "goldfish"))
        assertEquals(0, context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)
        assertEquals("1.4.2", context.packageManager.getPackageInfo(pkg, 0).versionName)
        Configurator.getInstance().waitForIdleTimeout = 0
        context.getSharedPreferences("rocketboy", Context.MODE_PRIVATE).edit().clear().commit()
        launch()
        val word = "ぼうけんのはじまり"
        seed("NORMAL", JSONArray().put(meteor(1, word, 0, .15, 0.0)))
        val input = requireNotNull(device.findObject(By.res(pkg, "kana_input")))
        input.text = word.take(7)
        SystemClock.sleep(400)
        assertEquals(word.take(7), input.text)
        assertTrue(device.hasObject(By.text("000000")))
        shot("underline-second-row")
        input.text = word
        SystemClock.sleep(400)
        shot("underline-final-row")
        click("発射")
        assertTrue(device.wait(Until.hasObject(By.text("000090")), 5000))
        // Android accessibility exposes the hint as text when this EditText is empty.
        assertEquals("ここにひらがな", device.findObject(By.res(pkg, "kana_input")).text)
        shot("underline-intercepted")
    }

    @Test fun captureCompetitiveResultFraming() {
        check(android.os.Build.HARDWARE in listOf("ranchu", "goldfish"))
        Configurator.getInstance().waitForIdleTimeout=0
        launch()
        seed("COMPETITIVE",JSONArray().put(meteor(4,"そら",0,.2,.01)).put(meteor(5,"ほし",1,.60,.05)))
        device.findObject(By.res(pkg,"kana_input")).text="そら"
        click("発射")
        assertTrue(device.wait(Until.hasObject(By.text("000020")),5000))
        assertTrue(device.wait(Until.hasObject(By.res(pkg,"share_x_button")),15000))
        device.swipe(540,1630,540,1490,20); SystemClock.sleep(350)
        shot("05-competitive-result")
    }
    @Test fun releaseAcceptanceAndScreenshots() {
        check(android.os.Build.HARDWARE in listOf("ranchu","goldfish"))
        assertEquals(0,context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)
        assertEquals("1.4.1",context.packageManager.getPackageInfo(pkg,0).versionName)
        Configurator.getInstance().waitForIdleTimeout=0
        context.getSharedPreferences("rocketboy",Context.MODE_PRIVATE).edit().clear().commit()
        launch(); shot("08-title")
        seed("NORMAL",JSONArray().put(meteor(1,"そら",0,.20,.01)).put(meteor(2,"ほし",1,.36,.01)))
        shot("01-normal")
        device.findObject(By.res(pkg,"kana_input")).text="そら"
        click("発射")
        assertTrue(device.wait(Until.hasObject(By.text("000020")),5000))
        home()
        seed("QUOTES",JSONArray().put(meteor(3,"われおもうゆえにわれあり",0,.16,.006,"descartes-think")))
        shot("02-quote")
        device.findObject(By.res(pkg,"kana_input")).text="われおもうゆえにわれあり"
        click("発射")
        assertTrue(device.wait(Until.hasObject(By.text("003636")),5000))
        shot("03-quote-interception")
        assertTrue(context.getSharedPreferences("rocketboy",0).getStringSet("collectedQuotes",emptySet())!!.contains("descartes-think"))
        home()
        seed("COMPETITIVE",JSONArray().put(meteor(4,"そら",0,.2,.01)).put(meteor(5,"ほし",1,.60,.05)))
        shot("04-competitive")
        device.findObject(By.res(pkg,"kana_input")).text="そら"
        click("発射")
        assertTrue(device.wait(Until.hasObject(By.text("000020")),5000))
        assertTrue(device.wait(Until.hasObject(By.res(pkg,"share_x_button")),15000))
        assertTrue(has("#rocketboy"))
        device.swipe(540,1630,540,1490,20); SystemClock.sleep(350)
        shot("05-competitive-result")
        device.pressBack(); SystemClock.sleep(500)
        if (!device.hasObject(By.text("メニュー"))) device.pressBack()
        assertTrue(has("メニュー")); click("メニュー"); click("名言集")
        assertTrue(has("228")); shot("06-collection")
        device.pressBack(); SystemClock.sleep(300); click("BGM試聴室")
        assertTrue(has("26"))
        val earth = requireNotNull(device.wait(Until.findObject(By.textStartsWith("03  ")),5000))
        val earthBounds = earth.visibleBounds; device.click(earthBounds.centerX(),earthBounds.centerY())
        assertTrue(has("再生中")); SystemClock.sleep(500); shot("07-music")
        val play = requireNotNull(device.wait(Until.findObject(By.res(pkg,"preview_play_button")),5000))
        play.click(); assertTrue(has("再生中"))
        device.findObject(By.res(pkg,"preview_stop_button")).click(); assertTrue(has("停止中"))
        device.pressBack(); SystemClock.sleep(300); click("遊び方・設定")
        repeat(9) { if(!device.hasObject(By.res(pkg,"privacy_button"))) { device.swipe(540,1610,540,430,25); SystemClock.sleep(200) } }
        click("プライバシーポリシー")
        assertTrue(has("ROCKETBOY プライバシーポリシー")); assertTrue(has("acesmash@gmail.com")); shot("verification-privacy")
        device.pressBack(); assertTrue(has("PILOT'S MANUAL"))
        device.pressBack(); click("メニュー"); click("自己ベスト TOP 10")
        assertTrue(has("003636")); shot("verification-top10")
        File(folder,"acceptance.txt").writeText("Release 1.4.1 (8), debuggable=false. Native UI: normal input 20 PT; quote input 3636 PT and collection; competitive one-impact game over and score share card; BGM play/stop; privacy and back; top10. Saved demo fixtures; no shipping game code injected. External X submission not performed.\n")
    }
}
