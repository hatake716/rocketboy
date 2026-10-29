package io.github.hatake716.rocketboy

/** Points count the hiragana reading entered by the player, including small kana. */
object Scoring {
    const val LETTER_POINTS = 10
    const val QUOTE_LETTER_POINTS = 300
    fun word(reading: String) = reading.codePointCount(0, reading.length) * LETTER_POINTS
    fun quote(reading: String) = reading.codePointCount(0, reading.length) * QUOTE_LETTER_POINTS
    fun bonusPercent(startLevel: Int) = startLevel
    fun earned(base: Int, startLevel: Int): Int {
        require(base >= 0 && startLevel in 1..PlayOptions.MAX_START_LEVEL)
        return (base.toLong() * (100 + bonusPercent(startLevel)) / 100).toInt()
    }
}
