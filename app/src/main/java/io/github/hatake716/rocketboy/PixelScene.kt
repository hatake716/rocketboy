package io.github.hatake716.rocketboy

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private data class Burst(val x: Float, val y: Float, val text: String, var age: Float = 0f,
                         val impact: Boolean = false, val shatteredWord: String = "") {
    val quotation get() = shatteredWord.isNotEmpty()
    val lifetime get() = if (quotation) 1.6f else .85f
}

/** The scene is rendered at 320 logical pixels and enlarged with nearest-neighbor sampling. */
class PixelScene(context: Context) : View(context) {
    var engine: GameEngine? = null
    var attractPalette = CelestialPalettes.cadet
        set(value) { field = value; invalidate() }
    internal val palette get() = engine?.let { CelestialPalettes.forScore(it.score) } ?: attractPalette
    var typed = ""
    var countdown = ""
    var banner = ""
    private var bannerTime = 0f
    private var clock = 0f
    private var buffer: Bitmap? = null
    private var pixelCanvas: Canvas? = null
    private val dest = Rect()
    private val paint = Paint().apply { isAntiAlias = false; isFilterBitmap = false }
    private val font: Typeface = resources.getFont(R.font.dot_gothic)
    private val bursts = mutableListOf<Burst>()
    private val stars = List(80) { index ->
        val r = Random(101L + index * 37)
        Triple(r.nextInt(10, 310).toFloat(), r.nextFloat(), r.nextInt(1, 4))
    }
    private var logicalHeight = 300f

    fun animate(seconds: Float) {
        clock += seconds
        bannerTime = (bannerTime - seconds).coerceAtLeast(0f)
        bursts.forEach { it.age += seconds }
        bursts.removeAll { it.age > it.lifetime }
        invalidate()
    }

    fun event(event: GameEvent) {
        when (event) {
            is GameEvent.Intercept -> bursts += Burst(
                if (event.quoteId != null) 160f else laneX(event.lane),
                if (event.quoteId != null) quoteTop(event.y, event.word.length) + quoteHeight(event.word.length) / 2
                    else meteorY(event.y, event.word.length), "+${event.points}",
                shatteredWord = if (event.quoteId != null) event.word else "")
            is GameEvent.Impact -> bursts += Burst(if (event.quoteId != null) 160f else laneX(event.lane),
                groundY(), "IMPACT", impact = true)
            is GameEvent.Wave -> showBanner("WAVE %02d".format(event.number))
        }
        if (bursts.size > 24) bursts.removeAt(0)
    }

    fun showBanner(text: String) { banner = text; bannerTime = 2.0f }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (w <= 0 || h <= 0) return
        buffer?.recycle()
        val bh = (h * 320f / w).toInt().coerceAtLeast(1)
        buffer = Bitmap.createBitmap(320, bh, Bitmap.Config.RGB_565)
        pixelCanvas = Canvas(buffer!!)
        logicalHeight = bh.toFloat()
        dest.set(0, 0, w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val c = pixelCanvas ?: return
        c.drawColor(palette.ink)
        drawStars(c)
        val game = engine
        if (game == null) drawAttract(c) else drawGame(c, game)
        // Small squared brackets make the viewport feel like an LCD instrument.
        for (x in listOf(1f, 312f)) for (y in listOf(1f, logicalHeight - 8)) {
            rect(c, x, y, 7f, 1f, palette.mid)
            rect(c, if (x == 1f) x else x + 6, y, 1f, 7f, palette.mid)
        }
        paint.alpha = 255
        canvas.drawBitmap(buffer!!, null, dest, paint)
    }

    private fun drawStars(c: Canvas) {
        stars.forEachIndexed { i, (x, p, size) ->
            val y = (p * logicalHeight + clock * (if (engine == null) 1.5f else 3f)) % logicalHeight
            val color = if ((clock * 1.5f + i).toInt() % 9 == 0) palette.light else palette.dark
            rect(c, x, y, if (size == 3) 2f else 1f, if (size == 3) 2f else 1f, color)
            if (size == 3 && i % 5 == 0) {
                rect(c, x - 2, y, 6f, 1f, palette.mid)
                rect(c, x, y - 2, 1f, 6f, palette.mid)
            }
        }
    }

