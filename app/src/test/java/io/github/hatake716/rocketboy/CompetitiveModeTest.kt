package io.github.hatake716.rocketboy

import org.junit.Assert.*
import org.junit.Test

class CompetitiveModeTest {
    @Test fun competitionFixesInitialConditionsAndPreservesNormalSpawningAndScoring() {
        val normal = GameEngine(77)
        val contest = GameEngine(77, options = PlayOptions(GameMode.COMPETITIVE))
        assertEquals(0L, contest.score)
        assertEquals(1, contest.lives)
        assertEquals(1, contest.wave)
        assertEquals(3, normal.lives)
        assertThrows(IllegalArgumentException::class.java) { PlayOptions(GameMode.COMPETITIVE, 16) }
        normal.resume(); contest.resume()
        repeat(8) { normal.tick(.1); contest.tick(.1) }
        val first = contest.meteors.single()
        assertNull(first.quote)
        assertEquals(normal.meteors.single().word, first.word)
        assertEquals(normal.meteors.single().speed * 1.5, first.speed, 1e-12)
        assertEquals(normal.pointsFor(first), contest.pointsFor(first))
    }

    @Test fun fallMultiplierAppliesToOrdinaryAndQuoteMeteorsEvenAtTheSpeedCap() {
        for (elapsed in listOf(0.0, 25.0, 150.0, 10000.0)) {
            for (quoteDue in listOf(false, true)) {
                fun next(mode: GameMode) = GameEngine(987, options = PlayOptions(mode)).apply {
                    restore(snapshot().copy(elapsed = elapsed, spawnIn = 0.0,
                        nextQuoteAt = if (quoteDue) 0.0 else elapsed + 500.0))
                    resume(); tick(.01)
                }
                val normal = next(GameMode.NORMAL)
                val contest = next(GameMode.COMPETITIVE)
                val a = normal.meteors.single(); val b = contest.meteors.single()
                assertEquals(quoteDue, b.quote != null)
                assertEquals(a.word, b.word)
                assertEquals(a.speed * 1.5, b.speed, 1e-12)
                assertEquals(normal.difficulty.spawnInterval, contest.difficulty.spawnInterval, 0.0)
                assertEquals(normal.difficulty.maxMeteors, contest.difficulty.maxMeteors)
            }
        }
    }

    @Test fun oneImpactEndsCompetitionImmediatelyAndCannotResumeOrAwardLaterHits() {
        val game = GameEngine(options = PlayOptions(GameMode.COMPETITIVE)).apply {
            resume()
            meteors += Meteor(1, "そら", 0, .999, .1)
            meteors += Meteor(2, "ほし", 1, .5, .1, targeted = true, rocket = .99)
        }
        assertEquals(listOf(GameEvent.Impact(0)), game.tick(.1))
        assertEquals(Phase.GAME_OVER, game.phase)
        assertEquals(0, game.lives)
        assertEquals(1, game.damage)
        assertEquals(0L, game.score)
        game.resume()
        assertTrue(game.tick(.1).isEmpty())
        assertNull(game.fire("ほし"))
        val restored = GameEngine().apply { restore(game.snapshot()); resume() }
        assertEquals(Phase.GAME_OVER, restored.phase)
        assertEquals(0L, restored.score)
    }

    @Test fun competitionRestoresWithoutMultiplyingSpeedAgainAndQuotesKeepTheirReward() {
        val quote = Quotes.all.first()
        val game = GameEngine(options = PlayOptions(GameMode.COMPETITIVE)).apply {
            restore(snapshot().copy(spawnIn = 100.0,
                meteors = listOf(Meteor(1, quote.reading, 0, .3, .08, quoteId = quote.id))))
            resume(); fire(quote.reading); tick(.1)
        }
        val restored = GameEngine().apply { restore(game.snapshot()) }
        assertEquals(Phase.PAUSED, restored.phase)
        assertEquals(1, restored.lives)
        assertEquals(.08, restored.meteors.single().speed, 0.0)
        restored.resume()
        repeat(10) { restored.tick(.1) }
        assertEquals(quote.reading.length * 303L, restored.score)
        assertEquals(setOf(quote.id), restored.collectedQuotes)
        assertEquals(1, restored.destroyed)
    }
}
