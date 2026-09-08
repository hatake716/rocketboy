package io.github.hatake716.rocketboy

import org.junit.Assert.*
import org.junit.Test

class RanksTest {
    @Test fun exactly25EquallySpacedRanksEndAt100000() {
        assertEquals(25, Ranks.all.size)
        assertEquals(25, Ranks.all.map { it.name }.distinct().size)
        Ranks.all.forEachIndexed { index, rank -> assertEquals((index + 1) * 4000L, rank.threshold) }
        assertEquals("冥王星", Ranks.all.last().name)
        assertEquals(100000L, Ranks.all.last().threshold)
    }

    @Test fun everyThresholdRequiresTheFullScore() {
        assertEquals("訓練生", Ranks.forScore(0).name)
        Ranks.all.forEach { rank ->
            assertNotEquals(rank, Ranks.forScore(rank.threshold - 1))
            assertEquals(rank, Ranks.forScore(rank.threshold))
            assertEquals(rank, Ranks.forScore(rank.threshold + 1))
        }
    }

    @Test fun plutoRemainsTheTopRankBeyond100000() {
        assertNotEquals("冥王星", Ranks.forScore(99999).name)
        assertEquals("冥王星", Ranks.forScore(100000).name)
        assertEquals("冥王星", Ranks.forScore(5000000).name)
        assertNull(Ranks.next(100000))
        assertEquals(1f, Ranks.progress(100000))
    }

    @Test fun rankIsPerMissionAndNeverTheSumOfGames() {
        val firstScore = 2500L
        val secondScore = 3000L
        assertEquals("訓練生", Ranks.forScore(firstScore).name)
        assertEquals("訓練生", Ranks.forScore(secondScore).name)
        assertEquals("訓練生", Ranks.forScore(maxOf(firstScore, secondScore)).name)
    }
}