    private fun drawAttract(c: Canvas) {
        val compact = logicalHeight < 350
        val titleTop = if (compact) 16f else 28f
        pixelText(c, "ROCKET", 35f, titleTop + 3, 7, palette.dark)
        pixelText(c, "ROCKET", 32f, titleTop, 7, palette.light)
        pixelText(c, "BOY", 101f, titleTop + 63, 7, palette.dark)
        pixelText(c, "BOY", 98f, titleTop + 60, 7, palette.light)
        text(c, "TYPE. LAUNCH. SAVE THE WORLD.", 160f, titleTop + 127, 12f, palette.mid)

        val centerY = (logicalHeight * 0.60f).coerceAtLeast(titleTop + 177)
        // Dashed launch trajectory, deliberately broken into pixel steps.
        for (i in 0..9) rect(c, 158f, centerY + 43 + i * 8, 3f, 4f, palette.dark)
        drawRocket(c, 160f, centerY + sin(clock * 2) * 3, 3)
        if (logicalHeight > 315) {
            drawKana(c, "あ", 65f, centerY - 20, false, "")
            drawKana(c, "ほし", 260f, centerY + 36, false, "")
        }
        drawEarth(c, 0)
        text(c, "EARTH DEFENSE UNIT / 01", 160f, logicalHeight - 13, 10f, palette.light)
    }

    private fun drawGame(c: Canvas, game: GameEngine) {
        val gy = groundY()
        val longest = Words.forWave(game.wave).maxOf { it.length }
        val footprint = kanaSize(longest) + (kanaRows(longest) - 1) * 19 + 25 + if (longest > 8) 16 else 0
        val travel = meteorY(1.0, longest) - meteorY(0.0, longest)
        game.minimumLaneGap = (footprint / travel.coerceAtLeast(1f)).toDouble().coerceIn(.30, 1.0)
        for (x in listOf(80f, 240f)) {
            for (i in 0..12) rect(c, x, 12 + i * ((gy - 15) / 13), 1f, 2f, palette.dark)
        }
        text(c, "WAVE %02d".format(game.wave), 15f, 20f, 12f, palette.mid, false)
        text(c, "%02d:%02d".format(game.elapsed.toInt() / 60, game.elapsed.toInt() % 60), 303f, 20f, 12f, palette.mid, alignRight = true)

        drawEarth(c, game.damage)
        for (x in 0..320 step 8) rect(c, x.toFloat(), gy + 12, 4f, 1f, palette.mid)
        drawRocket(c, 160f, gy + 22, 1)
        rect(c, 143f, gy + 34, 34f, 4f, palette.mid)
        rect(c, 137f, gy + 38, 46f, 4f, palette.dark)

        game.meteors.forEach { meteor ->
            val quotation = meteor.quote
            val x = if (quotation != null) 160f else laneX(meteor.lane)
            val y = if (quotation != null) quoteTop(meteor.y, meteor.word.length) + quoteHeight(meteor.word.length) / 2
                else meteorY(meteor.y, meteor.word.length)
            if (quotation != null) drawQuote(c, meteor, quotation)
            else drawKana(c, meteor.word, x, y, meteor.targeted, typed)
            if (meteor.targeted) {
                val t = meteor.rocket.toFloat().coerceIn(0f, 1f)
                val rx = 160 + (x - 160) * t
                val ry = gy + 15 + (y - gy - 15) * t
                for (i in 1..5) rect(c, rx - 1, ry + 6 + i * 4, 2f, 3f, if (i < 3) palette.mid else palette.dark)
                drawRocket(c, rx, ry, 1)
            }
        }

        bursts.forEach { burst ->
            if (burst.quotation) {
                drawQuoteBurst(c, burst)
                return@forEach
            }
            val radius = 7 + burst.age * (if (burst.impact) 100 else 43)
            repeat(10) { i ->
                val angle = i * Math.PI * 2 / 10
                val x = burst.x + cos(angle).toFloat() * radius
                val y = burst.y + sin(angle).toFloat() * radius
                val size = (4 - burst.age * 4).coerceAtLeast(1f)
                rect(c, x, y, size, size, if (i % 2 == 0) palette.light else palette.mid)
            }
            if (!burst.impact) text(c, burst.text, burst.x, burst.y - 16 - burst.age * 24, 16f, palette.light)
        }
        if (!compact) {
            if (game.combo >= 3) text(c, "${game.combo} CHAIN", 304f, gy + 31, 12f, palette.light, alignRight = true)
            text(c, "EARTH", 16f, gy + 31, 10f, palette.light, false)
            repeat(game.maxLives) { index -> drawHeart(c, 16f + index * 13, gy + 39, index < game.lives) }
        }

        if (bannerTime > 0 && game.meteors.none { it.quote != null } && bursts.none { it.quotation }) {
            rect(c, 58f, 31f, 204f, 27f, palette.light)
            text(c, banner, 160f, 50f, 16f, palette.ink)
        }
        if (countdown.isNotEmpty()) {
            rect(c, 125f, logicalHeight / 2 - 36, 70f, 66f, palette.ink)
            text(c, countdown, 160f, logicalHeight / 2 + 16, 48f, palette.light)
        }
        if (game.lives == 1 && (clock * 2).toInt() % 2 == 0) {
            rect(c, 3f, 3f, 314f, 1f, palette.mid)
            if (!compact) text(c, "LAST SHIELD", 160f, gy + 56, 10f, palette.mid)
        }
    }

