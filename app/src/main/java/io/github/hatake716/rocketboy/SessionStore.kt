package io.github.hatake716.rocketboy

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("rocketboy", Context.MODE_PRIVATE)
    val bestScore get() = prefs.getLong("bestScore", 0)
    val collectedQuoteIds get() = prefs.getStringSet("collectedQuotes", emptySet()).orEmpty()
        .filter { Quotes.find(it) != null }.toSet()
    var musicEnabled: Boolean
        get() = prefs.getBoolean("music", true)
        set(value) { prefs.edit().putBoolean("music", value).apply() }
    var effectsEnabled: Boolean
        get() = prefs.getBoolean("effects", true)
        set(value) { prefs.edit().putBoolean("effects", value).apply() }
    var playOptions: PlayOptions
        get() {
            val mode = runCatching { GameMode.valueOf(prefs.getString("mode", "NORMAL")!!) }.getOrDefault(GameMode.NORMAL)
            return PlayOptions(mode, if (mode == GameMode.COMPETITIVE) 1 else
                prefs.getInt("startLevel", 1).coerceIn(1, PlayOptions.MAX_START_LEVEL))
        }
        set(value) { prefs.edit().putString("mode", value.mode.name).putInt("startLevel", value.startLevel).apply() }

    val topScores: List<ScoreEntry> get() {
        val stored = prefs.getString("topScores", null)
        if (stored == null) {
            val saved = load()
            if (bestScore <= 0) return emptyList()
            // Old versions kept one best number, not ten historical runs. Preserve it honestly.
            return listOf(if (saved != null && saved.score == bestScore) scoreEntry(saved, false, 0L)
                else ScoreEntry(ScoreEntry.LEGACY_ID, bestScore, 0L, finished = true))
        }
        return runCatching {
            val array = JSONArray(stored)
            (0 until array.length()).mapNotNull { index -> runCatching {
                val row = array.getJSONObject(index)
                ScoreEntry(row.getString("id"), row.getLong("score"), row.getLong("playedAt"),
                    if (row.isNull("mode")) null else GameMode.valueOf(row.getString("mode")),
                    if (row.isNull("startLevel")) null else row.getInt("startLevel"),
                    if (row.isNull("wave")) null else row.getInt("wave"),
                    if (row.isNull("seconds")) null else row.getInt("seconds"), row.optBoolean("finished"))
                    .also { check(it.score > 0 && it.missionId.isNotBlank()) }
            }.getOrNull() }.fold(emptyList<ScoreEntry>()) { all, entry -> ScoreHistory.record(all, entry) }
        }.getOrDefault(if (bestScore > 0) listOf(ScoreEntry(ScoreEntry.LEGACY_ID, bestScore, 0L)) else emptyList())
    }

    private fun scoreEntry(m: Mission, finished: Boolean, now: Long) = ScoreEntry(m.id, m.score, now,
        m.options.mode, m.options.startLevel, m.options.startLevel + (m.elapsed / 25).toInt(), m.elapsed.toInt(), finished)

    private fun encodeScores(entries: List<ScoreEntry>) = JSONArray().apply {
        entries.forEach { e -> put(JSONObject().apply {
            put("id", e.missionId); put("score", e.score); put("playedAt", e.playedAt)
            put("mode", e.mode?.name ?: JSONObject.NULL); put("startLevel", e.startLevel ?: JSONObject.NULL)
            put("wave", e.wave ?: JSONObject.NULL); put("seconds", e.elapsedSeconds ?: JSONObject.NULL)
            put("finished", e.finished)
        }) }
    }.toString()

    fun save(engine: GameEngine, finished: Boolean = engine.phase == Phase.GAME_OVER) {
        val ranking = ScoreHistory.record(topScores, scoreEntry(engine.snapshot(), finished, System.currentTimeMillis()))
        val edit = prefs.edit().putLong("bestScore", maxOf(bestScore, engine.score))
            .putStringSet("collectedQuotes", collectedQuoteIds + engine.collectedQuotes)
            .putString("topScores", encodeScores(ranking))
        if (engine.phase == Phase.GAME_OVER) edit.remove("mission")
        else edit.putString("mission", encode(engine.snapshot()).toString())
        edit.apply()
    }

    fun clearMission() { prefs.edit().remove("mission").apply() }

    fun load(): Mission? = runCatching {
        val root = JSONObject(prefs.getString("mission", null) ?: return null)
        check(root.getInt("version") in 1..3)
        val options = PlayOptions(GameMode.valueOf(root.optString("mode", "NORMAL")), root.optInt("startLevel", 1))
        val array = root.getJSONArray("meteors")
        check(array.length() <= 6)
        val meteors = (0 until array.length()).mapNotNull { i ->
            val m = array.getJSONObject(i)
            Meteor(m.getInt("id"), m.getString("word"), m.getInt("lane"), m.getDouble("y"),
                m.getDouble("speed"), m.getBoolean("targeted"), m.getDouble("rocket"),
                if (m.isNull("quoteId")) null else m.getString("quoteId")).also {
                check(it.word.isNotBlank() && it.word.length <= Quotes.MAX_READING_LENGTH && it.lane in 0..1)
                check(if (it.quoteId == null) it.word.length <= 16 else it.quote != null || Quotes.isRetired(it.quoteId, it.word))
                check(it.y in 0.0..1.0 && it.speed in 0.0..1.0 && it.rocket in 0.0..1.0)
            }.takeUnless { Quotes.isRetired(it.quoteId, it.word) }
        }
        Mission(root.getString("id"), root.getDouble("elapsed"), root.getLong("score"),
            root.getInt("damage"), root.getInt("destroyed"), root.getInt("combo"),
            root.getInt("bestCombo"), root.getDouble("spawnIn"), root.getLong("random"),
            root.getInt("nextId"), meteors,
            root.optJSONArray("collectedQuotes").let { a ->
                if (a == null) emptySet() else (0 until a.length()).map { a.getString(it) }
                    .filter { Quotes.find(it) != null }.toSet()
            }, root.optDouble("nextQuoteAt", maxOf(Quotes.FIRST_APPEARANCE, root.getDouble("elapsed") + 5.0)), options).also {
            check(it.damage in 0 until options.mode.maxLives && it.score >= 0 && it.elapsed >= 0 && it.elapsed.isFinite())
            check(it.nextQuoteAt.isFinite() && it.nextQuoteAt >= 0)
            check(options.mode != GameMode.QUOTES || meteors.all { m -> m.quote != null })
        }
    }.getOrNull()

    private fun encode(m: Mission) = JSONObject().apply {
        put("version", 3); put("mode", m.options.mode.name); put("startLevel", m.options.startLevel)
        put("id", m.id); put("elapsed", m.elapsed); put("score", m.score)
        put("collectedQuotes", JSONArray(m.collectedQuotes.toList())); put("nextQuoteAt", m.nextQuoteAt)
        put("damage", m.damage); put("destroyed", m.destroyed); put("combo", m.combo)
        put("bestCombo", m.bestCombo); put("spawnIn", m.spawnIn); put("random", m.randomState)
        put("nextId", m.nextId)
        put("meteors", JSONArray().apply { m.meteors.forEach { meteor ->
            put(JSONObject().apply {
                put("id", meteor.id); put("word", meteor.word); put("lane", meteor.lane)
                put("y", meteor.y); put("speed", meteor.speed)
                put("targeted", meteor.targeted); put("rocket", meteor.rocket)
                put("quoteId", meteor.quoteId)
            })
        } })
    }
}
