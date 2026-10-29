package io.github.hatake716.rocketboy

import org.junit.Assert.*
import org.junit.Test

class PlayOptionsTest {
    @Test fun highStartLevelChangesDifficultyButStartsAtZeroAndKeepsItsBonus() {
        val game = GameEngine(42, options = PlayOptions(GameMode.NORMAL, 10))
        assertEquals(0L, game.score)
        assertEquals(0.0, game.elapsed, 0.0)
        assertEquals(10, game.wave)
        assertEquals(GameEngine.difficultyAt(225.0), game.difficulty)
        val meteor = Meteor(1, "ちきゅう", 0, .3, .04)
        assertEquals(44, game.pointsFor(meteor))
        game.restore(game.snapshot().copy(elapsed = 50.0))
        assertEquals(12, game.wave)
        assertEquals(44, game.pointsFor(meteor))
        assertEquals(10, game.options.startLevel)
    }

    @Test fun bonusUsesInitialLevelAndTruncatesOncePerMeteor() {
        assertEquals(20, Scoring.earned(20, 1)) // 20.2 -> 20
        assertEquals(52, Scoring.earned(50, 5)) // 52.5 -> 52
        assertEquals(3030, Scoring.earned(3000, 1))
        assertEquals(3300, Scoring.earned(3000, 10))
        assertEquals(3480, Scoring.earned(3000, 16))
        for (level in 1..16) assertEquals(level, Scoring.bonusPercent(level))
    }

    @Test fun quoteModeStartsWithQuotesAndKeepsSpawningOnlyQuotes() {
        val game = GameEngine(51, options = PlayOptions(GameMode.QUOTES, 8)).apply { resume() }
        val ids = mutableSetOf<String>()
        repeat(1800) {
            game.tick(.05)
            game.meteors.forEach { meteor ->
                assertNotNull(meteor.quote)
                ids += meteor.quoteId!!
                game.fire(meteor.word)
            }
            assertTrue(game.meteors.size <= 1)
        }
        assertTrue(ids.size > 20)
        assertEquals(3, game.lives)
        val restored = GameEngine().apply { restore(game.snapshot()) }
        assertEquals(game.options, restored.options)
        assertEquals(game.wave, restored.wave)
        assertEquals(game.score, restored.score)
        assertEquals(Phase.PAUSED, restored.phase)
    }

    @Test fun bonusIsAddedOnlyWhenTheRocketArrivesAndSurvivesRestore() {
        val quote = Quotes.all.first()
        val game = GameEngine(options = PlayOptions(GameMode.QUOTES, 16)).apply {
            resume(); meteors += Meteor(7, quote.reading, 0, .5, .03, quoteId = quote.id)
        }
        game.fire(quote.reading)
        game.tick(.1)
        assertEquals(0L, game.score)
        val restored = GameEngine().apply { restore(game.snapshot()); resume() }
        val events = (0..5).flatMap { restored.tick(.1) }.filterIsInstance<GameEvent.Intercept>()
        val expected = quote.reading.length * 300L * 116 / 100
        assertEquals(expected, restored.score)
        assertEquals(expected.toInt(), events.single().points)
        repeat(4) { restored.tick(.1) }
        assertEquals(expected, restored.score)
    }

    @Test fun threeMissesStillEndQuoteModeWithoutAwardingPoints() {
        val game = GameEngine(options = PlayOptions(GameMode.QUOTES, 16)).apply { resume() }
        repeat(3000) { game.tick(.1) }
        assertEquals(Phase.GAME_OVER, game.phase)
        assertEquals(0L, game.score)
        assertEquals(0, game.lives)
    }

    @Test fun invalidInitialLevelsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { PlayOptions(startLevel = 0) }
        assertThrows(IllegalArgumentException::class.java) { PlayOptions(startLevel = 17) }
    }
}