    /** All debris follows a fixed trajectory: animation never changes the mission clock or RNG. */
    private fun drawQuoteBurst(c: Canvas, burst: Burst) {
        val t = burst.age
        val cx = burst.x
        val cy = burst.y.coerceIn(65f, (groundY() - 25).coerceAtLeast(65f))
        val color = if (t < 1.2f) palette.light else palette.mid
        // A single bright core opens into two square-edged shock waves.
        if (t < .18f) {
            val size = 12 + t * 200
            rect(c, cx - size, cy - 4, size * 2, 8f, palette.light)
            rect(c, cx - 4, cy - size, 8f, size * 2, palette.light)
        }
        repeat(2) { ring ->
            val radius = (t - ring * .12f) * 155
            if (radius > 0 && radius < 190) repeat(64) { i ->
                val angle = i * Math.PI * 2 / 64
                val x = cx + cos(angle).toFloat() * radius
                val y = cy + sin(angle).toFloat() * radius * .72f
                val size = if (ring == 0) 3f else 2f
                rect(c, x, y, size, size, if (ring == 0) color else palette.mid)
            }
        }
        // Eight long rays and their separated afterimages fill the playfield.
        if (t < .8f) repeat(8) { ray ->
            val angle = ray * Math.PI / 4 + .16
            repeat(13) { segment ->
                val distance = 12 + t * 110 + segment * 7
                val x = cx + cos(angle).toFloat() * distance
                val y = cy + sin(angle).toFloat() * distance * .7f
                rect(c, x, y, if (segment % 3 == 0) 5f else 3f, 3f,
                    if (segment < 8) palette.light else palette.mid)
            }
        }
        repeat(40) { i ->
            val angle = i * 2.399963 + burst.shatteredWord.length * .17
            val speed = 38 + (i * 29 % 100)
            val x = cx + cos(angle).toFloat() * (10 + t * speed)
            val y = cy + sin(angle).toFloat() * (10 + t * speed) * .75f + t * t * 28
            val size = (5 - t * 2 - i % 3).coerceAtLeast(1f)
            rect(c, x - cos(angle).toFloat() * 8, y - sin(angle).toFloat() * 6, 2f, 2f, palette.dark)
            rect(c, x, y, size, size, if (i % 3 == 0) palette.mid else color)
            if (i % 5 == 0 && t < 1.15f) {
                rect(c, x - 3, y + 1, 9f, 1f, color)
                rect(c, x + 1, y - 3, 1f, 9f, color)
            }
        }
        repeat(minOf(10, burst.shatteredWord.length)) { i ->
            val angle = i * Math.PI * 2 / minOf(10, burst.shatteredWord.length) - .4
            val distance = 30 + t * (48 + i % 3 * 17)
            text(c, burst.shatteredWord[i].toString(), cx + cos(angle).toFloat() * distance,
                cy + sin(angle).toFloat() * distance * .7f + t * t * 20, 16f,
                if (t < .9f) palette.light else palette.mid)
        }
        if (t < 1.45f) {
            val y = (cy - 28 - t * 16).coerceIn(62f, (logicalHeight - 62).coerceAtLeast(62f))
            rect(c, 68f, y - 22, 184f, 63f, palette.dark)
            rect(c, 64f, y - 26, 184f, 63f, color)
            rect(c, 67f, y - 23, 178f, 57f, palette.ink)
            text(c, "名言撃破！", 156f, y - 2, 20f, color)
            text(c, "${burst.text} PT", 156f, y + 25, 24f, color)
        }
    }

