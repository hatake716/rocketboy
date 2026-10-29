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
        assertEquals(40L, game.score)
        assertEquals(1, game.destroyed)
        advance(game, .6)
        assertEquals(40L, game.score)
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
        assertEquals(10L, game.score)
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
        assertEquals(20L, restored.score)
        val again = GameEngine().apply { restore(restored.snapshot()); resume() }
        advance(again, .5)
        assertEquals(20L, again.score)
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
        assertEquals(20L, game.score)
        assertEquals(3, game.lives)
    }

    @Test fun difficultyIncreasesAndRemainsPlayable() {
        val first = GameEngine.difficultyAt(0.0)
        val later = GameEngine.difficultyAt(200.0)
        val capped = GameEngine.difficultyAt(50000.0)
        assertEquals(16.0, 1.0 / first.fallSpeed, .001)
        assertTrue(later.fallSpeed > first.fallSpeed)
        assertTrue(later.spawnInterval < first.spawnInterval)
        assertTrue(later.maxMeteors > first.maxMeteors)
        assertEquals(6, capped.maxMeteors)
        assertEquals(1.6, capped.spawnInterval, 0.0)
        assertEquals(.092, capped.fallSpeed, 0.0)
        assertTrue(Words.forWave(10).minOf { it.length } > Words.forWave(2).minOf { it.length })
    }

    @Test fun firstWaveSpawnsOnlyTwoCharacterWords() {
        val game = GameEngine(19).apply { resume() }
        repeat(2400) {
            game.tick(.01)
            game.meteors.forEach {
                assertEquals(2, it.word.length)
                assertNull(it.quote)
                game.fire(it.word)
            }
        }
        assertTrue(game.destroyed >= 4)
    }

    @Test fun quoteRewardsReadingLengthTimes300AndCollectsOnlyOnArrivalEvenAcrossRestore() {
        val quote = Quotes.all.first()
        val game = GameEngine(2).apply {
            resume()
            meteors += Meteor(91, quote.reading, 0, .3, .05, quoteId = quote.id)
        }
        assertNull(game.fire(quote.reading.dropLast(1)))
        assertTrue(game.collectedQuotes.isEmpty())
        assertNotNull(game.fire(quote.reading))
        advance(game, .1)
        assertEquals(0L, game.score)
        assertTrue(game.collectedQuotes.isEmpty())
        val restored = GameEngine().apply { restore(game.snapshot()); resume() }
        val events = mutableListOf<GameEvent>()
        repeat(80) { events += restored.tick(.01) }
        assertEquals(quote.reading.length * 303L, restored.score)
        assertEquals(setOf(quote.id), restored.collectedQuotes)
        val hit = events.filterIsInstance<GameEvent.Intercept>().single()
        assertEquals(quote.reading.length * 303, hit.points)
        assertEquals(quote.id, hit.quoteId)
        val again = GameEngine().apply { restore(restored.snapshot()); resume() }
        advance(again, .5)
        assertEquals(quote.reading.length * 303L, again.score)
        again.meteors += Meteor(92, quote.reading, 0, .5, .05, quoteId = quote.id)
        again.fire(quote.reading)
        advance(again, .5)
        assertEquals(quote.reading.length * 606L, again.score)
        assertEquals(setOf(quote.id), again.collectedQuotes)
    }

    @Test fun missedQuoteIsNotCollectedOrRewarded() {
        val quote = Quotes.all.last()
        val game = GameEngine().apply {
            resume()
            meteors += Meteor(71, quote.reading, 0, .999, .1, quoteId = quote.id)
        }
        val impact = game.tick(.1).filterIsInstance<GameEvent.Impact>().single()
        assertEquals(quote.id, impact.quoteId)
        assertEquals(2, game.lives)
        assertEquals(0L, game.score)
        assertTrue(game.collectedQuotes.isEmpty())
    }

    @Test fun allQuotesAppearWithoutOverlapsAndUncollectedOnesHavePriority() {
        val game = GameEngine(21).apply { resume() }
        repeat(40000) {
            game.tick(.01)
            if (game.meteors.any { it.quote != null }) assertEquals(1, game.meteors.size)
            game.meteors.forEach { game.fire(it.word) }
        }
        assertTrue(game.collectedQuotes.size >= 8)
        val allIds = Quotes.all.map { it.id }.toSet()
        Quotes.all.forEach { wanted ->
            val nextGame = GameEngine(9, allIds - wanted.id).apply {
                restore(snapshot().copy(elapsed = 25.0, spawnIn = 0.0)); resume()
            }
            nextGame.tick(.01)
            assertEquals(wanted.id, nextGame.meteors.single().quoteId)
        }
    }

    @Test fun quoteCatalogFitsInputAndHasDistinctIdsReadingsAndSources() {
        assertEquals(QuoteCategory.entries.toSet(), Quotes.all.map { it.category }.toSet())
        assertEquals(100, Quotes.inCategory(QuoteCategory.HYAKUNIN).size)
        assertEquals(Quotes.all.size, Quotes.all.map { it.id }.toSet().size)
        assertEquals(Quotes.all.size, Quotes.all.map { it.reading }.toSet().size)
        assertEquals(Quotes.authors.size, Quotes.authors.map { it.id }.toSet().size)
        Quotes.all.forEach {
            assertTrue(it.reading.length in 2..Quotes.MAX_READING_LENGTH)
            assertTrue(it.reading.all { c -> c in 'ぁ'..'ん' || c == 'ー' })
            assertEquals(it.reading, GameEngine.normalizeKana(it.reading))
            assertTrue(it.author.biography.isNotBlank())
            assertTrue(it.explanation.length >= 30)
            assertTrue(it.wording.isNotBlank())
            assertTrue(it.source.isNotBlank())
            assertTrue(it.sourceUrl.startsWith("https://"))
        }
    }

    @Test fun catalogSearchRespectsCategoryAndUnlocksAndNormalizesKana() {
        val quote = Quotes.find("hyakunin-001")!!
        val ids = Quotes.all.map { it.id }.toSet()
        assertTrue(Quotes.search(null, emptySet(), "").isEmpty())
        assertEquals(listOf(quote), Quotes.search(QuoteCategory.HYAKUNIN, setOf(quote.id), "アキノタノ"))
        assertTrue(Quotes.search(QuoteCategory.BUSINESS, ids, quote.reading).isEmpty())
        assertEquals(listOf(quote), Quotes.search(null, setOf(quote.id), quote.author.name))
        assertEquals(listOf(quote), Quotes.search(null, setOf(quote.id), quote.explanation.take(8)))
        assertTrue(Quotes.search(null, ids, "該当しない検索文字列").isEmpty())
        assertEquals(Quotes.all, Quotes.search(null, ids))
    }

    @Test fun legacyQuoteIdsAndReadingsRemainCompatible() {
        val legacy = mapOf(
            "descartes-think" to "われおもうゆえにわれあり",
            "descartes-reason" to "りょうしきはもっともこうへいにわけられている",
            "pascal-reed" to "にんげんはかんがえるあしである",
            "pascal-heart" to "こころにはりせいのしらないりゆうがある",
            "confucius-learning" to "まなびてときにこれをならう",
            "confucius-reciprocity" to "おのれのほっせざるところひとにほどこすことなかれ",
            "marcus-good" to "よいひとをろんじるなよいひとになれ",
            "epictetus-judgment" to "ひとをまどわすのはものごとへのはんだんだ",
        )
        legacy.forEach { (id, reading) ->
            val kept = Quotes.find(id)
            if (kept != null) {
                assertEquals(reading, kept.reading)
                assertEquals(QuoteCategory.HISTORY, kept.category)
            } else assertTrue(Quotes.isRetired(id, reading))
        }
    }

    @Test fun everyPlayableQuoteHasAWikipediaRevisionAndReuseAttribution() {
        Quotes.all.forEach { quote ->
            val uri = java.net.URI(quote.sourceUrl)
            assertEquals("https", uri.scheme)
            assertEquals("ja.wikipedia.org", uri.host)
            assertTrue(uri.query.contains("oldid="))
            assertTrue(quote.source.contains("Wikipedia"))
            assertTrue(quote.license.contains("Wikipedia"))
            assertTrue(quote.wording.contains("Wikipedia"))
            assertFalse(Quotes.isRetired(quote.id, quote.reading))
        }
    }

    @Test fun categoryAndWorkSearchKeepUncollectedQuotesHidden() {
        QuoteCategory.entries.forEach { category ->
            val quote = Quotes.inCategory(category).first()
            val collected = setOf(quote.id)
            assertEquals(listOf(quote), Quotes.search(category, collected, quote.author.name))
            assertEquals(listOf(quote), Quotes.search(category, collected, quote.reading))
            assertTrue(Quotes.search(category, emptySet(), quote.author.name).isEmpty())
            if (category.showsWorkTitle) {
                assertEquals(listOf(quote), Quotes.search(category, collected, quote.author.period))
            }
        }
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

    @Test fun everyQuoteUsesReadingLengthAndUnknownIdentityGetsOrdinaryPoints() {
        Quotes.all.forEach { quote ->
            val meteor = Meteor(1, quote.reading, 0, .2, .05, quoteId = quote.id)
            assertEquals(quote.reading.codePointCount(0, quote.reading.length) * 300, meteor.points)
        }
        assertEquals(40, Meteor(1, "ちきゅう", 0, .2, .05, quoteId = "missing").points)
        assertEquals(20, Meteor(1, "ほし", 0, .2, .05, quoteId = Quotes.all.first().id).points)
        assertEquals(10, Scoring.word("が"))
        assertEquals(40, Scoring.word("ちきゅう"))
        assertEquals(3000, Scoring.quote("あ".repeat(10)))
    }

    @Test fun savedScoresKeepTheirValueAndOnlyNewHitsUseTheNewRate() {
        val before = withMeteor("ほし").snapshot().copy(score = 123L)
        val game = GameEngine().apply { restore(before); resume() }
        assertEquals(123L, game.score)
        game.fire("ほし")
        advance(game, .6)
        assertEquals(143L, game.score)
    }
}
