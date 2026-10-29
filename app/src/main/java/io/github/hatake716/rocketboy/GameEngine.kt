package io.github.hatake716.rocketboy

import java.text.Normalizer
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

enum class Phase { READY, RUNNING, PAUSED, GAME_OVER }
enum class GameMode(val title: String, val maxLives: Int = 3, val fallSpeedMultiplier: Double = 1.0) {
    NORMAL("通常モード"), QUOTES("名言モード"), COMPETITIVE("競技モード", 1, 1.5)
}

data class PlayOptions(val mode: GameMode = GameMode.NORMAL, val startLevel: Int = 1) {
    init {
        require(startLevel in 1..MAX_START_LEVEL)
        require(mode != GameMode.COMPETITIVE || startLevel == 1) { "Competitive missions always start at WAVE 1" }
    }
    companion object { const val MAX_START_LEVEL = 16 }
}

data class Meteor(
    val id: Int, val word: String, val lane: Int,
    var y: Double, val speed: Double,
    var targeted: Boolean = false, var rocket: Double = 0.0,
    val quoteId: String? = null,
) {
    val quote get() = Quotes.find(quoteId)?.takeIf { it.reading == word }
    val points get() = quote?.points ?: Scoring.word(word)
}

sealed interface GameEvent {
    data class Intercept(val word: String, val lane: Int, val y: Double,
                         val points: Int = Scoring.word(word), val quoteId: String? = null) : GameEvent
    data class Impact(val lane: Int, val quoteId: String? = null) : GameEvent
    data class Wave(val number: Int) : GameEvent
}

data class Mission(
    val id: String, val elapsed: Double, val score: Long, val damage: Int,
    val destroyed: Int, val combo: Int, val bestCombo: Int,
    val spawnIn: Double, val randomState: Long, val nextId: Int,
    val meteors: List<Meteor>,
    val collectedQuotes: Set<String> = emptySet(), val nextQuoteAt: Double = Quotes.FIRST_APPEARANCE,
    val options: PlayOptions = PlayOptions(),
)

/** Pure game rules. Distances are normalized, so showing an IME never changes flight time. */
class GameEngine(seed: Long = System.nanoTime(), private val knownQuotes: Set<String> = emptySet(),
                 options: PlayOptions = PlayOptions()) {
    var options = options
        private set
    var phase = Phase.READY
        private set
    var elapsed = 0.0
        private set
    var score = 0L
        private set
    var damage = 0
        private set
    var destroyed = 0
        private set
    var combo = 0
        private set
    var bestCombo = 0
        private set
    val maxLives get() = options.mode.maxLives
    val lives get() = (maxLives - damage).coerceAtLeast(0)
    val wave get() = (elapsed / 25).toInt() + options.startLevel
    val difficulty get() = difficultyAt(elapsed + (options.startLevel - 1) * 25.0).let {
        it.copy(fallSpeed = it.fallSpeed * options.mode.fallSpeedMultiplier)
    }
    val meteors = mutableListOf<Meteor>()
    /** Renderer supplies a normalized clearance so kana never stack over an unreadable entry. */
    var minimumLaneGap = 0.30
    var missionId = UUID.randomUUID().toString()
        private set
    private var spawnIn = 0.7
    private var randomState = seed
    private var nextId = 1
    var collectedQuotes: Set<String> = emptySet()
        private set
    private var nextQuoteAt = Quotes.FIRST_APPEARANCE

    fun resume() { if (phase != Phase.GAME_OVER) phase = Phase.RUNNING }
    fun pause() { if (phase == Phase.RUNNING) phase = Phase.PAUSED }
    fun pointsFor(meteor: Meteor) = Scoring.earned(meteor.points, options.startLevel)

    /** IME composing text is only used for highlighting; the caller submits committed text. */
    fun fire(input: String): Meteor? {
        if (phase != Phase.RUNNING) return null
        val word = normalizeKana(input)
        return meteors.filter { !it.targeted && it.word == word }.maxByOrNull { it.y }?.also {
            it.targeted = true
            it.rocket = 0.0
        }
    }

    fun tick(seconds: Double): List<GameEvent> {
        if (phase != Phase.RUNNING || !seconds.isFinite() || seconds <= 0) return emptyList()
        // Do not punish players for a stalled frame, screen transition, or a system interruption.
        val dt = min(seconds, 0.1)
        val previousWave = wave
        elapsed += dt
        val events = mutableListOf<GameEvent>()
        if (wave != previousWave) events += GameEvent.Wave(wave)
        val iterator = meteors.iterator()
        while (iterator.hasNext()) {
            val meteor = iterator.next()
            if (meteor.targeted) {
                meteor.rocket += dt / (0.18 + 0.28 * (1 - meteor.y))
                if (meteor.rocket >= 1) {
                    val earned = pointsFor(meteor)
                    score += earned
                    meteor.quote?.let { collectedQuotes = collectedQuotes + it.id }
                    destroyed++
                    combo++
                    bestCombo = max(bestCombo, combo)
                    events += GameEvent.Intercept(meteor.word, meteor.lane, meteor.y, earned, meteor.quote?.id)
                    iterator.remove()
                }
            } else {
                meteor.y += meteor.speed * dt
                if (meteor.y >= 1) {
                    damage = min(maxLives, damage + 1)
                    combo = 0
                    events += GameEvent.Impact(meteor.lane, meteor.quote?.id)
                    iterator.remove()
                    if (damage == maxLives) {
                        phase = Phase.GAME_OVER
                        break
                    }
                }
            }
        }
        if (phase == Phase.GAME_OVER) return events
        spawnIn -= dt
        if (spawnIn <= 0) {
            if (meteors.size < difficulty.maxMeteors && spawn()) {
                spawnIn = if (options.mode == GameMode.QUOTES) 1.2 else difficulty.spawnInterval
            } else spawnIn = 0.2
        }
        return events
    }

    private fun random(bound: Int): Int {
        randomState = randomState * 6364136223846793005L + 1442695040888963407L
        return ((randomState ushr 33) % bound).toInt()
    }

    private fun spawn(): Boolean {
        // Give a long quotation the full field, after existing ordinary meteors have cleared.
        if (meteors.any { it.quote != null }) return false
        if (options.mode == GameMode.QUOTES || elapsed >= nextQuoteAt) {
            if (meteors.isNotEmpty()) return false
            val unseen = Quotes.all.filter { it.id !in knownQuotes && it.id !in collectedQuotes }
            val pool = unseen.ifEmpty { Quotes.all }
            val quote = pool[random(pool.size)]
            val flightTime = (16.0 + quote.reading.length * .30) * (0.0625 / difficulty.fallSpeed)
            meteors += Meteor(nextId++, quote.reading, 0, 0.0, 1.0 / flightTime, quoteId = quote.id)
            nextQuoteAt = elapsed + Quotes.INTERVAL
            return true
        }
        val freeLanes = (0..1).filter { lane -> meteors.none { it.lane == lane && it.y < minimumLaneGap.coerceIn(.30, 1.0) } }
        if (freeLanes.isEmpty()) return false
        val candidates = Words.forWave(wave).filter { word -> meteors.none { it.word == word } }
        if (candidates.isEmpty()) return false
        meteors += Meteor(nextId++, candidates[random(candidates.size)], freeLanes[random(freeLanes.size)],
            0.0, difficulty.fallSpeed)
        return true
    }

    fun snapshot() = Mission(missionId, elapsed, score, damage, destroyed, combo, bestCombo,
        spawnIn, randomState, nextId, meteors.map { it.copy() }, collectedQuotes.toSet(), nextQuoteAt, options)

    fun restore(saved: Mission) {
        missionId = saved.id
        options = saved.options
        elapsed = saved.elapsed
        score = saved.score
        damage = saved.damage
        destroyed = saved.destroyed
        combo = saved.combo
        bestCombo = saved.bestCombo
        spawnIn = saved.spawnIn
        randomState = saved.randomState
        nextId = saved.nextId
        collectedQuotes = saved.collectedQuotes.toSet()
        nextQuoteAt = saved.nextQuoteAt
        meteors.clear()
        meteors.addAll(saved.meteors.map { it.copy() })
        phase = if (damage >= maxLives) Phase.GAME_OVER else Phase.PAUSED
    }

    companion object {
        fun normalizeKana(value: String): String = Normalizer.normalize(value.trim(), Normalizer.Form.NFKC)
            .map { if (it in 'ァ'..'ヶ') (it.code - 0x60).toChar() else it }.joinToString("")

        fun difficultyAt(seconds: Double): Difficulty {
            val level = (seconds / 25).toInt().coerceAtLeast(0)
            return Difficulty(
                spawnInterval = max(1.6, 5.2 - level * 0.32),
                fallSpeed = min(0.092, 0.0625 + level * 0.002),
                maxMeteors = min(6, 2 + level / 2),
            )
        }
    }
}

