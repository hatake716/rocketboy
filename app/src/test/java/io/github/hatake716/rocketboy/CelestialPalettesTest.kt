package io.github.hatake716.rocketboy

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.pow

class CelestialPalettesTest {
    @Test fun eachRankHasItsOwnPaletteAtExactlyItsScoreThreshold() {
        assertEquals(listOf(Ranks.trainee) + Ranks.all, CelestialPalettes.all.map { it.rank })
        assertEquals(26, CelestialPalettes.all.map { listOf(it.ink, it.dark, it.mid, it.light, it.muted) }.distinct().size)
        for ((index, palette) in CelestialPalettes.all.withIndex()) {
            assertEquals(palette, CelestialPalettes.forScore(palette.rank.threshold))
            if (index > 0) assertEquals(CelestialPalettes.all[index - 1], CelestialPalettes.forScore(palette.rank.threshold - 1))
        }
        assertEquals(CelestialPalettes.cadet, CelestialPalettes.forScore(Long.MIN_VALUE))
        assertEquals("PLUTO", CelestialPalettes.forScore(Long.MAX_VALUE).rank.english)
        // Starting a new mission selects green even after a high previous score.
        CelestialPalettes.forScore(150_000)
        assertEquals(CelestialPalettes.cadet, CelestialPalettes.forScore(0))
    }

    @Test fun foregroundAndBackgroundStayReadableForEveryBody() {
        fun luminance(color: Int): Double {
            fun channel(shift: Int): Double {
                val c = ((color ushr shift) and 255) / 255.0
                return if (c <= .04045) c / 12.92 else ((c + .055) / 1.055).pow(2.4)
            }
            return .2126 * channel(16) + .7152 * channel(8) + .0722 * channel(0)
        }
        fun contrast(a: Int, b: Int): Double {
            val x = luminance(a); val y = luminance(b)
            return (maxOf(x, y) + .05) / (minOf(x, y) + .05)
        }
        for (p in CelestialPalettes.all) {
            assertTrue("${p.rank.name}: meteor text", contrast(p.light, p.dark) >= 7.0)
            assertTrue("${p.rank.name}: secondary text", contrast(p.muted, p.dark) >= 4.5)
            assertTrue("${p.rank.name}: labels", contrast(p.mid, p.ink) >= 4.5)
            for (tone in PaletteTone.entries) {
                assertEquals(255, p[tone] ushr 24)
                assertEquals(tone, p.toneOf(p[tone]))
            }
        }
    }

    @Test fun cadetRetainsTheOriginalGameBoyColors() {
        val p = CelestialPalettes.cadet
        assertEquals(0xFF101E19.toInt(), p.ink)
        assertEquals(0xFF27412F.toInt(), p.dark)
        assertEquals(0xFF739453.toInt(), p.mid)
        assertEquals(0xFFD4ED9B.toInt(), p.light)
        assertEquals(0xFFA4BB84.toInt(), p.muted)
    }
}
