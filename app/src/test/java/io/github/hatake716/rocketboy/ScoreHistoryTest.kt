package io.github.hatake716.rocketboy

import org.junit.Assert.*
import org.junit.Test

class ScoreHistoryTest {
    @Test fun keepsTenHighestSeparateMissionsAndUpdatesTheSameMission() {
        var entries = emptyList<ScoreEntry>()
        for (i in 1..15) entries = ScoreHistory.record(entries, ScoreEntry("m$i", i * 100L, i.toLong()))
        assertEquals((15 downTo 6).map { it * 100L }, entries.map { it.score })
        entries = ScoreHistory.record(entries, ScoreEntry("m8", 2000, 99L, finished = true))
        assertEquals(10, entries.size)
        assertEquals("m8", entries.first().missionId)
        assertEquals(8L, entries.first().playedAt)
        assertTrue(entries.first().finished)
        assertEquals(1, entries.count { it.missionId == "m8" })
        assertEquals(entries, ScoreHistory.record(entries, ScoreEntry("m8", 100, 100L)))
    }

    @Test fun equalScoresRemainDistinctAndOldestWinsTheTie() {
        var entries = listOf(ScoreEntry(ScoreEntry.LEGACY_ID, 1000, 0))
        for (i in 1..12) entries = ScoreHistory.record(entries, ScoreEntry("m$i", 1000, i.toLong()))
        assertEquals(10, entries.size)
        assertTrue(entries.first().legacy)
        assertEquals("m9", entries.last().missionId)
        assertEquals(entries, ScoreHistory.record(entries, ScoreEntry("zero", 0, 99)))
    }
}
