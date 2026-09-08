package io.github.hatake716.rocketboy

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

object Palette {
    val ink = Color.rgb(16, 30, 25)
    val dark = Color.rgb(39, 65, 47)
    val mid = Color.rgb(115, 148, 83)
    val light = Color.rgb(212, 237, 155)
    val muted = Color.rgb(164, 187, 132)
}

private data class Burst(val x: Float, val y: Float, val text: String, var age: Float = 0f, val impact: Boolean = false)

/** The scene is rendered at 320 logical pixels and enlarged with nearest-neighbor sampling. */
class PixelScene(context: Context) : View(context) {
    var engine: GameEngine? = null
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
        bursts.removeAll { it.age > 0.85f }
        invalidate()
    }

    fun event(event: GameEvent) {
        when (event) {
            is GameEvent.Intercept -> bursts += Burst(laneX(event.lane), meteorY(event.y, event.word.length), "+${event.word.length}")
            is GameEvent.Impact -> bursts += Burst(laneX(event.lane), groundY(), "IMPACT", impact = true)
            is GameEvent.Wave -> showBanner("WAVE %02d".format(event.number))
        }
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
        c.drawColor(Palette.ink)
        drawStars(c)
        val game = engine
        if (game == null) drawAttract(c) else drawGame(c, game)
        // Small squared brackets make the viewport feel like an LCD instrument.
        for (x in listOf(1f, 312f)) for (y in listOf(1f, logicalHeight - 8)) {
            rect(c, x, y, 7f, 1f, Palette.mid)
            rect(c, if (x == 1f) x else x + 6, y, 1f, 7f, Palette.mid)
        }
        paint.alpha = 255
        canvas.drawBitmap(buffer!!, null, dest, paint)
    }

    private fun drawStars(c: Canvas) {
        stars.forEachIndexed { i, (x, p, size) ->
            val y = (p * logicalHeight + clock * (if (engine == null) 1.5f else 3f)) % logicalHeight
            val color = if ((clock * 1.5f + i).toInt() % 9 == 0) Palette.light else Palette.dark
            rect(c, x, y, if (size == 3) 2f else 1f, if (size == 3) 2f else 1f, color)
            if (size == 3 && i % 5 == 0) {
                rect(c, x - 2, y, 6f, 1f, Palette.mid)
                rect(c, x, y - 2, 1f, 6f, Palette.mid)
            }
        }
    }

    private fun drawAttract(c: Canvas) {
        val compact = logicalHeight < 350
        val titleTop = if (compact) 16f else 28f
        pixelText(c, "ROCKET", 35f, titleTop + 3, 7, Palette.dark)
        pixelText(c, "ROCKET", 32f, titleTop, 7, Palette.light)
        pixelText(c, "BOY", 101f, titleTop + 63, 7, Palette.dark)
        pixelText(c, "BOY", 98f, titleTop + 60, 7, Palette.light)
        text(c, "TYPE. LAUNCH. SAVE THE WORLD.", 160f, titleTop + 127, 12f, Palette.mid)

        val centerY = (logicalHeight * 0.60f).coerceAtLeast(titleTop + 177)
        // Dashed launch trajectory, deliberately broken into pixel steps.
        for (i in 0..9) rect(c, 158f, centerY + 43 + i * 8, 3f, 4f, Palette.dark)
        drawRocket(c, 160f, centerY + sin(clock * 2) * 3, 3)
        if (logicalHeight > 315) {
            drawKana(c, "あ", 65f, centerY - 20, false, "")
            drawKana(c, "ほし", 260f, centerY + 36, false, "")
        }
        drawEarth(c, 0)
        text(c, "EARTH DEFENSE UNIT / 01", 160f, logicalHeight - 13, 10f, Palette.light)
    }

    private fun drawGame(c: Canvas, game: GameEngine) {
        val gy = groundY()
        val longest = Words.forWave(game.wave).maxOf { it.length }
        val footprint = kanaSize(longest) + (kanaRows(longest) - 1) * 19 + 25 + if (longest > 8) 16 else 0
        val travel = meteorY(1.0, longest) - meteorY(0.0, longest)
        game.minimumLaneGap = (footprint / travel.coerceAtLeast(1f)).toDouble().coerceIn(.30, 1.0)
        for (x in listOf(80f, 240f)) {
            for (i in 0..12) rect(c, x, 12 + i * ((gy - 15) / 13), 1f, 2f, Palette.dark)
        }
        text(c, "WAVE %02d".format(game.wave), 15f, 20f, 12f, Palette.mid, false)
        text(c, "%02d:%02d".format(game.elapsed.toInt() / 60, game.elapsed.toInt() % 60), 303f, 20f, 12f, Palette.mid, alignRight = true)

        drawEarth(c, game.damage)
        for (x in 0..320 step 8) rect(c, x.toFloat(), gy + 12, 4f, 1f, Palette.mid)
        drawRocket(c, 160f, gy + 22, 1)
        rect(c, 143f, gy + 34, 34f, 4f, Palette.mid)
        rect(c, 137f, gy + 38, 46f, 4f, Palette.dark)

        game.meteors.forEach { meteor ->
            val x = laneX(meteor.lane)
            val y = meteorY(meteor.y, meteor.word.length)
            drawKana(c, meteor.word, x, y, meteor.targeted, typed)
            if (meteor.targeted) {
                val t = meteor.rocket.toFloat().coerceIn(0f, 1f)
                val rx = 160 + (x - 160) * t
                val ry = gy + 15 + (y - gy - 15) * t
                for (i in 1..5) rect(c, rx - 1, ry + 6 + i * 4, 2f, 3f, if (i < 3) Palette.mid else Palette.dark)
                drawRocket(c, rx, ry, 1)
            }
        }

        bursts.forEach { burst ->
            val radius = 7 + burst.age * (if (burst.impact) 100 else 43)
            repeat(10) { i ->
                val angle = i * Math.PI * 2 / 10
                val x = burst.x + cos(angle).toFloat() * radius
                val y = burst.y + sin(angle).toFloat() * radius
                val size = (4 - burst.age * 4).coerceAtLeast(1f)
                rect(c, x, y, size, size, if (i % 2 == 0) Palette.light else Palette.mid)
            }
            if (!burst.impact) text(c, burst.text, burst.x, burst.y - 16 - burst.age * 24, 16f, Palette.light)
        }
        if (!compact) {
            if (game.combo >= 3) text(c, "${game.combo} CHAIN", 304f, gy + 31, 12f, Palette.light, alignRight = true)
            text(c, "EARTH", 16f, gy + 31, 10f, Palette.light, false)
            repeat(3) { index -> drawHeart(c, 16f + index * 13, gy + 39, index < game.lives) }
        }

        if (bannerTime > 0) {
            rect(c, 58f, 31f, 204f, 27f, Palette.light)
            text(c, banner, 160f, 50f, 16f, Palette.ink)
        }
        if (countdown.isNotEmpty()) {
            rect(c, 125f, logicalHeight / 2 - 36, 70f, 66f, Palette.ink)
            text(c, countdown, 160f, logicalHeight / 2 + 16, 48f, Palette.light)
        }
        if (game.lives == 1 && (clock * 2).toInt() % 2 == 0) {
            rect(c, 3f, 3f, 314f, 1f, Palette.mid)
            if (!compact) text(c, "LAST SHIELD", 160f, gy + 56, 10f, Palette.mid)
        }
    }

    private fun drawKana(c: Canvas, word: String, x: Float, y: Float, locked: Boolean, prefix: String) {
        val color = if (locked) Palette.mid else Palette.light
        val size = kanaSize(word.length)
        // Use complete 16px font cells; fractional font sizes lose pixel strokes.
        val rows = word.chunked(kanaColumns(word.length))
        val top = y - (rows.size - 1) * 9f
        rows.forEachIndexed { row, chars ->
            val baseline = top + row * 19
            for (i in 0..3) rect(c, x - chars.length * 8 + i * 8, baseline - 28 - i * 4, 2f, 7f, Palette.dark)
            text(c, chars, x + 2, baseline + 2, size, Palette.dark)
            text(c, chars, x, baseline, size, color)
        }
        val labelY = top + (rows.size - 1) * 19 + 18
        // A full, readable reading beneath every kana-shaped meteor.
        val labelSize = 16f
        paint.typeface = font; paint.textSize = labelSize
        val reading = if (word.length > 8) word.chunked(5) else listOf(word)
        val width = reading.maxOf { paint.measureText(it) }
        rect(c, x - width / 2 - 4, labelY - labelSize, width + 8, reading.size * labelSize + 5, Palette.dark)
        reading.forEachIndexed { index, line -> text(c, line, x, labelY + index * 16, labelSize, color) }
        val normalized = GameEngine.normalizeKana(prefix)
        if (!locked && normalized.isNotBlank() && word.startsWith(normalized)) {
            val matchedWidth = paint.measureText(normalized.take(reading.first().length))
            rect(c, x - width / 2, labelY + 3, matchedWidth, 2f, Palette.light)
            rect(c, x - width / 2 - 7, labelY - 10, 2f, 10f, Palette.light)
            rect(c, x + width / 2 + 5, labelY - 10, 2f, 10f, Palette.light)
        }
    }

    private fun drawEarth(c: Canvas, damage: Int) {
        val cy = logicalHeight + if (engine != null && compact) 122 else 100
        val radius = if (engine != null && compact) 150f else 166f
        // Horizontal 4px strips and 4px land tiles keep the horizon genuinely pixelated.
        for (yy in (cy - radius).toInt()..logicalHeight.toInt() step 4) {
            val dy = yy - cy
            val half = sqrt((radius * radius - dy * dy).coerceAtLeast(0f))
            rect(c, 160 - half, yy.toFloat(), half * 2, 4f, Palette.mid)
            if (half > 10 && yy > cy - radius + 4) rect(c, 164 - half, yy.toFloat(), half * 2 - 8, 4f, Palette.dark)
            for (xx in (160 - half + 8).toInt() until (160 + half - 8).toInt() step 4) {
                val shape = sin(xx * .045 + yy * .07) + cos(xx * .07 - yy * .03)
                if (shape > .35) rect(c, xx.toFloat(), yy.toFloat(), 4f, 4f, Palette.mid)
            }
        }
        repeat(damage) { i ->
            val x = 75 + i * 85f
            val y = logicalHeight - 22 + (i % 2) * 9
            rect(c, x - 8, y, 21f, 8f, Palette.ink)
            rect(c, x - 3, y - 4, 10f, 16f, Palette.ink)
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
                '0' -> Palette.ink
                '1' -> Palette.mid
                '2' -> Palette.light
                '3' -> if ((clock * 12).toInt() % 2 == 0) Palette.light else Palette.mid
                else -> return@forEachIndexed
            }
            rect(c, x + (column - 6) * scale, y + (row - 8) * scale, scale.toFloat(), scale.toFloat(), color)
        } }
    }

    private fun drawHeart(c: Canvas, x: Float, y: Float, alive: Boolean) {
        val color = if (alive) Palette.light else Palette.ink
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