data class Difficulty(val spawnInterval: Double, val fallSpeed: Double, val maxMeteors: Int)

object Words {
    private fun words(s: String) = s.split(" ").filter { it.isNotBlank() }
    val singles = words("あ い う え お か き く け こ さ し す せ そ た ち つ て と な に ぬ ね の は ひ ふ へ ほ ま み む め も や ゆ よ ら り る れ ろ わ を ん が ぎ ぐ げ ご ざ じ ず ぜ ぞ だ で ど ば び ぶ べ ぼ ぱ ぴ ぷ ぺ ぽ")
    val short = words("そら ほし つき ゆめ にじ うみ かぜ あめ ゆき はな とり ねこ いぬ くも やま かわ もり ひかり みらい いのち きぼう せかい ちきゅう うちゅう たいよう ぎんが きせき かなた こだま つばさ しずく ほのお ひみつ けしき")
    val medium = words("あおぞら ほしぞら ほしくず ながれぼし みかづき まんげつ たんけん ぼうけん ともだち ひまわり たんぽぽ あさやけ ゆうやけ ほしあかり りゅうせい すいせい わくせい いんせき えいせい じゅうりょく ひこうせん きらめき かがやき たからもの おまもり すいへいせん はくちょう しろうさぎ なつやすみ ほっきょく ふゆぞら しんりん おんがく こうせん かいおうせい めいおうせい")
    val long = words("りゅうせいぐん うちゅうひこうし たいようけい ぎんがてつどう ほしのかけら つきのうさぎ ほしのものがたり まほうのことば うちゅうりょこう なないろのにじ あしたのひかり ひみつのきち ぎんがのはて ぼくらのみらい ちきゅうをまもれ ひかりのつばさ かなたのぎんが かがやくみらい ほしぞらさんぽ きぼうのひかり うちゅうたんけん きらめくほしぞら ぼうけんのはじまり きせきのいちげき さいごのろけっと せかいのはてまで ひゃくまんこうねん たいようのつばさ げつめんちゃくりく うちゅうぼうえいたい")

    fun forWave(wave: Int): List<String> = when {
        wave <= 1 -> short.filter { it.length == 2 }
        wave <= 3 -> short
        wave <= 6 -> medium
        wave <= 9 -> medium.filter { it.length >= 5 } + long.filter { it.length <= 7 }
        else -> long
    }
}
