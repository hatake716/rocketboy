package io.github.hatake716.rocketboy

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.content.FileProvider
import java.io.File
import java.util.Locale

/** Immutable result: returning from another app cannot change the score being shared. */
data class CompetitiveResult(val missionId: String, val score: Long, val wave: Int,
                             val seconds: Int, val destroyed: Int, val chain: Int, val abandoned: Boolean) {
    val caption get() = "ROCKETBOY 競技モード\n${String.format(Locale.US, "%,d", score)}点 / ${Ranks.forScore(score).name}" +
        " / WAVE $wave${if (abandoned) "（途中終了）" else ""}\n#rocketboy"

    fun toBundle() = Bundle().apply {
        putString("missionId", missionId); putLong("score", score); putInt("wave", wave)
        putInt("seconds", seconds); putInt("destroyed", destroyed); putInt("chain", chain)
        putBoolean("abandoned", abandoned)
    }

    companion object {
        fun from(engine: GameEngine, abandoned: Boolean) = CompetitiveResult(engine.missionId, engine.score,
            engine.wave, engine.elapsed.toInt(), engine.destroyed, engine.bestCombo, abandoned)

        fun fromBundle(bundle: Bundle): CompetitiveResult? = runCatching {
            CompetitiveResult(requireNotNull(bundle.getString("missionId")), bundle.getLong("score"),
                bundle.getInt("wave"), bundle.getInt("seconds"), bundle.getInt("destroyed"),
                bundle.getInt("chain"), bundle.getBoolean("abandoned")).also {
                require(it.score >= 0 && it.wave >= 1 && it.seconds >= 0 && it.destroyed >= 0 && it.chain >= 0)
            }
        }.getOrNull()
    }
}

/** The displayed score panel and exported PNG use the very same 320-pixel image. */
class CompetitiveScoreCard(context: Context, result: CompetitiveResult) : View(context) {
    private val palette = CelestialPalettes.forScore(result.score)
    private val pixels = Bitmap.createBitmap(320, 360, Bitmap.Config.RGB_565)
    private val paint = Paint().apply { isAntiAlias = false; isFilterBitmap = false }
    private val destination = Rect()

    init {
        contentDescription = "競技スコア画像。${result.caption}。${result.seconds}秒、${result.destroyed}個迎撃、最大${result.chain}連続迎撃。残機1、落下速度1.5倍、開始WAVE 1。"
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        val c = Canvas(pixels)
        c.drawColor(palette.ink)
        fun box(x: Int, y: Int, w: Int, h: Int, color: Int) {
            paint.color = color
            c.drawRect(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat(), paint)
        }
        fun text(value: String, y: Int, size: Float, color: Int = palette.light) {
            paint.color = color; paint.typeface = resources.getFont(R.font.dot_gothic)
            paint.textAlign = Paint.Align.CENTER; paint.textSize = size
            if (paint.measureText(value) > 284) paint.textSize *= 284 / paint.measureText(value)
            c.drawText(value, 160f, y.toFloat(), paint)
        }
        repeat(64) { i -> box(8 + i * 47 % 302, 9 + i * 83 % 343, if (i % 7 == 0) 2 else 1, 2, palette.dark) }
        box(0, 0, 320, 3, palette.mid); box(0, 357, 320, 3, palette.mid)
        box(0, 0, 3, 360, palette.mid); box(317, 0, 3, 360, palette.mid)
        text("ROCKETBOY", 38, 32f)
        text("COMPETITION / 競技モード", 62, 16f, palette.mid)
        box(22, 76, 276, 1, palette.mid)
        text(if (result.abandoned) "MISSION ENDED / 途中終了" else "MISSION RESULT", 99, 14f, palette.mid)
        text(String.format(Locale.US, "%,d", result.score), 145, 42f)
        text("POINTS", 165, 14f, palette.mid)
        text(Ranks.forScore(result.score).name, 204, 28f)
        text("RANK / ${Ranks.forScore(result.score).english}", 227, 14f, palette.mid)
        box(22, 242, 276, 1, palette.dark)
        text("WAVE ${result.wave}  /  ${result.seconds}秒", 264, 16f)
        text("${result.destroyed}個迎撃  /  ${result.chain} CHAIN", 287, 16f)
        text("残機1 · 速度1.5倍 · 開始W1 (+1%)", 311, 13f, palette.mid)
        box(22, 325, 276, 23, palette.light)
        text("#rocketboy", 342, 16f, palette.ink)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = resolveSize(320, widthMeasureSpec)
        setMeasuredDimension(w, resolveSize(w * 360 / 320, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        destination.set(0, 0, width, height)
        paint.color = palette.light
        canvas.drawBitmap(pixels, null, destination, paint)
    }

    fun screenshot(): Bitmap = Bitmap.createScaledBitmap(pixels, 960, 1080, false)
}

object ScoreSharing {
    const val X_PACKAGE = "com.twitter.android"

    /** Write on a worker; expose only this subdirectory via FileProvider. */
    fun saveImage(context: Context, image: Bitmap, result: CompetitiveResult): Uri {
        val directory = File(context.cacheDir, "score-shares").apply { check(mkdirs() || isDirectory) }
        // Do not remove recent shares: a receiving app may still be reading them.
        directory.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 7 * 24 * 60 * 60 * 1000L }
            ?.forEach { it.delete() }
        val file = File.createTempFile("rocketboy-${result.score}-", ".png", directory)
        try {
            file.outputStream().use { check(image.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            return FileProvider.getUriForFile(context, "${context.packageName}.score-images", file)
        } catch (error: Exception) {
            file.delete()
            throw error
        }
    }

    fun intent(result: CompetitiveResult, image: Uri, packageName: String? = null) = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, image)
        putExtra(Intent.EXTRA_TEXT, result.caption)
        clipData = ClipData.newRawUri("ROCKETBOY score", image)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (packageName != null) setPackage(packageName)
    }
}
