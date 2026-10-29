package io.github.hatake716.rocketboy

data class ScoreEntry(
    val missionId: String, val score: Long, val playedAt: Long,
    val mode: GameMode? = null, val startLevel: Int? = null, val wave: Int? = null,
    val elapsedSeconds: Int? = null, val finished: Boolean = false,
) {
    val legacy get() = missionId == LEGACY_ID
    companion object { const val LEGACY_ID = "legacy-best" }
}

/** One entry per mission, including a saved run's best score. Equal scores keep the older entry. */
object ScoreHistory {
    fun record(entries: List<ScoreEntry>, entry: ScoreEntry): List<ScoreEntry> {
        if (entry.score <= 0) return entries
        val previous = entries.firstOrNull { it.missionId == entry.missionId }
        if (previous != null && previous.score > entry.score) return entries
        val updated = entry.copy(playedAt = previous?.playedAt ?: entry.playedAt,
            finished = previous?.finished == true || entry.finished)
        return (entries.filterNot { it.missionId == entry.missionId } + updated)
            .sortedWith(compareByDescending<ScoreEntry> { it.score }.thenBy { it.playedAt }.thenBy { it.missionId })
            .take(10)
    }
}
