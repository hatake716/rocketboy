package io.github.hatake716.rocketboy

import java.text.Normalizer
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

enum class Phase { READY, RUNNING, PAUSED, GAME_OVER }

data class Meteor(
    val id: Int, val word: String, val lane: Int,
    var y: Double, val speed: Double,
    var targeted: Boolean = false, var rocket: Double = 0.0,
)

sealed interface GameEvent {
    data class Intercept(val word: String, val lane: Int, val y: Double) : GameEvent
    data class Impact(val lane: Int) : GameEvent
    data class Wave(val number: Int) : GameEvent
}

data class Mission(
    val id: String, val elapsed: Double, val score: Long, val damage: Int,
    val destroyed: Int, val combo: Int, val bestCombo: Int,
    val spawnIn: Double, val randomState: Long, val nextId: Int,
    val meteors: List<Meteor>,
)

/** Pure game rules. Distances are normalized, so showing an IME never changes flight time. */
class GameEngine(seed: Long = System.nanoTime()) {
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
    val lives get() = (3 - damage).coerceAtLeast(0)
    val wave get() = (elapsed / 25).toInt() + 1
    val difficulty get() = difficultyAt(elapsed)
    val meteors = mutableListOf<Meteor>()
    /** Renderer supplies a normalized clearance so kana never stack over an unreadable entry. */
    var minimumLaneGap = 0.30
    var missionId = UUID.randomUUID().toString()
        private set
    private var spawnIn = 0.7
    private var randomState = seed
    private var nextId = 1

    fun resume() { if (phase != Phase.GAME_OVER) phase = Phase.RUNNING }
    fun pause() { if (phase == Phase.RUNNING) phase = Phase.PAUSED }

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
                    score += meteor.word.codePointCount(0, meteor.word.length)
                    destroyed++
                    combo++
                    bestCombo = max(bestCombo, combo)
                    events += GameEvent.Intercept(meteor.word, meteor.lane, meteor.y)
                    iterator.remove()
                }
            } else {
                meteor.y += meteor.speed * dt
                if (meteor.y >= 1) {
                    damage = min(3, damage + 1)
                    combo = 0
                    events += GameEvent.Impact(meteor.lane)
                    iterator.remove()
                    if (damage == 3) {
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
                spawnIn = difficulty.spawnInterval
            } else spawnIn = 0.2
        }
        return events
    }

    private fun random(bound: Int): Int {
        randomState = randomState * 6364136223846793005L + 1442695040888963407L
        return ((randomState ushr 33) % bound).toInt()
    }

    private fun spawn(): Boolean {
        val freeLanes = (0..1).filter { lane -> meteors.none { it.lane == lane && it.y < minimumLaneGap.coerceIn(.30, 1.0) } }
        if (freeLanes.isEmpty()) return false
        val candidates = Words.forWave(wave).filter { word -> meteors.none { it.word == word } }
        if (candidates.isEmpty()) return false
        meteors += Meteor(nextId++, candidates[random(candidates.size)], freeLanes[random(freeLanes.size)],
            0.0, difficulty.fallSpeed)
        return true
    }

    fun snapshot() = Mission(missionId, elapsed, score, damage, destroyed, combo, bestCombo,
        spawnIn, randomState, nextId, meteors.map { it.copy() })

    fun restore(saved: Mission) {
        missionId = saved.id
        elapsed = saved.elapsed
        score = saved.score
        damage = saved.damage
        destroyed = saved.destroyed
        combo = saved.combo
        bestCombo = saved.bestCombo
        spawnIn = saved.spawnIn
        randomState = saved.randomState
        nextId = saved.nextId
        meteors.clear()
        meteors.addAll(saved.meteors.map { it.copy() })
        phase = if (damage >= 3) Phase.GAME_OVER else Phase.PAUSED
    }

    companion object {
        fun normalizeKana(value: String): String = Normalizer.normalize(value.trim(), Normalizer.Form.NFKC)
            .map { if (it in 'ァ'..'ヶ') (it.code - 0x60).toChar() else it }.joinToString("")

        fun difficultyAt(seconds: Double): Difficulty {
            val level = (seconds / 25).toInt().coerceAtLeast(0)
            return Difficulty(
                spawnInterval = max(1.6, 5.2 - level * 0.32),
                fallSpeed = min(0.092, 0.037 + level * 0.003),
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
        wave == 1 -> singles
        wave <= 3 -> short
        wave <= 6 -> medium
        wave <= 9 -> medium.filter { it.length >= 5 } + long.filter { it.length <= 7 }
        else -> long
    }
}
