package io.github.hatake716.rocketboy

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Choreographer
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Locale
import kotlin.math.ceil

class MainActivity : ComponentActivity(), Choreographer.FrameCallback {
    internal var engine = GameEngine()
        private set
    internal lateinit var input: KanaInput
        private set
    private lateinit var store: SessionStore
    private lateinit var audio: ChipAudio
    private lateinit var root: FrameLayout
    private lateinit var font: Typeface
    private var scene: PixelScene? = null
    private var gameHeader: LinearLayout? = null
    private var overlay: View? = null
    private var scoreText: TextView? = null
    private var livesText: TextView? = null
    private var rankText: TextView? = null
    private var rankBar: ProgressBar? = null
    private var hint: TextView? = null
    private var screen = "home"
    private var foreground = false
    private var lastFrame = 0L
    private var countdown = 0.0
    private var checkpointClock = 0.0
    private var lastHud = ""
    private var imeWasVisible = false
    private var inputBeforePause = ""
    private var previousBest = 0L
    private var messageTime = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        volumeControlStream = android.media.AudioManager.STREAM_MUSIC
        font = resources.getFont(R.font.dot_gothic)
        store = SessionStore(this)
        audio = ChipAudio(this, store) { runOnUiThread { if (screen == "game") pauseGame() } }
        root = FrameLayout(this).apply { setBackgroundColor(Palette.ink) }
        setContentView(root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, ime.bottom))
            val visible = insets.isVisible(WindowInsetsCompat.Type.ime())
            if (imeWasVisible && !visible && screen == "game" && engine.phase == Phase.RUNNING) {
                root.post { if (!imeWasVisible && screen == "game") pauseGame() }
            }
            imeWasVisible = visible
            view.post { adaptGameLayout() }
            WindowInsetsCompat.CONSUMED
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (screen) {
                    "game" -> if (overlay == null) pauseGame() else showHome()
                    "home" -> { isEnabled = false; onBackPressedDispatcher.onBackPressed(); isEnabled = true }
                    else -> showHome()
                }
            }
        })
        showHome()
    }

    private fun freshPage(name: String): LinearLayout {
        screen = name
        root.removeAllViews()
        overlay = null
        scene = null
        gameHeader = null
        scoreText = null; livesText = null; rankText = null; rankBar = null; hint = null
        lastHud = ""
        return column().also { root.addView(it, FrameLayout.LayoutParams(-1, -1)) }
    }

    private fun showHome() {
        if (screen == "game" && engine.phase != Phase.GAME_OVER) {
            engine.pause()
            store.save(engine)
        }
        countdown = 0.0
        audio.pause()
        hideKeyboard()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val page = freshPage("home")
        page.setPadding(dp(22), dp(8), dp(22), dp(14))
        val header = row()
        header.addView(label("■  ORBITAL DEFENSE", 11f, Palette.muted), weighted(1f, 36))
        header.addView(label("01 / READY", 10f, Palette.mid, Gravity.END or Gravity.CENTER_VERTICAL), fixed(-2, 36))
        page.addView(header)
        val art = PixelScene(this).apply { contentDescription = "ROCKETBOY。ドット絵のロケットと地球、ひらがなの隕石" }
        scene = art
        page.addView(art, LinearLayout.LayoutParams(-1, 0, 1f))
        page.addView(label("その一文字が、地球を救う。", 17f, Palette.light, Gravity.CENTER), fixed(-1, 42))

        val bestCard = row().apply {
            background = border(Palette.dark, Palette.dark)
            setPadding(dp(14), dp(5), dp(14), dp(5))
        }
        val scoreBlock = column()
        scoreBlock.addView(label("BEST SCORE", 10f, Palette.muted))
        scoreBlock.addView(label(number(store.bestScore), 23f))
        bestCard.addView(scoreBlock, weighted(1f, 55))
        val rankBlock = column().apply { gravity = Gravity.END or Gravity.CENTER_VERTICAL }
        rankBlock.addView(label("最高到達ランク", 10f, Palette.muted))
        rankBlock.addView(label(Ranks.forScore(store.bestScore).name, 17f))
        bestCard.addView(rankBlock, fixed(-2, 55))
        page.addView(bestCard, spaced(-1, 65, bottom = 14))
        val saved = store.load()
        page.addView(button(if (saved != null) "▶  ミッションを再開" else "▶  ミッション開始", true) {
            previousBest = store.bestScore
            engine = GameEngine()
            if (saved != null) engine.restore(saved)
            showGame()
        }, spaced(-1, 56, bottom = 10))
        val links = row()
        links.addView(button("称号一覧") { showRanks() }, weighted(1f, 48).apply { marginEnd = dp(10) })
        links.addView(button("遊び方・設定") { showSettings() }, weighted(1f, 48))
        page.addView(links)
        page.addView(label("1 LETTER = 1 PT    /    3 IMPACTS = GAME OVER", 9f, Palette.mid, Gravity.CENTER), fixed(-1, 28))
    }

    private fun showGame() {
        val page = freshPage("game")
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        page.setPadding(dp(14), dp(6), dp(14), dp(6))
        val header = row()
        gameHeader = header
        val block = column()
        block.addView(label("SCORE / 撃ち落とした文字数", 10f, Palette.muted))
        scoreText = label(number(engine.score), 31f)
        block.addView(scoreText)
        header.addView(block, weighted(1f, 64))
        val shield = column().apply { gravity = Gravity.CENTER }
        shield.addView(label("SHIELD", 10f, Palette.muted, Gravity.CENTER))
        livesText = label("♥ ♥ ♥", 18f)
        shield.addView(livesText)
        header.addView(shield, fixed(75, 64))
        header.addView(button("Ⅱ") { pauseGame() }.apply {
            contentDescription = "一時停止"
            id = R.id.pause_button
        }, fixed(48, 48))
        page.addView(header)
        rankText = label("", 11f, Palette.muted)
        page.addView(rankText, fixed(-1, 24))
        rankBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 4000
            progressTintList = android.content.res.ColorStateList.valueOf(Palette.light)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(Palette.dark)
        }
        page.addView(rankBar, spaced(-1, 4, bottom = 8))
        val art = PixelScene(this).apply {
            this.engine = this@MainActivity.engine
            contentDescription = "ひらがなの隕石を迎撃する宇宙の画面"
            id = R.id.game_scene
        }
        scene = art
        page.addView(art, LinearLayout.LayoutParams(-1, 0, 1f))
        hint = label("ひらがなを入力して地球をまもれ", 11f, Palette.muted)
        page.addView(hint, fixed(-1, 29))
        val inputRow = row()
        input = KanaInput(this).apply {
            id = R.id.kana_input
            typeface = font
            textSize = 21f
            setTextColor(Palette.light)
            setHintTextColor(Palette.mid)
            hint = "ここにひらがな"
            contentDescription = "隕石のひらがなを入力"
            setPadding(dp(12), 0, dp(8), 0)
            background = border(Palette.ink, Palette.mid)
            onCommitted = { submitWord(false) }
            onKeyboardBack = { pauseGame() }
            setOnEditorActionListener { _, action, event ->
                if (action == EditorInfo.IME_ACTION_SEND || action == EditorInfo.IME_ACTION_DONE ||
                    (event?.keyCode == android.view.KeyEvent.KEYCODE_ENTER && event.action == android.view.KeyEvent.ACTION_UP)) {
                    submitWord(true)
                    true
                } else false
            }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    scene?.typed = s?.toString().orEmpty()
                }
                override fun afterTextChanged(s: Editable?) = Unit
            })
        }
        inputRow.addView(input, weighted(1f, 52).apply { marginEnd = dp(8) })
        inputRow.addView(button("発射", true) { submitWord(true) }.apply { id = R.id.fire_button }, fixed(76, 52))
        page.addView(inputRow)
        updateHud()
        root.post { adaptGameLayout() }
        beginCountdown()
    }

    private fun adaptGameLayout() {
        if (screen != "game") return
        val usableHeight = (root.height - root.paddingTop - root.paddingBottom) / resources.displayMetrics.density
        val compact = usableHeight < 390
        gameHeader?.let { header ->
            for (index in 0..1) {
                val child = header.getChildAt(index)
                val h = dp(if (compact) 48 else 64)
                if (child.layoutParams.height != h) child.layoutParams = child.layoutParams.apply { height = h }
            }
        }
        scoreText?.textSize = if (compact) 23f else 31f
        rankText?.visibility = if (compact) View.GONE else View.VISIBLE
        hint?.visibility = if (compact) View.GONE else View.VISIBLE
    }

    private fun beginCountdown() {
        overlay?.let { root.removeView(it) }; overlay = null
        engine.pause()
        countdown = 3.0
        scene?.countdown = "3"
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        input.suppressSubmission = true
        input.setText(inputBeforePause)
        input.setSelection(input.text.length)
        input.suppressSubmission = false
        inputBeforePause = ""
        input.requestFocus()
        input.postDelayed({
            if (screen == "game" && overlay == null && foreground) {
                getSystemService(InputMethodManager::class.java).showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
            }
        }, 180)
        hint?.text = "READY / 日本語キーボードに切り替えて準備"
    }

    internal fun submitWord(explicit: Boolean) {
        if (screen != "game" || countdown > 0 || engine.phase != Phase.RUNNING) return
        val value = input.text?.toString().orEmpty()
        if (value.isBlank()) return
        val meteor = engine.fire(value)
        if (meteor != null) {
            input.clearAfterLaunch()
            audio.launch()
            hint?.text = "LAUNCH  ›  ${meteor.word}"
            messageTime = 1.2
            store.save(engine)
        } else if (explicit) {
            hint?.text = "同じことばの隕石をねらおう（減点なし）"
            messageTime = 2.5
        }
    }

    private fun pauseGame() {
        if (screen != "game" || engine.phase == Phase.GAME_OVER || overlay != null) return
        inputBeforePause = if (::input.isInitialized) input.text.toString() else ""
        engine.pause()
        countdown = 0.0
        scene?.countdown = ""
        audio.pause()
        store.save(engine)
        hideKeyboard()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val card = modal("MISSION PAUSED", "ひと休み。地球は待っている。")
        card.addView(label("${number(engine.score)} PT  /  ${Ranks.forScore(engine.score).name}", 17f, Palette.light, Gravity.CENTER), spaced(-1, 46))
        card.addView(button("▶  つづける", true) { beginCountdown() }.apply { id = R.id.resume_button }, spaced(-1, 52, bottom = 10))
        card.addView(button("保存してタイトルへ") { showHome() }, spaced(-1, 48, bottom = 10))
        card.addView(button("この挑戦を終了") {
            store.clearMission()
            showResult(abandoned = true)
        }, fixed(-1, 48))
    }

    private fun showResult(abandoned: Boolean = false) {
        countdown = 0.0
        scene?.countdown = ""
        if (!abandoned) store.save(engine)
        hideKeyboard()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (!abandoned) audio.gameOver() else audio.pause()
        // Keep the short result jingle, then relinquish audio focus.
        root.postDelayed({ if (screen == "result") audio.pause() }, 1750)
        screen = "result"
        val rank = Ranks.forScore(engine.score)
        val card = modal(if (abandoned) "MISSION ENDED" else "GAME OVER",
            if (engine.score > previousBest) "NEW RECORD / 自己ベスト更新！" else "次の一文字が、次の希望になる。")
        card.addView(label(number(engine.score), 44f, Palette.light, Gravity.CENTER), spaced(-1, 68))
        card.addView(label("${engine.score}字を迎撃  /  ${engine.destroyed}個の隕石", 13f, Palette.muted, Gravity.CENTER), fixed(-1, 29))
        card.addView(label("RANK  ${rank.english}", 11f, Palette.mid, Gravity.CENTER), fixed(-1, 26))
        card.addView(label(rank.name, 28f, Palette.light, Gravity.CENTER), fixed(-1, 40))
        val next = Ranks.next(engine.score)
        card.addView(label(if (next == null) "100,000字達成。太陽系の最上位へ！" else "次の${next.name}まで ${number(next.threshold - engine.score)}字",
            12f, Palette.muted, Gravity.CENTER), fixed(-1, 28))
        card.addView(label("WAVE ${engine.wave}   /   ${engine.elapsed.toInt()}秒   /   ${engine.bestCombo} CHAIN", 11f, Palette.mid, Gravity.CENTER), spaced(-1, 28, bottom = 14))
        card.addView(button("▶  もう一度", true) {
            audio.pause()
            previousBest = store.bestScore
            engine = GameEngine()
            inputBeforePause = ""
            showGame()
        }.apply { id = R.id.retry_button }, spaced(-1, 52, bottom = 10))
        card.addView(button("タイトルへ") { showHome() }.apply { id = R.id.home_button }, fixed(-1, 48))
    }

    private fun modal(title: String, subtitle: String): LinearLayout {
        overlay?.let { root.removeView(it) }
        val veil = FrameLayout(this).apply {
            setBackgroundColor(0xE6101E19.toInt())
            isClickable = true
            isFocusable = true
        }
        val scroller = ScrollView(this).apply { isFillViewport = false }
        val card = column().apply {
            setPadding(dp(20), dp(24), dp(20), dp(22))
            background = border(Palette.ink, Palette.mid)
        }
        card.addView(label(title, 23f, Palette.light, Gravity.CENTER), fixed(-1, 39))
        card.addView(label(subtitle, 12f, Palette.muted, Gravity.CENTER).apply { minHeight = dp(36) })
        scroller.addView(card)
        veil.addView(scroller, FrameLayout.LayoutParams(-1, -2, Gravity.CENTER).apply {
            setMargins(dp(22), dp(16), dp(22), dp(16))
        })
        root.addView(veil, FrameLayout.LayoutParams(-1, -1))
        overlay = veil
        return card
    }

    private fun showRanks() {
        audio.pause()
        val page = freshPage("ranks")
        page.setPadding(dp(22), dp(10), dp(22), dp(10))
        page.addView(button("‹  タイトルへ") { showHome() }, fixed(-1, 48))
        page.addView(label("THE SOLAR LADDER", 24f), fixed(-1, 50))
        page.addView(label("称号は1回のプレイのスコアで決定。\n4,000字ごとに、新しい天体へ。", 13f, Palette.muted).apply { setLineSpacing(dp(5).toFloat(), 1f) }, spaced(-1, 55, bottom = 10))
        val scroll = ScrollView(this)
        val list = column()
        val best = Ranks.forScore(store.bestScore)
        list.addView(label("現在の最高到達  ${best.name}  /  ${number(store.bestScore)} PT", 12f, Palette.light), spaced(-1, 42, bottom = 8))
        Ranks.all.forEachIndexed { index, rank ->
            val reached = store.bestScore >= rank.threshold
            val line = row().apply {
                background = border(if (reached) Palette.dark else Palette.ink, Palette.dark)
                setPadding(dp(12), dp(9), dp(12), dp(9))
            }
            line.addView(label("%02d".format(index + 1), 15f, if (reached) Palette.light else Palette.mid), fixed(35, 45))
            val names = column()
            names.addView(label(rank.name, 18f, if (reached) Palette.light else Palette.muted))
            names.addView(label(rank.family, 10f, Palette.mid))
            line.addView(names, weighted(1f, 45))
            line.addView(label(number(rank.threshold), 16f, if (reached) Palette.light else Palette.muted, Gravity.END or Gravity.CENTER_VERTICAL), fixed(-2, 45))
            list.addView(line, spaced(-1, 68, bottom = 7))
        }
        list.addView(label("代表的な25天体を採用しています。衛星は親惑星の平均的な太陽からの距離にまとめ、同じ惑星の衛星は内側から並べています。冥王星を最上位とするゲーム独自の称号表です。", 12f, Palette.mid).apply { setLineSpacing(dp(5).toFloat(), 1f); setPadding(0, dp(8), 0, dp(18)) })
        scroll.addView(list)
        page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
    }

    private fun showSettings() {
        val page = freshPage("settings")
        page.setPadding(dp(22), dp(10), dp(22), dp(10))
        page.addView(button("‹  タイトルへ") { showHome() }, fixed(-1, 48))
        val scroll = ScrollView(this)
        val content = column()
        content.addView(label("PILOT'S MANUAL", 25f), fixed(-1, 60))
        listOf(
            "01  よんで、うつ" to "隕石はひらがなのかたち。下に書かれたことばをスマホの日本語キーボードで入力しよう。",
            "02  ロケット発射" to "ことばを確定すると自動発射。変換中でも「発射」を押せば迎撃できる。漢字に変換せず、ひらがなで入力しよう。カタカナも同じ読みとして使える。",
            "03  地球をまもる" to "着弾3回でゲームオーバー。迎撃した1字につき1点。入力ミスは減点なし。連続迎撃のCHAINに得点倍率はない。",
            "04  遠い星をめざす" to "25秒ごとにWAVEが進み、隕石の数・速さ・ことばの長さが増える。称号は1プレイのスコアで決まり、4,000点ごとに昇格。100,000点で冥王星へ。",
            "05  途中でひと休み" to "一時停止、アプリの切り替え、画面ロックで挑戦を保存。タイトルの「ミッションを再開」で続きから遊べる。再開時は3秒のカウントダウン。",
        ).forEach { (title, body) ->
            content.addView(label(title, 17f), spaced(-1, 38))
            content.addView(label(body, 14f, Palette.muted).apply { setLineSpacing(dp(6).toFloat(), 1f) }, spaced(-1, -2, bottom = 17))
        }
        content.addView(label("SOUND CONTROL", 21f), fixed(-1, 50))
        val musicButton = button("") {}
        fun updateMusic() { musicButton.text = "BGM   ${if (store.musicEnabled) "ON" else "OFF"}" }
        musicButton.setOnClickListener { store.musicEnabled = !store.musicEnabled; audio.settingsChanged(); updateMusic() }
        updateMusic()
        content.addView(musicButton, spaced(-1, 50, bottom = 10))
        val effectsButton = button("") {}
        fun updateEffects() { effectsButton.text = "効果音   ${if (store.effectsEnabled) "ON" else "OFF"}" }
        effectsButton.setOnClickListener { store.effectsEnabled = !store.effectsEnabled; audio.settingsChanged(); updateEffects() }
        updateEffects()
        content.addView(effectsButton, spaced(-1, 50, bottom = 18))
        content.addView(label("ORBIT RUNNER / ORIGINAL CHIPTUNE\n168 BPM · PULSE / TRIANGLE / NOISE\n音量はスマホのメディア音量で調整できます。", 11f, Palette.mid).apply { setLineSpacing(dp(5).toFloat(), 1f) }, spaced(-1, -2, bottom = 24))
        content.addView(label("CREDITS", 21f), fixed(-1, 40))
        content.addView(label("ROCKETBOY  1.0.0\n映像・音楽・効果音：本ゲーム用のオリジナル\n日本語フォント：DotGothic16 / Fontworks\n通信・広告・アカウント登録なし。\n記録と設定はこの端末に保存されます。", 12f, Palette.muted).apply { setLineSpacing(dp(6).toFloat(), 1f) }, spaced(-1, -2, bottom = 16))
        content.addView(button("フォントライセンス") { showLicense() }, spaced(-1, 48, bottom = 18))
        scroll.addView(content)
        page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
    }

    private fun showLicense() {
        val page = freshPage("license")
        page.setPadding(dp(20), dp(10), dp(20), dp(10))
        page.addView(button("‹  遊び方・設定へ") { showSettings() }, fixed(-1, 48))
        val scroll = ScrollView(this)
        scroll.addView(label(assets.open("licenses/DotGothic16-OFL.txt").bufferedReader().use { it.readText() }, 12f, Palette.muted).apply {
            setPadding(0, dp(20), 0, dp(20)); setLineSpacing(dp(4).toFloat(), 1f)
        })
        page.addView(scroll)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!foreground) return
        val dt = if (lastFrame == 0L) 0.0 else ((frameTimeNanos - lastFrame) / 1_000_000_000.0).coerceIn(0.0, .1)
        lastFrame = frameTimeNanos
        if (screen == "home" || screen == "game" && overlay == null || screen == "result") scene?.animate(dt.toFloat())
        if (screen == "game" && overlay == null) {
            if (countdown > 0) {
                countdown = (countdown - dt).coerceAtLeast(0.0)
                scene?.countdown = if (countdown > 0) ceil(countdown).toInt().toString() else ""
                if (countdown <= 0) {
                    engine.resume()
                    audio.start()
                    hint?.text = "ことばを確定、または「発射」で迎撃"
                }
            } else if (engine.phase == Phase.RUNNING) {
                val oldRank = Ranks.forScore(engine.score)
                val events = engine.tick(dt)
                events.forEach { event ->
                    scene?.event(event)
                    when (event) {
                        is GameEvent.Intercept -> {
                            audio.intercept()
                            hint?.text = "HIT  +${event.word.length} PT  /  ${event.word}"
                            messageTime = 1.3
                        }
                        is GameEvent.Impact -> {
                            audio.impact()
                            hint?.text = "着弾！ シールド残り${engine.lives}"
                            messageTime = 2.0
                        }
                        else -> Unit
                    }
                }
                val newRank = Ranks.forScore(engine.score)
                if (oldRank != newRank) { audio.rankUp(); scene?.showBanner("RANK UP / ${newRank.name}") }
                checkpointClock += dt
                if (events.isNotEmpty() || checkpointClock > 5) { store.save(engine); checkpointClock = 0.0 }
                if (messageTime > 0) {
                    messageTime = (messageTime - dt).coerceAtLeast(0.0)
                    if (messageTime == 0.0) hint?.text = "ことばを確定、または「発射」で迎撃"
                }
                updateHud()
                if (engine.phase == Phase.GAME_OVER) showResult()
            }
        }
        Choreographer.getInstance().postFrameCallback(this)
    }

    private fun updateHud() {
        val key = "${engine.score}:${engine.damage}"
        if (key == lastHud) return
        lastHud = key
        scoreText?.text = number(engine.score)
        livesText?.text = (0..2).joinToString(" ") { if (it < engine.lives) "♥" else "·" }
        livesText?.contentDescription = "シールド残り${engine.lives}"
        val next = Ranks.next(engine.score)
        rankText?.text = if (next == null) "RANK  冥王星  /  SOLAR SYSTEM COMPLETE"
            else "${Ranks.forScore(engine.score).name}  ›  ${next.name}まで ${number(next.threshold - engine.score)}字"
        rankBar?.progress = if (next == null) 4000 else (engine.score % 4000).toInt()
    }

    override fun onResume() {
        super.onResume()
        foreground = true
        lastFrame = 0
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun onPause() {
        if (screen == "game") pauseGame()
        audio.pause()
        foreground = false
        Choreographer.getInstance().removeFrameCallback(this)
        super.onPause()
    }

    override fun onDestroy() {
        Choreographer.getInstance().removeFrameCallback(this)
        audio.release()
        super.onDestroy()
    }

    private fun hideKeyboard() {
        if (::input.isInitialized) {
            input.suppressSubmission = true
            input.clearFocus()
            getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(input.windowToken, 0)
        }
    }
    private fun label(value: String, size: Float, color: Int = Palette.light, alignment: Int = Gravity.CENTER_VERTICAL) = TextView(this).apply {
        text = value; typeface = font; textSize = size; setTextColor(color); gravity = alignment
        includeFontPadding = false
    }
    private fun button(value: String, primary: Boolean = false, action: () -> Unit) = Button(this).apply {
        text = value; typeface = font; textSize = 15f; isAllCaps = false; gravity = Gravity.CENTER
        setTextColor(if (primary) Palette.ink else Palette.light)
        background = border(if (primary) Palette.light else Palette.ink, if (primary) Palette.light else Palette.mid)
        setPadding(dp(6), 0, dp(6), 0)
        stateListAnimator = null
        minimumHeight = 0; minimumWidth = 0
        setOnClickListener { action() }
    }
    private fun border(fill: Int, stroke: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE; setColor(fill); setStroke(dp(1), stroke); cornerRadius = 0f
    }
    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private fun fixed(w: Int, h: Int) = LinearLayout.LayoutParams(if (w < 0) w else dp(w), if (h < 0) h else dp(h))
    private fun weighted(weight: Float, h: Int) = LinearLayout.LayoutParams(0, dp(h), weight)
    private fun spaced(w: Int, h: Int, bottom: Int = 0) = fixed(w, h).apply { bottomMargin = dp(bottom) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun number(value: Long) = String.format(Locale.US, "%06d", value)
}
