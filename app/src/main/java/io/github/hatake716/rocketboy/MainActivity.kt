package io.github.hatake716.rocketboy

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import android.widget.EditText
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
import java.util.concurrent.Executors
import kotlin.math.ceil

class MainActivity : ComponentActivity(), Choreographer.FrameCallback {
    internal var palette = CelestialPalettes.cadet
        private set
    private class PaletteBinding(val update: (PixelPalette) -> Unit)
    internal var engine = GameEngine()
        private set
    internal lateinit var input: KanaInput
        private set
    private lateinit var store: SessionStore
    internal lateinit var audio: ChipAudio
        private set
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
    private var selectedQuoteId: String? = null
    private var quoteCategory: QuoteCategory? = null
    private var quoteQuery = ""
    private var quotePage = 0
    private var previewTrack = CelestialMusic.cadet
    private var musicStatus: TextView? = null
    private var competitiveResult: CompetitiveResult? = null
    private val shareWorker = Executors.newSingleThreadExecutor()

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
        root = FrameLayout(this).apply { setBackgroundColor(palette.ink) }
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
                    "quotes", "music", "scores" -> showMenu()
                    "play-options" -> showHome()
                    "quote-category" -> showQuotes()
                    "quote-detail" -> showQuoteCategory(quoteCategory ?: QuoteCategory.HISTORY)
                    "quote-author" -> Quotes.find(selectedQuoteId)?.let { showQuote(it) } ?: showQuotes()
                    "license", "privacy" -> showSettings()
                    "home" -> { isEnabled = false; onBackPressedDispatcher.onBackPressed(); isEnabled = true }
                    else -> showHome()
                }
            }
        })
        val restoredResult = savedInstanceState?.getBundle("competitiveResult")?.let(CompetitiveResult::fromBundle)
        if (restoredResult != null) showCompetitiveResult(restoredResult) else showHome()
    }

    private fun freshPage(name: String): LinearLayout {
        if (screen == "music" && name != "music") audio.pause()
        musicStatus = null
        screen = name
        root.removeAllViews()
        overlay = null
        scene = null
        gameHeader = null
        scoreText = null; livesText = null; rankText = null; rankBar = null; hint = null
        lastHud = ""
        applyPalette(when (name) {
            "game" -> engine.score
            "competitive-result" -> competitiveResult?.score ?: engine.score
            else -> store.bestScore
        })
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
        header.addView(label("■  ORBITAL DEFENSE", 11f, palette.muted), weighted(1f, 36))
        header.addView(button("メニュー") { showMenu() }.apply { id = R.id.menu_button }, fixed(96, 48))
        page.addView(header)
        val art = PixelScene(this).apply {
            attractPalette = this@MainActivity.palette
            contentDescription = "ROCKETBOY。ドット絵のロケットと地球、ひらがなの隕石"
        }
        scene = art
        page.addView(art, LinearLayout.LayoutParams(-1, 0, 1f))
        page.addView(label("その一文字が、地球を救う。", 17f, palette.light, Gravity.CENTER), fixed(-1, 42))

        val bestCard = row().apply {
            background = border(palette.dark, palette.dark)
            setPadding(dp(14), dp(5), dp(14), dp(5))
        }
        val scoreBlock = column()
        scoreBlock.addView(label("BEST SCORE", 10f, palette.muted))
        scoreBlock.addView(label(number(store.bestScore), 23f))
        bestCard.addView(scoreBlock, weighted(1f, 55))
        val rankBlock = column().apply { gravity = Gravity.END or Gravity.CENTER_VERTICAL }
        rankBlock.addView(label("最高到達ランク", 10f, palette.muted))
        rankBlock.addView(label(Ranks.forScore(store.bestScore).name, 17f))
        bestCard.addView(rankBlock, fixed(-2, 55))
        bestCard.setOnClickListener { showScores() }
        bestCard.contentDescription = "自己ベストトップ10を開く"
        page.addView(bestCard, spaced(-1, 65, bottom = 10))
        val options = store.playOptions
        page.addView(button("${options.mode.title} · WAVE ${options.startLevel} · +${Scoring.bonusPercent(options.startLevel)}%  › 設定") { showPlayOptions() }
            .apply { id = R.id.play_options_button; textSize = 12f }, spaced(-1, 44, bottom = 10))
        val saved = store.load()
        page.addView(button(if (saved != null) "▶  ミッションを再開" else "▶  ミッション開始", true) {
            previousBest = store.bestScore
            engine = GameEngine(knownQuotes = store.collectedQuoteIds, options = store.playOptions)
            if (saved != null) engine.restore(saved)
            showGame()
        }, spaced(-1, 56, bottom = 10))
        val links = row()
        links.addView(button("称号一覧") { showRanks() }, weighted(1f, 48).apply { marginEnd = dp(10) })
        links.addView(button("遊び方・設定") { showSettings() }, weighted(1f, 48))
        page.addView(links)
        page.addView(label("基本点：1字 10 PT / 名言 1字 300 PT", 9f, palette.mid, Gravity.CENTER), fixed(-1, 28))
    }

    private fun showGame() {
        val page = freshPage("game")
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        page.setPadding(dp(14), dp(6), dp(14), dp(6))
        val header = row()
        gameHeader = header
        val block = column()
        block.addView(label("SCORE / 迎撃ポイント", 10f, palette.muted))
        scoreText = label(number(engine.score), 31f)
        block.addView(scoreText)
        header.addView(block, weighted(1f, 64))
        val shield = column().apply { gravity = Gravity.CENTER }
        shield.addView(label("SHIELD", 10f, palette.muted, Gravity.CENTER))
        livesText = label("♥ ♥ ♥", 18f)
        shield.addView(livesText)
        header.addView(shield, fixed(75, 64))
        header.addView(button("Ⅱ") { pauseGame() }.apply {
            contentDescription = "一時停止"
            id = R.id.pause_button
        }, fixed(48, 48))
        page.addView(header)
        rankText = label("", 11f, palette.muted)
        page.addView(rankText, fixed(-1, 24))
        rankBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 4000
        }.bindPalette { colors ->
            progressTintList = android.content.res.ColorStateList.valueOf(colors.light)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(colors.dark)
        }
        page.addView(rankBar, spaced(-1, 4, bottom = 8))
        val art = PixelScene(this).apply {
            this.engine = this@MainActivity.engine
            contentDescription = "ひらがなの隕石を迎撃する宇宙の画面"
            id = R.id.game_scene
        }
        scene = art
        page.addView(art, LinearLayout.LayoutParams(-1, 0, 1f))
        hint = label("ひらがなを入力して地球をまもれ", 11f, palette.muted)
        page.addView(hint, fixed(-1, 29))
        val inputRow = row()
        input = KanaInput(this).apply {
            id = R.id.kana_input
            typeface = font
            textSize = 21f
            bindPalette { colors ->
                setTextColor(colors.light)
                setHintTextColor(colors.mid)
                highlightColor = (colors.light and 0x00FFFFFF) or (0x66 shl 24)
                background = border(colors.ink, colors.mid)
                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    textCursorDrawable = textCursorDrawable?.mutate()?.apply { setTint(colors.light) }
                }
            }
            hint = "ここにひらがな"
            contentDescription = "隕石のひらがなを入力"
            setPadding(dp(12), 0, dp(8), 0)
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
        card.addView(label("${number(engine.score)} PT  /  ${Ranks.forScore(engine.score).name}", 17f, palette.light, Gravity.CENTER), spaced(-1, 46))
        card.addView(button("▶  つづける", true) { beginCountdown() }.apply { id = R.id.resume_button }, spaced(-1, 52, bottom = 10))
        card.addView(button("保存してタイトルへ") { showHome() }, spaced(-1, 48, bottom = 10))
        card.addView(button("この挑戦を終了") {
            store.save(engine, finished = true)
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
        root.postDelayed({ if (screen == "result" || screen == "competitive-result") audio.pause() }, 1750)
        if (engine.options.mode == GameMode.COMPETITIVE) {
            showCompetitiveResult(CompetitiveResult.from(engine, abandoned))
            return
        }
        screen = "result"
        applyPalette(engine.score)
        val rank = Ranks.forScore(engine.score)
        val card = modal(if (abandoned) "MISSION ENDED" else "GAME OVER",
            if (engine.score > previousBest) "NEW RECORD / 自己ベスト更新！" else "次の一文字が、次の希望になる。")
        card.addView(label(number(engine.score), 44f, palette.light, Gravity.CENTER), spaced(-1, 68))
        card.addView(label("${engine.score}点  /  ${engine.destroyed}個の隕石を迎撃", 13f, palette.muted, Gravity.CENTER), fixed(-1, 29))
        card.addView(label("RANK  ${rank.english}", 11f, palette.mid, Gravity.CENTER), fixed(-1, 26))
        card.addView(label(rank.name, 28f, palette.light, Gravity.CENTER), fixed(-1, 40))
        val next = Ranks.next(engine.score)
        card.addView(label(if (next == null) "100,000点達成。太陽系の最上位へ！" else "次の${next.name}まで ${number(next.threshold - engine.score)}点",
            12f, palette.muted, Gravity.CENTER), fixed(-1, 28))
        card.addView(label("${engine.options.mode.title} · 開始WAVE ${engine.options.startLevel} · +${Scoring.bonusPercent(engine.options.startLevel)}%", 11f, palette.muted, Gravity.CENTER), spaced(-1, 28))
        card.addView(label("WAVE ${engine.wave}   /   ${engine.elapsed.toInt()}秒   /   ${engine.bestCombo} CHAIN", 11f, palette.mid, Gravity.CENTER), spaced(-1, 28, bottom = 14))
        card.addView(button("▶  もう一度", true) {
            audio.pause()
            previousBest = store.bestScore
            engine = GameEngine(knownQuotes = store.collectedQuoteIds, options = engine.options)
            inputBeforePause = ""
            showGame()
        }.apply { id = R.id.retry_button }, spaced(-1, 52, bottom = 10))
        card.addView(button("タイトルへ") { showHome() }.apply { id = R.id.home_button }, fixed(-1, 48))
    }

    private fun showCompetitiveResult(result: CompetitiveResult) {
        competitiveResult = result
        countdown = 0.0
        hideKeyboard()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val content = collectionPage("competitive-result", "‹  タイトルへ") { showHome() }
        content.addView(label("競技結果 / X共有", 23f), spaced(-1, -2, bottom = 14))
        val scoreCard = CompetitiveScoreCard(this, result).apply { id = R.id.competitive_score_card }
        content.addView(scoreCard, spaced(-1, -2, bottom = 14))
        content.addView(label(result.caption, 15f).apply { id = R.id.share_caption; setTextIsSelectable(true) }, spaced(-1, -2, bottom = 12))
        val status = label("スコア画像を準備中…", 13f, palette.muted).apply { id = R.id.share_status }
        content.addView(status, spaced(-1, -2, bottom = 12))
        var imageUri: Uri? = null
        lateinit var share: Button
        fun prepareImage() {
            share.isEnabled = false
            status.text = "スコア画像を準備中…"
            val bitmap = scoreCard.screenshot()
            shareWorker.execute {
                val exported = runCatching { ScoreSharing.saveImage(applicationContext, bitmap, result) }
                bitmap.recycle()
                runOnUiThread {
                    if (isDestroyed || screen != "competitive-result" || competitiveResult !== result) return@runOnUiThread
                    imageUri = exported.getOrNull()
                    share.isEnabled = true
                    share.text = if (imageUri != null) "Xで共有（画像＋#rocketboy）" else "画像の作成を再試行"
                    status.text = if (imageUri != null)
                        "画像と文章をXの投稿作成画面へ送ります。X未インストール時は共有先を選べます。"
                    else "画像を作成できませんでした。空き容量を確認して再試行してください。"
                }
            }
        }
        share = button("スコア画像を準備中…", true) {
            val uri = imageUri
            if (uri == null) prepareImage() else {
                val xIntent = ScoreSharing.intent(result, uri, ScoreSharing.X_PACKAGE)
                try {
                    if (xIntent.resolveActivity(packageManager) != null) startActivity(xIntent)
                    else startActivity(Intent.createChooser(ScoreSharing.intent(result, uri), "スコア画像を共有"))
                } catch (_: ActivityNotFoundException) {
                    try { startActivity(Intent.createChooser(ScoreSharing.intent(result, uri), "スコア画像を共有")) }
                    catch (_: ActivityNotFoundException) { status.text = "画像を共有できるアプリが見つかりませんでした。" }
                }
            }
        }.apply { id = R.id.share_x_button; minHeight = dp(56) }
        content.addView(share, spaced(-1, -2, bottom = 12))
        content.addView(button("▶  もう一度") {
            audio.pause()
            previousBest = store.bestScore
            engine = GameEngine(knownQuotes = store.collectedQuoteIds, options = PlayOptions(GameMode.COMPETITIVE))
            inputBeforePause = ""
            showGame()
        }.apply { id = R.id.retry_button }, spaced(-1, 52, bottom = 12))
        content.addView(button("タイトルへ") { showHome() }.apply { id = R.id.home_button }, fixed(-1, 48))
        prepareImage()
    }

    private fun modal(title: String, subtitle: String): LinearLayout {
        overlay?.let { root.removeView(it) }
        val veil = FrameLayout(this).apply {
            setBackgroundColor((palette.ink and 0x00FFFFFF) or (0xE6 shl 24))
            isClickable = true
            isFocusable = true
        }
        val scroller = ScrollView(this).apply { isFillViewport = false }
        val card = column().apply {
            setPadding(dp(20), dp(24), dp(20), dp(22))
            background = border(palette.ink, palette.mid)
        }
        card.addView(label(title, 23f, palette.light, Gravity.CENTER), fixed(-1, 39))
        card.addView(label(subtitle, 12f, palette.muted, Gravity.CENTER).apply { minHeight = dp(36) })
        scroller.addView(card)
        veil.addView(scroller, FrameLayout.LayoutParams(-1, -2, Gravity.CENTER).apply {
            setMargins(dp(22), dp(16), dp(22), dp(16))
        })
        root.addView(veil, FrameLayout.LayoutParams(-1, -1))
        overlay = veil
        return card
    }

    private fun showMenu() {
        if (screen == "game") { engine.pause(); store.save(engine) }
        audio.pause()
        hideKeyboard()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val page = freshPage("menu")
        page.setPadding(dp(22), dp(10), dp(22), dp(10))
        page.addView(button("‹  タイトルへ") { showHome() }, fixed(-1, 48))
        val content = column()
        content.addView(label("MENU / メニュー", 24f), fixed(-1, 65))
        content.addView(button("名言集", true) { showQuotes() }.apply { id = R.id.quotes_button }, spaced(-1, 56, bottom = 10))
        content.addView(label("収集済み ${store.collectedQuoteIds.size} / ${Quotes.all.size}\n迎撃した名言を、人物と解説から読み返そう。", 13f, palette.muted)
            .apply { setLineSpacing(dp(5).toFloat(), 1f) }, spaced(-1, -2, bottom = 24))
        content.addView(button("プレイ設定・モード選択") { showPlayOptions() }.apply { id = R.id.play_options_button }, spaced(-1, 52, bottom = 12))
        content.addView(button("BGM試聴室") { showMusicRoom() }.apply { id = R.id.music_room_button }, spaced(-1, 52, bottom = 12))
        content.addView(button("自己ベスト TOP 10") { showScores() }.apply { id = R.id.scores_button }, spaced(-1, 52, bottom = 12))
        content.addView(button("称号一覧") { showRanks() }, spaced(-1, 52, bottom = 12))
        content.addView(button("遊び方・設定") { showSettings() }, spaced(-1, 52, bottom = 12))
        page.addView(ScrollView(this).apply { addView(content) }, LinearLayout.LayoutParams(-1, 0, 1f))
    }

    private fun showPlayOptions() {
        audio.pause(); hideKeyboard()
        val content = collectionPage("play-options", "‹  タイトルへ") { showHome() }
        var selected = store.playOptions
        var standardLevel = selected.startLevel
        article(content, "プレイ設定", "開始WAVEで隕石の速さ・数・文字の長さを選べます。スコアは0点から。開始WAVE×1％のボーナスは、その挑戦中ずっと固定です。")
        val normal = button("") {}
        val quotesOnly = button("") {}
        val competitive = button("") {}
        lateinit var minus: Button
        lateinit var plus: Button
        val level = label("", 23f, palette.light, Gravity.CENTER).apply { id = R.id.start_level_label }
        val bonus = label("", 13f, palette.muted).apply { id = R.id.bonus_label; setLineSpacing(dp(5).toFloat(), 1f) }
        fun update() {
            normal.text = "${if (selected.mode == GameMode.NORMAL) "●" else "○"} 通常モード"
            quotesOnly.text = "${if (selected.mode == GameMode.QUOTES) "●" else "○"} 名言モード"
            competitive.text = "${if (selected.mode == GameMode.COMPETITIVE) "●" else "○"} 競技モード"
            val competition = selected.mode == GameMode.COMPETITIVE
            level.text = "WAVE ${selected.startLevel}${if (competition) " 固定" else ""}"
            minus.isEnabled = !competition && selected.startLevel > 1
            plus.isEnabled = !competition && selected.startLevel < PlayOptions.MAX_START_LEVEL
            minus.alpha = if (minus.isEnabled) 1f else .35f
            plus.alpha = if (plus.isEnabled) 1f else .35f
            bonus.text = "獲得点 +${Scoring.bonusPercent(selected.startLevel)}％\n通常4字：${Scoring.earned(40, selected.startLevel)}点 / 名言10字：${Scoring.earned(3000, selected.startLevel)}点\n迎撃ごとに計算し、1点未満は切り捨てます。"
        }
        normal.id = R.id.normal_mode_button
        quotesOnly.id = R.id.quote_mode_button
        competitive.id = R.id.competitive_mode_button
        normal.setOnClickListener { selected = PlayOptions(GameMode.NORMAL, standardLevel); update() }
        quotesOnly.setOnClickListener { selected = PlayOptions(GameMode.QUOTES, standardLevel); update() }
        competitive.setOnClickListener { selected = PlayOptions(GameMode.COMPETITIVE); update() }
        content.addView(normal, spaced(-1, 52, bottom = 10))
        content.addView(quotesOnly, spaced(-1, 52, bottom = 12))
        content.addView(competitive, spaced(-1, 52, bottom = 12))
        content.addView(label("通常・名言モード：残機3。名言モードは名言だけを1つずつ出題。\n競技モード：開始WAVE 1・0点・残機1で固定。通常のお題（名言を含む）が速度1.5倍で落下。獲得点は開始WAVE 1の+1％。終了後にスコア画像をXへ共有できます。", 13f, palette.muted)
            .apply { setLineSpacing(dp(4).toFloat(), 1f) }, spaced(-1, -2, bottom = 20))
        val control = row()
        minus = button("−") { standardLevel = (selected.startLevel - 1).coerceAtLeast(1); selected = selected.copy(startLevel = standardLevel); update() }
            .apply { id = R.id.level_minus_button; contentDescription = "開始WAVEを下げる" }
        control.addView(minus, fixed(56, 52))
        control.addView(level, weighted(1f, 52))
        plus = button("＋") { standardLevel = (selected.startLevel + 1).coerceAtMost(PlayOptions.MAX_START_LEVEL); selected = selected.copy(startLevel = standardLevel); update() }
            .apply { id = R.id.level_plus_button; contentDescription = "開始WAVEを上げる" }
        control.addView(plus, fixed(56, 52))
        content.addView(control, spaced(-1, 52, bottom = 14))
        content.addView(bonus, spaced(-1, -2, bottom = 20))
        val saved = store.load()
        if (saved != null) content.addView(label("中断中：${saved.options.mode.title} / 開始WAVE ${saved.options.startLevel} / ${saved.score}点。設定の保存だけでは中断中の挑戦は変わりません。", 13f, palette.muted), spaced(-1, -2, bottom = 16))
        content.addView(button("設定を保存して戻る") { store.playOptions = selected; showHome() }
            .apply { id = R.id.save_options_button }, spaced(-1, 52, bottom = 12))
        content.addView(button(if (saved == null) "この設定でミッション開始" else "中断中の挑戦を終了して、新しく開始", true) {
            saved?.let { store.save(GameEngine().apply { restore(it) }, finished = true) }
            store.clearMission(); store.playOptions = selected
            previousBest = store.bestScore
            engine = GameEngine(knownQuotes = store.collectedQuoteIds, options = selected)
            inputBeforePause = ""
            showGame()
        }.apply { id = R.id.start_configured_button; minHeight = dp(56) }, spaced(-1, -2, bottom = 12))
        update()
    }

    private fun showScores() {
        audio.pause(); hideKeyboard()
        val content = collectionPage("scores", "‹  メニューへ") { showMenu() }
        article(content, "自己ベスト TOP 10", "各挑戦の最高得点を10件保存します。途中保存は同じ挑戦の記録を更新。同点は先に記録した挑戦を優先します。")
        val scores = store.topScores
        if (scores.isEmpty()) content.addView(label("まだ記録がありません。ミッションで隕石を迎撃しよう。", 16f), spaced(-1, -2))
        scores.forEachIndexed { index, entry ->
            val date = if (entry.playedAt > 0) java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN).format(java.util.Date(entry.playedAt)) else "日時の記録なし"
            val conditions = if (entry.legacy) "旧版の自己ベスト（モード・WAVEの記録なし）" else
                "${entry.mode?.title} / 開始WAVE ${entry.startLevel} / +${Scoring.bonusPercent(entry.startLevel ?: 1)}％\n到達WAVE ${entry.wave} / ${entry.elapsedSeconds}秒 / ${if (entry.finished) "終了" else "途中記録"}"
            article(content, "%02d  %s PT  ·  %s".format(index + 1, number(entry.score), Ranks.forScore(entry.score).name), "$conditions\n$date")
        }
    }

    private fun showMusicRoom() {
        audio.pause(); hideKeyboard()
        val content = collectionPage("music", "‹  メニューへ") { showMenu() }
        article(content, "BGM試聴室", "全26曲を自由に聴けます。曲を選ぶとループ再生。ゲーム中のBGM設定に関係なく試聴できます。音量は端末のメディア音量で調整してください。")
        musicStatus = label("停止中", 15f, palette.light).apply { id = R.id.music_status }
        content.addView(musicStatus, spaced(-1, -2, bottom = 14))
        val transport = row()
        transport.addView(button("▶ 再生") { audio.preview(previewTrack) }.apply { id = R.id.preview_play_button }, weighted(1f, 50).apply { marginEnd = dp(10) })
        transport.addView(button("■ 停止") { audio.pause() }.apply { id = R.id.preview_stop_button }, weighted(1f, 50))
        content.addView(transport, spaced(-1, 50, bottom = 22))
        (listOf(CelestialMusic.cadet) + CelestialMusic.all).forEachIndexed { index, track ->
            content.addView(button("%02d  %s · %d BPM\n%s".format(index, Ranks.forScore(track.threshold).name, track.bpm, track.title)) {
                previewTrack = track; audio.preview(track)
            }.apply { tag = "track-${track.id}"; contentDescription = "${Ranks.forScore(track.threshold).name}のBGMを再生"; minHeight = dp(68); setPadding(dp(12), dp(12), dp(12), dp(12)) }, spaced(-1, -2, bottom = 10))
        }
    }

    private fun collectionPage(name: String, back: String, action: () -> Unit): LinearLayout {
        val page = freshPage(name)
        page.setPadding(dp(22), dp(10), dp(22), dp(10))
        page.addView(button(back, action = action), fixed(-1, 48))
        val content = column().apply { setPadding(0, dp(16), 0, dp(22)) }
        page.addView(ScrollView(this).apply { addView(content) }, LinearLayout.LayoutParams(-1, 0, 1f))
        return content
    }

    private fun article(content: LinearLayout, title: String, body: String) {
        content.addView(label(title, 16f), spaced(-1, -2, bottom = 10))
        content.addView(label(body, 15f, palette.muted).apply { setLineSpacing(dp(7).toFloat(), 1f) }, spaced(-1, -2, bottom = 26))
    }

    private fun quoteLink(content: LinearLayout, quote: HistoricalQuote) {
        val work = if (quote.category.showsWorkTitle) "${quote.author.period}\n" else ""
        content.addView(button("$work${quote.text}\n${quote.author.name}  ›") { showQuote(quote) }.apply {
            setPadding(dp(14), dp(14), dp(14), dp(14))
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            minHeight = dp(76)
        }, spaced(-1, -2, bottom = 12))
    }

    private fun showQuotes() {
        val content = collectionPage("quotes", "‹  メニューへ") { showMenu() }
        content.addView(label("名言集", 28f), spaced(-1, -2, bottom = 12))
        val collected = store.collectedQuoteIds
        article(content, "COLLECTION  ${collected.size} / ${Quotes.all.size}",
            "名言の基本点は読みの文字数×300点。開始WAVE×1％のボーナスが加わります。初めての名言がここに加わります。出典はすべてWikipedia。収集記録は次の挑戦にも引き継がれます。")
        QuoteCategory.entries.forEach { category ->
            val entries = Quotes.inCategory(category)
            val count = entries.count { it.id in collected }
            content.addView(button("${category.title}\n$count / ${entries.size} 件  ›") {
                quoteQuery = ""; quotePage = 0; showQuoteCategory(category)
            }.apply {
                contentDescription = "${category.title}、収集済み$count 件、全${entries.size}件"
                tag = category.key
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12)); minHeight = dp(76)
            }, spaced(-1, -2, bottom = 12))
        }
        val remaining = Quotes.all.size - collected.size
        if (remaining > 0) article(content, "未収集  $remaining 件", "まだ出会っていない言葉が待っています。名言は未収集のものを優先して出現します。")
        article(content, "言葉を読むときに", "各項目に読み・解説・出典・表記方法を掲載しています。百人一首は一首につき一件です。訳や趣旨の要約は、詳細の表記で区別しています。")
    }

    private fun showQuoteCategory(category: QuoteCategory) {
        quoteCategory = category
        hideKeyboard()
        val content = collectionPage("quote-category", "‹  カテゴリ一覧へ") { showQuotes() }
        content.addView(label(category.title, 26f), spaced(-1, -2, bottom = 12))
        val collected = store.collectedQuoteIds
        val total = Quotes.inCategory(category)
        val count = total.count { it.id in collected }
        article(content, "COLLECTION  $count / ${total.size}", category.description)
        val search = EditText(this).apply {
            id = R.id.quote_search_input
            typeface = font; textSize = 16f
            setTextColor(palette.light); setHintTextColor(palette.muted)
            background = border(palette.ink, palette.mid)
            hint = if (category.showsWorkTitle) "名言・作品名・人物名で検索" else "名言・読み・人物名で検索"
            setSingleLine(true); setText(quoteQuery)
            imeOptions = EditorInfo.IME_ACTION_SEARCH or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }
        val submit = {
            quoteQuery = search.text.toString().trim(); quotePage = 0
            showQuoteCategory(category)
        }
        search.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_SEARCH) { submit(); true } else false
        }
        content.addView(search, spaced(-1, 52, bottom = 8))
        content.addView(button("検索") { submit() }.apply { id = R.id.quote_search_button }, spaced(-1, 48, bottom = 12))
        if (quoteQuery.isNotEmpty()) content.addView(button("検索を解除") {
            quoteQuery = ""; quotePage = 0; showQuoteCategory(category)
        }, spaced(-1, 48, bottom = 12))
        val matching = Quotes.search(category, collected, quoteQuery)
        val pages = ((matching.size + 29) / 30).coerceAtLeast(1)
        quotePage = quotePage.coerceIn(0, pages - 1)
        content.addView(label("${matching.size} 件  ·  ${quotePage + 1} / $pages ページ", 13f, palette.muted), spaced(-1, -2, bottom = 12))
        if (pages > 1) {
            val controls = row()
            controls.addView(button("‹ 前へ") { quotePage--; showQuoteCategory(category) }.apply {
                id = R.id.quote_previous_button; isEnabled = quotePage > 0
            }, weighted(1f, 48))
            controls.addView(button("次へ ›") { quotePage++; showQuoteCategory(category) }.apply {
                id = R.id.quote_next_button; isEnabled = quotePage < pages - 1
            }, weighted(1f, 48))
            content.addView(controls, spaced(-1, 48, bottom = 12))
        }
        matching.drop(quotePage * 30).take(30).forEach { quoteLink(content, it) }
        if (matching.isEmpty()) article(content,
            if (count == 0) "まだ収集されていません" else "一致する名言がありません",
            if (count == 0) "ミッションでこのカテゴリの名言を迎撃すると、解説を読めるようになります。" else "短いことばや、ひらがなの読みでも検索できます。")
        content.addView(label("未収集 ${total.size - count} 件", 13f, palette.muted), spaced(-1, -2, bottom = 12))
    }

    private fun showQuote(quote: HistoricalQuote) {
        if (quote.id !in store.collectedQuoteIds) { showQuotes(); return }
        selectedQuoteId = quote.id
        quoteCategory = quote.category
        val content = collectionPage("quote-detail", "‹  ${quote.category.title}へ") { showQuoteCategory(quote.category) }
        content.addView(label("${quote.category.title} / ${quote.wording}", 12f, palette.muted), spaced(-1, -2, bottom = 12))
        if (quote.category.showsWorkTitle)
            content.addView(label(quote.author.period, 16f, palette.muted), spaced(-1, -2, bottom = 12))
        content.addView(label(quote.text, 26f).apply { setLineSpacing(dp(6).toFloat(), 1f) }, spaced(-1, -2, bottom = 18))
        content.addView(button("${quote.author.name}  › ${if (quote.author.isWork) "出典を知る" else "人物を知る"}") { showQuoteAuthor(quote.author) }.apply {
            id = R.id.quote_author_button
            setPadding(dp(10), dp(12), dp(10), dp(12)); minHeight = dp(52)
        }, spaced(-1, -2, bottom = 24))
        article(content, "基本点 / ${quote.reading.codePointCount(0, quote.reading.length)}字 × 300点 = ${number(quote.points.toLong())}点", quote.reading)
        article(content, "名言の解説", quote.explanation)
        article(content, "出典・表記", quote.source)
        if (quote.license.isNotBlank()) article(content, "データの利用条件", quote.license)
        // Source links open only on an explicit tap; the game itself remains offline.
        content.addView(label(quote.sourceUrl, 12f, palette.muted).apply {
            autoLinkMask = android.text.util.Linkify.WEB_URLS
            text = quote.sourceUrl
            movementMethod = android.text.method.LinkMovementMethod.getInstance()
            setLinkTextColor(palette.light)
        })
    }

    private fun showQuoteAuthor(author: QuoteAuthor) {
        val content = collectionPage("quote-author", "‹  名言に戻る") { Quotes.find(selectedQuoteId)?.let { showQuote(it) } ?: showQuotes() }
        content.addView(label(author.name, 26f), spaced(-1, -2, bottom = 18))
        article(content, author.period, author.biography)
        content.addView(label("関連する収集済みの名言", 16f), spaced(-1, -2, bottom = 16))
        val related = Quotes.all.filter { it.authorId == author.id && it.id in store.collectedQuoteIds }
        related.take(30).forEach { quoteLink(content, it) }
        if (related.size > 30) content.addView(button("カテゴリ内でこの名前を検索") {
            quoteQuery = author.name; quotePage = 0; showQuoteCategory(quoteCategory ?: QuoteCategory.HISTORY)
        }, spaced(-1, 52, bottom = 12))
    }

    private fun showRanks() {
        audio.pause()
        val page = freshPage("ranks")
        page.setPadding(dp(22), dp(10), dp(22), dp(10))
        page.addView(button("‹  タイトルへ") { showHome() }, fixed(-1, 48))
        page.addView(label("THE SOLAR LADDER", 24f), fixed(-1, 50))
        page.addView(label("称号は1回のプレイのスコアで決定。\n4,000点ごとに、新しい天体へ。", 13f, palette.muted).apply { setLineSpacing(dp(5).toFloat(), 1f) }, spaced(-1, 55, bottom = 10))
        val scroll = ScrollView(this)
        val list = column()
        val best = Ranks.forScore(store.bestScore)
        list.addView(label("現在の最高到達  ${best.name}  /  ${number(store.bestScore)} PT", 12f, palette.light), spaced(-1, 42, bottom = 8))
        Ranks.all.forEachIndexed { index, rank ->
            val reached = store.bestScore >= rank.threshold
            val line = row().apply {
                background = border(if (reached) palette.dark else palette.ink, palette.dark)
                setPadding(dp(12), dp(9), dp(12), dp(9))
            }
            line.addView(label("%02d".format(index + 1), 15f, if (reached) palette.light else palette.mid), fixed(35, 45))
            val names = column()
            names.addView(label(rank.name, 18f, if (reached) palette.light else palette.muted))
            names.addView(label(rank.family, 10f, palette.mid))
            line.addView(names, weighted(1f, 45))
            line.addView(label(number(rank.threshold), 16f, if (reached) palette.light else palette.muted, Gravity.END or Gravity.CENTER_VERTICAL), fixed(-2, 45))
            list.addView(line, spaced(-1, 68, bottom = 7))
        }
        list.addView(label("代表的な25天体を採用しています。衛星は親惑星の平均的な太陽からの距離にまとめ、同じ惑星の衛星は内側から並べています。冥王星を最上位とするゲーム独自の称号表です。", 12f, palette.mid).apply { setLineSpacing(dp(5).toFloat(), 1f); setPadding(0, dp(8), 0, dp(18)) })
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
            "03  地球をまもる" to "通常・名言モードは着弾3回、競技モードは1回でゲームオーバー。競技モードは開始WAVE 1固定・落下速度1.5倍。終了画面からスコア画像をXへ共有できる。基本点は通常1字10点、名言1字300点。開始WAVE×1％を迎撃ごとに加算し、1点未満は切り捨て。小さい「ゃ・ゅ・ょ・っ」も1字。入力ミスは減点なし。連続迎撃のCHAINに得点倍率はない。",
            "04  遠い星をめざす" to "開始WAVE 1では2文字、落下時間は約16秒。プレイ設定で開始WAVE 1〜16を選べる。スコアは0点から。25秒ごとにWAVEが進み、隕石の数・速さ・ことばの長さが増える。称号は1プレイのスコアで決まり、4,000点ごとに昇格。100,000点で冥王星へ。昇格すると、その天体の専用BGMと配色に切り替わる。訓練生は緑、地球は青緑、火星は赤銅色など、25天体それぞれの色で冒険しよう。タイトルとメニューは自己ベストの到達色。新しい挑戦は訓練生の緑から始まり、途中再開では保存したスコアの色に戻る。",
            "05  名言を集めよう" to "通常モードでは名言が25秒経過後、画面が空くと出現。名言モードでは最初から名言だけを続けて出題。表示された読みを、改行や句読点なしのひらがなで入力しよう。迎撃で読みの文字数×300点とコレクションを獲得。小さな画面では入力に合わせて続きの行を表示する。メニューの名言集でカテゴリを選び、名言・解説・出典を読める。同じ名言も毎回加点、コレクションは1件だけ。",
            "07  試聴と記録" to "メニューのBGM試聴室では全26曲を自由に再生。自己ベストTOP 10には各挑戦の最高得点を保存し、同じ挑戦は記録を更新。モードと開始WAVEも確認できる。",
            "06  途中でひと休み" to "一時停止、アプリの切り替え、画面ロックで挑戦を保存。タイトルの「ミッションを再開」で続きから遊べる。再開時は3秒のカウントダウン。",
        ).forEach { (title, body) ->
            content.addView(label(title, 17f), spaced(-1, 38))
            content.addView(label(body, 14f, palette.muted).apply { setLineSpacing(dp(6).toFloat(), 1f) }, spaced(-1, -2, bottom = 17))
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
        content.addView(label("CELESTIAL ADVENTURE / ORIGINAL CHIPTUNE\n25天体 × 25曲 · ZUN進行の宇宙冒険曲\n訓練生：ORBIT RUNNER\n矩形波2声 / 波形メモリ / ノイズ\n音量はスマホのメディア音量で調整できます。", 11f, palette.mid).apply { setLineSpacing(dp(5).toFloat(), 1f) }, spaced(-1, -2, bottom = 24))
        content.addView(label("CREDITS", 21f), fixed(-1, 40))
        content.addView(label("ROCKETBOY  ${BuildConfig.VERSION_NAME}\n映像・音楽・効果音：本ゲーム用のオリジナル\n日本語フォント：DotGothic16 / Fontworks\nゲーム本体は通信・広告・アカウント登録なし。\nX共有には外部アプリを利用します。\n記録と設定はこの端末に保存されます。\nAndroidの設定によりバックアップ・端末移行の対象になります。", 12f, palette.muted).apply { setLineSpacing(dp(6).toFloat(), 1f) }, spaced(-1, -2, bottom = 16))
        content.addView(button("プライバシーポリシー") { showPrivacy() }.apply { id = R.id.privacy_button }, spaced(-1, 48, bottom = 12))
        content.addView(button("フォント・名言データのライセンス") { showLicense() }, spaced(-1, 48, bottom = 18))
        scroll.addView(content)
        page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
    }

    private fun showPrivacy() {
        val page = freshPage("privacy")
        page.setPadding(dp(20), dp(10), dp(20), dp(10))
        page.addView(button("‹  遊び方・設定へ") { showSettings() }, fixed(-1, 48))
        val scroll = ScrollView(this)
        scroll.addView(label(assets.open("privacy-ja.txt").bufferedReader().use { it.readText() }, 14f, palette.muted).apply {
            id = R.id.privacy_text
            setPadding(0, dp(20), 0, dp(20))
            setLineSpacing(dp(6).toFloat(), 1f)
            setTextIsSelectable(true)
        })
        page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
    }

    private fun showLicense() {
        val page = freshPage("license")
        page.setPadding(dp(20), dp(10), dp(20), dp(10))
        page.addView(button("‹  遊び方・設定へ") { showSettings() }, fixed(-1, 48))
        val scroll = ScrollView(this)
        val licenses = listOf("Wikipedia-Reuse.txt", "DotGothic16-OFL.txt", "Hyakunin-Unlicense.txt", "Kotowaza-MIT.txt")
            .joinToString("\n\n") { name -> name + "\n\n" + assets.open("licenses/$name").bufferedReader().use { it.readText() } }
        scroll.addView(label(licenses, 12f, palette.muted).apply {
            setPadding(0, dp(20), 0, dp(20)); setLineSpacing(dp(4).toFloat(), 1f)
            android.text.util.Linkify.addLinks(this, android.text.util.Linkify.WEB_URLS)
            movementMethod = android.text.method.LinkMovementMethod.getInstance()
            setLinkTextColor(palette.light)
        })
        page.addView(scroll)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!foreground) return
        val dt = if (lastFrame == 0L) 0.0 else ((frameTimeNanos - lastFrame) / 1_000_000_000.0).coerceIn(0.0, .1)
        lastFrame = frameTimeNanos
        if (screen == "home" || screen == "game" && overlay == null || screen == "result") scene?.animate(dt.toFloat())
        if (screen == "music") musicStatus?.let { label ->
            val status = "${if (audio.isMusicPlaying) "再生中" else "停止中"}  ${Ranks.forScore(previewTrack.threshold).name} / ${previewTrack.title}"
            if (label.text.toString() != status) label.text = status
        }
        if (screen == "game" && overlay == null) {
            if (countdown > 0) {
                countdown = (countdown - dt).coerceAtLeast(0.0)
                scene?.countdown = if (countdown > 0) ceil(countdown).toInt().toString() else ""
                if (countdown <= 0) {
                    engine.resume()
                    audio.start(engine.score)
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
                            hint?.text = if (event.quoteId != null) "名言を収集！ +${event.points} PT / メニュー → 名言集"
                                else "HIT  +${event.points} PT  /  ${event.word}"
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
                if (oldRank != newRank) {
                    audio.selectScore(engine.score)
                    audio.rankUp()
                    scene?.showBanner("RANK UP / ${newRank.name}")
                }
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
        applyPalette(engine.score)
        val key = "${engine.score}:${engine.damage}:${engine.wave}"
        if (key == lastHud) return
        lastHud = key
        scoreText?.text = number(engine.score)
        livesText?.text = (0 until engine.maxLives).joinToString(" ") { if (it < engine.lives) "♥" else "·" }
        livesText?.contentDescription = "シールド残り${engine.lives}"
        val next = Ranks.next(engine.score)
        rankText?.text = if (next == null) "RANK  冥王星  /  SOLAR SYSTEM COMPLETE"
            else "${Ranks.forScore(engine.score).name}  ›  ${next.name}まで ${number(next.threshold - engine.score)}点"
        rankText?.append("${if (engine.options.mode == GameMode.COMPETITIVE) " · 競技 ×1.5" else ""} · W${engine.wave} +${Scoring.bonusPercent(engine.options.startLevel)}%")
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

    override fun onSaveInstanceState(outState: Bundle) {
        if (screen == "competitive-result") competitiveResult?.let { outState.putBundle("competitiveResult", it.toBundle()) }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        Choreographer.getInstance().removeFrameCallback(this)
        audio.release()
        shareWorker.shutdown()
        super.onDestroy()
    }

    private fun hideKeyboard() {
        val focused = currentFocus
        if (::input.isInitialized) {
            input.suppressSubmission = true
            input.clearFocus()
        }
        focused?.clearFocus()
        getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(window.decorView.windowToken, 0)
    }
    // Recolor existing controls without replacing the EditText or its active IME composition.
    private fun <T : View> T.bindPalette(update: T.(PixelPalette) -> Unit): T {
        setTag(R.id.palette_binding, PaletteBinding { colors -> update(colors) })
        update(palette)
        return this
    }

    @Suppress("DEPRECATION")
    private fun applyPalette(score: Long) {
        val next = CelestialPalettes.forScore(score)
        if (next == palette) return
        palette = next
        root.setBackgroundColor(next.ink)
        window.statusBarColor = next.ink
        window.navigationBarColor = next.ink
        fun refresh(view: View) {
            (view.getTag(R.id.palette_binding) as? PaletteBinding)?.update?.invoke(next)
            if (view is PixelScene) view.attractPalette = next
            if (view is ViewGroup) for (i in 0 until view.childCount) refresh(view.getChildAt(i))
        }
        refresh(root)
    }

    private fun label(value: String, size: Float, color: Int = palette.light, alignment: Int = Gravity.CENTER_VERTICAL): TextView {
        val tone = palette.toneOf(color)
        return TextView(this).apply {
            text = value; typeface = font; textSize = size; gravity = alignment
            includeFontPadding = false
        }.bindPalette { colors -> setTextColor(colors[tone]) }
    }
    private fun button(value: String, primary: Boolean = false, action: () -> Unit) = Button(this).apply {
        text = value; typeface = font; textSize = 15f; isAllCaps = false; gravity = Gravity.CENTER
        bindPalette { colors ->
            setTextColor(if (primary) colors.ink else colors.light)
            background = border(if (primary) colors.light else colors.ink, if (primary) colors.light else colors.mid)
        }
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
