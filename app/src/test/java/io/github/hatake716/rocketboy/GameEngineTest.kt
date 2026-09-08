package io.github.hatake716.rocketboy

import org.junit.Assert.*
import org.junit.Test

class GameEngineTest {
    private fun advance(game: GameEngine, seconds: Double) {
        repeat((seconds * 100).toInt()) { game.tick(.01) }
    }
    private fun withMeteor(word: String = "ちきゅう", y: Double = .25): GameEngine = GameEngine(7).apply {
        resume()
        meteors += Meteor(200, word, 0, y, .05)
    }

    @Test fun successfulInterceptionScoresCharactersOnlyOnceOnArrival() {
        val game = withMeteor()
        assertNotNull(game.fire("ちきゅう"))
        assertEquals(0L, game.score)
        assertNull(game.fire("ちきゅう"))
        advance(game, .6)
        assertEquals(4L, game.score)
        assertEquals(1, game.destroyed)
        advance(game, .6)
        assertEquals(4L, game.score)
    }

    @Test fun threeImpactsEndTheMissionAndCannotScoreAfterward() {
        val game = GameEngine(2).apply { resume() }
        repeat(3) { index ->
            game.meteors += Meteor(index, "あ", index % 2, .999, .05)
            advance(game, .05)
            assertEquals(2 - index, game.lives)
            assertEquals(if (index == 2) Phase.GAME_OVER else Phase.RUNNING, game.phase)
        }
        game.meteors += Meteor(99, "そら", 1, .4, .05)
        assertNull(game.fire("そら"))
        assertTrue(game.tick(.1).isEmpty())
        assertEquals(0L, game.score)
        game.resume()
        assertEquals(Phase.GAME_OVER, game.phase)
    }

    @Test fun incorrectOrPartialTextDoesNotDamageEarthOrScore() {
        val game = withMeteor()
        assertNull(game.fire("ち"))
        assertNull(game.fire("地球"))
        assertEquals(3, game.lives)
        assertEquals(0L, game.score)
        assertFalse(game.meteors.first().targeted)
    }

    @Test fun supportsKatakanaHalfWidthAndCombiningDakuten() {
        assertEquals("ぎゃく", GameEngine.normalizeKana("ｷﾞｬｸ"))
        assertEquals("が", GameEngine.normalizeKana("か\u3099"))
        assertEquals("ちきゅう", GameEngine.normalizeKana(" チキュウ "))
        val game = withMeteor("が")
        assertNotNull(game.fire("か\u3099"))
        advance(game, .6)
        assertEquals(1L, game.score)
    }

    @Test fun pauseAndRestorePreserveRocketAndPreventDuplicatePoints() {
        val game = withMeteor("ほし")
        game.fire("ほし")
        advance(game, .1)
        game.pause()
        val before = game.snapshot()
        advance(game, 10.0)
        assertEquals(before, game.snapshot())
        val restored = GameEngine().apply { restore(before) }
        assertEquals(Phase.PAUSED, restored.phase)
        assertNull(restored.fire("ほし"))
        restored.resume()
        advance(restored, .5)
        assertEquals(2L, restored.score)
        val again = GameEngine().apply { restore(restored.snapshot()); resume() }
        advance(again, .5)
        assertEquals(2L, again.score)
    }

    @Test fun snapshotsAreIndependentCopies() {
        val game = withMeteor()
        val saved = game.snapshot()
        advance(game, .2)
        assertEquals(.25, saved.meteors.first().y, .0001)
        val restored = GameEngine().apply { restore(saved); resume() }
        advance(restored, .2)
        assertEquals(.25, saved.meteors.first().y, .0001)
    }

    @Test fun reservedMeteorCanBeInterceptedJustBeforeGround() {
        val game = withMeteor("ゆめ", .99999)
        game.fire("ゆめ")
        advance(game, .4)
        assertEquals(2L, game.score)
        assertEquals(3, game.lives)
    }

    @Test fun difficultyIncreasesAndRemainsPlayable() {
        val first = GameEngine.difficultyAt(0.0)
        val later = GameEngine.difficultyAt(200.0)
        val capped = GameEngine.difficultyAt(50000.0)
        assertTrue(later.fallSpeed > first.fallSpeed)
        assertTrue(later.spawnInterval < first.spawnInterval)
        assertTrue(later.maxMeteors > first.maxMeteors)
        assertEquals(6, capped.maxMeteors)
        assertEquals(1.6, capped.spawnInterval, 0.0)
        assertEquals(.092, capped.fallSpeed, 0.0)
        assertTrue(Words.forWave(10).minOf { it.length } > Words.forWave(2).minOf { it.length })
    }

    @Test fun allWordsAreHiraganaAndDoNotExceedDisplayLimit() {
        val all = Words.singles + Words.short + Words.medium + Words.long
        assertTrue(all.size > 140)
        assertTrue(all.all { it.length in 1..10 && it.all { ch -> ch in 'ぁ'..'ん' } })
    }

    @Test fun newMissionResetsScoreAndLives() {
        val game = withMeteor("ゆめ")
        game.fire("ゆめ")
        advance(game, .6)
        val next = GameEngine()
        assertEquals(0L, next.score)
        assertEquals(3, next.lives)
        assertNotEquals(game.missionId, next.missionId)
    }

    @Test fun spawningDoesNotOverlapTopOfLaneOrDuplicateWords() {
        val game = GameEngine(31).apply { resume() }
        repeat(20000) {
            game.tick(.02)
            assertTrue(game.meteors.size <= game.difficulty.maxMeteors)
            assertEquals(game.meteors.map { it.word }.distinct().size, game.meteors.size)
            (0..1).forEach { lane -> assertTrue(game.meteors.count { it.lane == lane && it.y < .30 } <= 1) }
            game.meteors.filter { !it.targeted && it.y > .45 }.forEach { game.fire(it.word) }
        }
        assertTrue(game.score > 0)
        assertTrue(game.wave > 10)
        assertEquals(3, game.lives)
    }

    @Test fun invalidFrameTimesDoNotAlterState() {
        val game = withMeteor()
        val snapshot = game.snapshot()
        game.tick(Double.NaN); game.tick(Double.POSITIVE_INFINITY); game.tick(-1.0)
        assertEquals(snapshot, game.snapshot())
    }
}
