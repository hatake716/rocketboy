package io.github.hatake716.rocketboy

import org.junit.Assert.*
import org.junit.Test

class CelestialMusicTest {
    @Test fun eachOfThe25RanksHasItsOwnTrackAndExactBoundary() {
        assertEquals(25, CelestialMusic.all.size)
        assertEquals(25, CelestialMusic.all.map { it.resource }.toSet().size)
        assertEquals(25, CelestialMusic.all.map { it.title }.toSet().size)
        assertEquals(CelestialMusic.cadet, CelestialMusic.forScore(0))
        Ranks.all.forEachIndexed { i, rank ->
            val track = CelestialMusic.all[i]
            assertEquals(rank.english.lowercase(), track.id)
            assertEquals(rank.threshold, track.threshold)
            assertNotEquals(track, CelestialMusic.forScore(rank.threshold - 1))
            assertEquals(track, CelestialMusic.forScore(rank.threshold))
            assertEquals(track, CelestialMusic.forScore(rank.threshold + 1))
        }
    }

    @Test fun multiRankJumpAndScoresAbovePlutoResolveToTheFinalTrack() {
        assertEquals("moon", CelestialMusic.forScore(4000 + 41 * 300L).id)
        assertEquals("pluto", CelestialMusic.forScore(100000).id)
        assertEquals("pluto", CelestialMusic.forScore(Long.MAX_VALUE).id)
        assertEquals("cadet", CelestialMusic.forScore(0).id)
    }
}