    private fun quoteAuthorHeight() = if (logicalHeight >= 180) 22 else 0
    private fun quoteRows(length: Int): Int {
        val availableRows = ((groundY() - 64 - quoteAuthorHeight()) / 18).toInt().coerceIn(1, 4)
        return ((length + 15) / 16).coerceAtMost(availableRows)
    }
    private fun quoteHeight(length: Int) = 30f + quoteRows(length) * 18 + quoteAuthorHeight()
    private fun quoteTop(y: Double, length: Int): Float =
        30f + y.toFloat() * (groundY() - quoteHeight(length) - 34f).coerceAtLeast(0f)

    internal fun quoteBounds(meteor: Meteor) = android.graphics.RectF(16f, quoteTop(meteor.y, meteor.word.length),
        304f, quoteTop(meteor.y, meteor.word.length) + quoteHeight(meteor.word.length))

    private fun drawQuote(c: Canvas, meteor: Meteor, quote: HistoricalQuote) {
        val bounds = quoteBounds(meteor)
        val top = bounds.top
        val color = if (meteor.targeted) palette.mid else palette.light
        rect(c, bounds.left, top, bounds.width(), bounds.height(), palette.mid)
        rect(c, bounds.left + 2, top + 2, bounds.width() - 4, bounds.height() - 4, palette.ink)
        rect(c, bounds.left + 2, top + 2, bounds.width() - 4, 21f, palette.dark)
        val prefix = GameEngine.normalizeKana(typed)
        val lines = quote.reading.chunked(16)
        val visibleRows = quoteRows(quote.reading.length)
        // Keep full-size pixel glyphs when an IME leaves little space. The next
        // lines appear as the player types; matching still uses the full reading.
        val startRow = (if (quote.reading.startsWith(prefix)) prefix.length / 16 else 0)
            .coerceIn(0, (lines.size - visibleRows).coerceAtLeast(0))
        val points = engine?.pointsFor(meteor) ?: quote.points
        text(c, if (visibleRows < lines.size) "名言 ${startRow + 1}〜${startRow + visibleRows}/${lines.size}行 ·${points}PT"
            else "名言 / ${points} PT", 160f, top + 18, 16f, color)
        lines.drop(startRow).take(visibleRows).forEachIndexed { index, line ->
            val baseline = top + 40 + index * 18
            text(c, line, 160f, baseline, 16f, color)
            if (!meteor.targeted && prefix.isNotBlank() && quote.reading.startsWith(prefix)) {
                val count = (prefix.length - (startRow + index) * 16).coerceIn(0, line.length)
                rect(c, 160f - line.length * 8, baseline + 2, count * 16f, 2f, palette.light)
            }
        }
        if (quoteAuthorHeight() > 0) text(c, quote.author.name.let { if (it.length > 17) it.take(16) + "…" else it },
            160f, bounds.bottom - 7, 16f, palette.muted)
    }

