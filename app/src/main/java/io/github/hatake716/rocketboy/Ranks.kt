package io.github.hatake716.rocketboy

data class Rank(val name: String, val english: String, val family: String, val threshold: Long)

object Ranks {
    const val STEP = 4_000L
    const val PLUTO_SCORE = 100_000L
    val trainee = Rank("訓練生", "CADET", "出発地点", 0)
    // Representative roster, grouped by the parent's mean heliocentric orbit.
    // Moons in one system share that distance; their inner-to-outer order breaks ties.
    val all = listOf(
        Triple("水星", "MERCURY", "惑星"),
        Triple("金星", "VENUS", "惑星"),
        Triple("地球", "EARTH", "惑星"),
        Triple("月", "MOON", "地球の衛星"),
        Triple("火星", "MARS", "惑星"),
        Triple("フォボス", "PHOBOS", "火星の衛星"),
        Triple("ダイモス", "DEIMOS", "火星の衛星"),
        Triple("ケレス", "CERES", "準惑星"),
        Triple("木星", "JUPITER", "惑星"),
        Triple("イオ", "IO", "木星の衛星"),
        Triple("エウロパ", "EUROPA", "木星の衛星"),
        Triple("ガニメデ", "GANYMEDE", "木星の衛星"),
        Triple("カリスト", "CALLISTO", "木星の衛星"),
        Triple("土星", "SATURN", "惑星"),
        Triple("エンケラドゥス", "ENCELADUS", "土星の衛星"),
        Triple("レア", "RHEA", "土星の衛星"),
        Triple("タイタン", "TITAN", "土星の衛星"),
        Triple("イアペトゥス", "IAPETUS", "土星の衛星"),
        Triple("天王星", "URANUS", "惑星"),
        Triple("アリエル", "ARIEL", "天王星の衛星"),
        Triple("チタニア", "TITANIA", "天王星の衛星"),
        Triple("オベロン", "OBERON", "天王星の衛星"),
        Triple("海王星", "NEPTUNE", "惑星"),
        Triple("トリトン", "TRITON", "海王星の衛星"),
        Triple("冥王星", "PLUTO", "準惑星"),
    ).mapIndexed { index, (name, english, family) -> Rank(name, english, family, (index + 1) * STEP) }

    fun forScore(score: Long) = all.lastOrNull { score >= it.threshold } ?: trainee
    fun next(score: Long) = all.firstOrNull { it.threshold > score }
    fun progress(score: Long): Float = if (score >= PLUTO_SCORE) 1f
        else ((score.coerceAtLeast(0) % STEP).toFloat() / STEP)
}
