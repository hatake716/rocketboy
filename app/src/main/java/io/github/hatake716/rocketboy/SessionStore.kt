package io.github.hatake716.rocketboy

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("rocketboy", Context.MODE_PRIVATE)
    val bestScore get() = prefs.getLong("bestScore", 0)
    var musicEnabled: Boolean
        get() = prefs.getBoolean("music", true)
        set(value) { prefs.edit().putBoolean("music", value).apply() }
    var effectsEnabled: Boolean
        get() = prefs.getBoolean("effects", true)
        set(value) { prefs.edit().putBoolean("effects", value).apply() }

    fun save(engine: GameEngine) {
        val edit = prefs.edit().putLong("bestScore", maxOf(bestScore, engine.score))
        if (engine.phase == Phase.GAME_OVER) edit.remove("mission")
        else edit.putString("mission", encode(engine.snapshot()).toString())
        edit.apply()
    }

    fun clearMission() { prefs.edit().remove("mission").apply() }

    fun load(): Mission? = runCatching {
        val root = JSONObject(prefs.getString("mission", null) ?: return null)
        check(root.getInt("version") == 1)
        val array = root.getJSONArray("meteors")
        check(array.length() <= 6)
        val meteors = (0 until array.length()).map { i ->
            val m = array.getJSONObject(i)
            Meteor(m.getInt("id"), m.getString("word"), m.getInt("lane"), m.getDouble("y"),
                m.getDouble("speed"), m.getBoolean("targeted"), m.getDouble("rocket")).also {
                check(it.word.isNotBlank() && it.word.length <= 16 && it.lane in 0..1)
                check(it.y in 0.0..1.0 && it.speed in 0.0..1.0 && it.rocket in 0.0..1.0)
            }
        }
        Mission(root.getString("id"), root.getDouble("elapsed"), root.getLong("score"),
            root.getInt("damage"), root.getInt("destroyed"), root.getInt("combo"),
            root.getInt("bestCombo"), root.getDouble("spawnIn"), root.getLong("random"),
            root.getInt("nextId"), meteors).also {
            check(it.damage in 0..2 && it.score >= 0 && it.elapsed >= 0 && it.elapsed.isFinite())
        }
    }.getOrNull()

    private fun encode(m: Mission) = JSONObject().apply {
        put("version", 1); put("id", m.id); put("elapsed", m.elapsed); put("score", m.score)
        put("damage", m.damage); put("destroyed", m.destroyed); put("combo", m.combo)
        put("bestCombo", m.bestCombo); put("spawnIn", m.spawnIn); put("random", m.randomState)
        put("nextId", m.nextId)
        put("meteors", JSONArray().apply { m.meteors.forEach { meteor ->
            put(JSONObject().apply {
                put("id", meteor.id); put("word", meteor.word); put("lane", meteor.lane)
                put("y", meteor.y); put("speed", meteor.speed)
                put("targeted", meteor.targeted); put("rocket", meteor.rocket)
            })
        } })
    }
}
