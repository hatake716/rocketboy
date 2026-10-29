package io.github.hatake716.rocketboy

enum class PaletteTone { INK, DARK, MID, LIGHT, MUTED }

/** Immutable, small color sets keep the pixel-art contrast consistent at every rank. */
data class PixelPalette(val rank: Rank, val motif: String, val ink: Int, val dark: Int,
                        val mid: Int, val light: Int, val muted: Int) {
    operator fun get(tone: PaletteTone): Int = when (tone) {
        PaletteTone.INK -> ink
        PaletteTone.DARK -> dark
        PaletteTone.MID -> mid
        PaletteTone.LIGHT -> light
        PaletteTone.MUTED -> muted
    }
    fun toneOf(color: Int) = PaletteTone.entries.single { this[it] == color }
}

object CelestialPalettes {
    private fun colors(rank: Rank, motif: String, ink: Int, dark: Int, mid: Int, light: Int, muted: Int) =
        PixelPalette(rank, motif, ink or (0xFF shl 24), dark or (0xFF shl 24), mid or (0xFF shl 24),
            light or (0xFF shl 24), muted or (0xFF shl 24))

    val cadet = colors(Ranks.trainee, "レトログリーン", 0x101E19, 0x27412F, 0x739453, 0xD4ED9B, 0xA4BB84)
    private fun body(english: String, motif: String, ink: Int, dark: Int, mid: Int, light: Int, muted: Int) =
        colors(Ranks.all.single { it.english == english }, motif, ink, dark, mid, light, muted)

    // Artistic palettes inspired by each body; these are not natural-color reconstructions.
    val all = listOf(cadet,
        body("MERCURY", "鋼色の岩肌", 0x151C27, 0x2C3B50, 0x93A5B8, 0xF1F4F8, 0xB8C6D6),
        body("VENUS", "琥珀色の雲", 0x241B10, 0x4D361C, 0xC79B4B, 0xFFEBAE, 0xEBC980),
        body("EARTH", "青い海と緑の大地", 0x0B1929, 0x123E53, 0x4BAABF, 0xC3F5E6, 0x91DACC),
        body("MOON", "銀色の月光", 0x1B1C24, 0x383C49, 0x9BA4B9, 0xF3F1EA, 0xC7CBD8),
        body("MARS", "赤銅色の砂漠", 0x261311, 0x522725, 0xD78565, 0xFFD9B8, 0xEDB296),
        body("PHOBOS", "錆色のクレーター", 0x231917, 0x49342B, 0xB69B78, 0xF1DFC0, 0xD3BC9E),
        body("DEIMOS", "桃色の砂塵", 0x261921, 0x50323F, 0xC292A8, 0xFFE3EA, 0xE4BCCC),
        body("CERES", "鉱石と白い輝き", 0x152321, 0x2C4540, 0x89B3A0, 0xE2F5D8, 0xBAD7C1),
        body("JUPITER", "橙色の雲の帯", 0x26190F, 0x52371E, 0xD9A15F, 0xFFE8C2, 0xEDC797),
        body("IO", "硫黄色の火山", 0x24200D, 0x484015, 0xB8AD43, 0xFFF3A2, 0xDDD17D),
        body("EUROPA", "氷の海", 0x11222B, 0x254554, 0x8AB9C6, 0xE2FAFF, 0xBCE0E8),
        body("GANYMEDE", "氷と岩の青灰色", 0x1A202D, 0x344057, 0x95A7C6, 0xE3E8FF, 0xC2CCE3),
        body("CALLISTO", "古い大地の銅色", 0x251A20, 0x4E343C, 0xBF9B92, 0xFCE4D5, 0xDFC0B2),
        body("SATURN", "金色の環", 0x241F12, 0x494020, 0xC7AF5C, 0xFFF1B8, 0xE5D294),
        body("ENCELADUS", "白い氷の噴泉", 0x122530, 0x244658, 0x85C6D5, 0xECFFFF, 0xB8E4F0),
        body("RHEA", "霜の藤色", 0x202030, 0x3D3C57, 0xABA6C9, 0xF1EFFF, 0xD0CCE6),
        body("TITAN", "オレンジ色の霞", 0x281B0E, 0x553717, 0xD1A059, 0xFFE1A1, 0xEDBE78),
        body("IAPETUS", "明暗の大地", 0x211D20, 0x423D42, 0xB0A2A4, 0xF6EDE5, 0xD6C8C5),
        body("URANUS", "淡い青緑の空", 0x0C2426, 0x19464B, 0x68BFC3, 0xC6FFF5, 0xA2E1DD),
        body("ARIEL", "翡翠色の氷", 0x162620, 0x2A493C, 0x8AC3A7, 0xDFFFE8, 0xBDE6CE),
        body("TITANIA", "紫水晶の夜", 0x251B31, 0x483254, 0xB197CA, 0xF5DFFF, 0xD8BDEB),
        body("OBERON", "紅紫色の夜明け", 0x2A1928, 0x4F304A, 0xC58DBA, 0xFFDAF1, 0xE6B5D8),
        body("NEPTUNE", "深い青の嵐", 0x10182E, 0x203661, 0x7B9BE2, 0xD2E3FF, 0xABC2F4),
        body("TRITON", "薄紅色の氷", 0x241D2D, 0x44374F, 0xB5A0C6, 0xFFE5F5, 0xDFCCE8),
        body("PLUTO", "遥かな星の紫", 0x20132F, 0x402450, 0xB58ADA, 0xF4D3FF, 0xD5B1ED),
    )

    fun forScore(score: Long): PixelPalette = all[(score.coerceIn(0, Ranks.PLUTO_SCORE) / Ranks.STEP).toInt()]
}