    private fun drawKana(c: Canvas, word: String, x: Float, y: Float, locked: Boolean, prefix: String) {
        val color = if (locked) palette.mid else palette.light
        val size = kanaSize(word.length)
        // Use complete 16px font cells; fractional font sizes lose pixel strokes.
        val rows = word.chunked(kanaColumns(word.length))
        val top = y - (rows.size - 1) * 9f
        rows.forEachIndexed { row, chars ->
            val baseline = top + row * 19
            for (i in 0..3) rect(c, x - chars.length * 8 + i * 8, baseline - 28 - i * 4, 2f, 7f, palette.dark)
            text(c, chars, x + 2, baseline + 2, size, palette.dark)
            text(c, chars, x, baseline, size, color)
        }
        val labelY = top + (rows.size - 1) * 19 + 18
        // A full, readable reading beneath every kana-shaped meteor.
        val labelSize = 16f
        paint.typeface = font; paint.textSize = labelSize
        val reading = if (word.length > 8) word.chunked(5) else listOf(word)
        val width = reading.maxOf { paint.measureText(it) }
        rect(c, x - width / 2 - 4, labelY - labelSize, width + 8, reading.size * labelSize + 5, palette.dark)
        reading.forEachIndexed { index, line -> text(c, line, x, labelY + index * 16, labelSize, color) }
        val normalized = GameEngine.normalizeKana(prefix)
        if (!locked && normalized.isNotBlank() && word.startsWith(normalized)) {
            var offset = 0
            reading.forEachIndexed { index, line ->
                val matched = (normalized.length - offset).coerceIn(0, line.length)
                if (matched > 0) {
                    val left = x - paint.measureText(line) / 2
                    val matchedWidth = paint.measureText(line.take(matched))
                    rect(c, left, labelY + index * 16 + 3, matchedWidth, 2f, palette.light)
                }
                offset += line.length
            }
            rect(c, x - width / 2 - 7, labelY - 10, 2f, 10f, palette.light)
            rect(c, x + width / 2 + 5, labelY - 10, 2f, 10f, palette.light)
        }
    }

    private fun drawEarth(c: Canvas, damage: Int) {
        val cy = logicalHeight + if (engine != null && compact) 122 else 100
        val radius = if (engine != null && compact) 150f else 166f
        // Horizontal 4px strips and 4px land tiles keep the horizon genuinely pixelated.
        for (yy in (cy - radius).toInt()..logicalHeight.toInt() step 4) {
            val dy = yy - cy
            val half = sqrt((radius * radius - dy * dy).coerceAtLeast(0f))
            rect(c, 160 - half, yy.toFloat(), half * 2, 4f, palette.mid)
            if (half > 10 && yy > cy - radius + 4) rect(c, 164 - half, yy.toFloat(), half * 2 - 8, 4f, palette.dark)
            for (xx in (160 - half + 8).toInt() until (160 + half - 8).toInt() step 4) {
                val shape = sin(xx * .045 + yy * .07) + cos(xx * .07 - yy * .03)
                if (shape > .35) rect(c, xx.toFloat(), yy.toFloat(), 4f, 4f, palette.mid)
            }
        }
        repeat(damage) { i ->
            val x = 75 + i * 85f
            val y = logicalHeight - 22 + (i % 2) * 9
            rect(c, x - 8, y, 21f, 8f, palette.ink)
            rect(c, x - 3, y - 4, 10f, 16f, palette.ink)
        }
    }

    private fun drawRocket(c: Canvas, x: Float, y: Float, scale: Int) {
        val sprite = listOf(
            ".....11.....", "....1221....", "....1221....", "...122221...",
            "...122221...", "...120021...", "...120021...", "...122221...",
            "..11222211..", ".1212222121.", "112122221211", "111122221111",
            "...133331...", "....1331....", "....1331....", ".....11.....",
        )
        sprite.forEachIndexed { row, pixels -> pixels.forEachIndexed { column, ch ->
            val color = when (ch) {
                '0' -> palette.ink
                '1' -> palette.mid
                '2' -> palette.light
                '3' -> if ((clock * 12).toInt() % 2 == 0) palette.light else palette.mid
                else -> return@forEachIndexed
            }
            rect(c, x + (column - 6) * scale, y + (row - 8) * scale, scale.toFloat(), scale.toFloat(), color)
        } }
    }

    private fun drawHeart(c: Canvas, x: Float, y: Float, alive: Boolean) {
        val color = if (alive) palette.light else palette.ink
        listOf("0110110", "1111111", "1111111", "0111110", "0011100", "0001000").forEachIndexed { row, line ->
            line.forEachIndexed { col, ch -> if (ch == '1') rect(c, x + col, y + row, 1f, 1f, color) }
        }
    }
    private fun laneX(lane: Int) = if (lane == 0) 82f else 238f
    private val compact get() = engine != null && logicalHeight < 230
    private fun kanaColumns(length: Int) = if (compact) { if (length <= 8) 8 else 5 } else if (length <= 3) 3 else 4
    private fun kanaRows(length: Int) = (length + kanaColumns(length) - 1) / kanaColumns(length)
    private fun kanaSize(length: Int) = if (!compact && length <= 3) 32f else 16f
    private fun groundY() = logicalHeight - if (compact) 28 else 72
    private fun meteorY(y: Double, length: Int): Float {
        val rows = kanaRows(length)
        val start = if (compact) 48f + (rows - 1) * 9 else 65f
        val labelBelow = (rows - 1) * 10 + 18 + if (length > 8) 16 else 0
        val end = groundY() - labelBelow - 4
        return start + y.toFloat() * (end - start).coerceAtLeast(1f)
    }

    private fun rect(c: Canvas, x: Float, y: Float, width: Float, height: Float, color: Int) {
        paint.color = color; paint.style = Paint.Style.FILL
        c.drawRect(x.toInt().toFloat(), y.toInt().toFloat(), (x + width).toInt().toFloat(), (y + height).toInt().toFloat(), paint)
    }

    private fun text(c: Canvas, value: String, x: Float, y: Float, size: Float, color: Int,
                     centered: Boolean = true, alignRight: Boolean = false) {
        paint.color = color; paint.typeface = font; paint.textSize = size.coerceAtLeast(16f)
        paint.textAlign = if (alignRight) Paint.Align.RIGHT else if (centered) Paint.Align.CENTER else Paint.Align.LEFT
        c.drawText(value, x.toInt().toFloat(), y.toInt().toFloat(), paint)
    }

    private fun pixelText(c: Canvas, value: String, x: Float, y: Float, scale: Int, color: Int) {
        val letters = mapOf(
            'R' to listOf("11110", "10001", "10001", "11110", "10100", "10010", "10001"),
            'O' to listOf("01110", "10001", "10001", "10001", "10001", "10001", "01110"),
            'C' to listOf("01111", "10000", "10000", "10000", "10000", "10000", "01111"),
            'K' to listOf("10001", "10010", "10100", "11000", "10100", "10010", "10001"),
            'E' to listOf("11111", "10000", "10000", "11110", "10000", "10000", "11111"),
            'T' to listOf("11111", "00100", "00100", "00100", "00100", "00100", "00100"),
            'B' to listOf("11110", "10001", "10001", "11110", "10001", "10001", "11110"),
            'Y' to listOf("10001", "10001", "01010", "00100", "00100", "00100", "00100"),
        )
        value.forEachIndexed { i, ch -> letters[ch]?.forEachIndexed { row, pixels ->
            pixels.forEachIndexed { col, p -> if (p == '1') rect(c, x + (i * 6 + col) * scale,
                y + row * scale, scale.toFloat(), scale.toFloat(), color) }
        } }
    }
}
