package io.github.hatake716.rocketboy

import androidx.annotation.RawRes

data class MusicTrack(val id: String, val title: String, val threshold: Long, val bpm: Int,
                      @param:RawRes val resource: Int)

/** One original stage theme for each celestial rank; cadet retains the opening theme. */
object CelestialMusic {
    val cadet = MusicTrack("cadet", "ORBIT RUNNER", 0L, 168, R.raw.orbit_runner)
    val all = listOf(
        MusicTrack("mercury", "太陽風のスタートライン", 4000L, 188, R.raw.bgm_mercury),
        MusicTrack("venus", "雲海の向こうの金色", 8000L, 164, R.raw.bgm_venus),
        MusicTrack("earth", "青い星からの出航", 12000L, 172, R.raw.bgm_earth),
        MusicTrack("moon", "月面バウンド・エクスプレス", 16000L, 156, R.raw.bgm_moon),
        MusicTrack("mars", "赤い砂のフロンティア", 20000L, 180, R.raw.bgm_mars),
        MusicTrack("phobos", "影を追い越す軌道", 24000L, 192, R.raw.bgm_phobos),
        MusicTrack("deimos", "小さな衛星の大旅行", 28000L, 168, R.raw.bgm_deimos),
        MusicTrack("ceres", "小惑星帯の道しるべ", 32000L, 160, R.raw.bgm_ceres),
        MusicTrack("jupiter", "大赤斑を越えて", 36000L, 178, R.raw.bgm_jupiter),
        MusicTrack("io", "火山列島ブースター", 40000L, 194, R.raw.bgm_io),
        MusicTrack("europa", "氷の扉と蒼い海", 44000L, 158, R.raw.bgm_europa),
        MusicTrack("ganymede", "磁力のコンパス", 48000L, 174, R.raw.bgm_ganymede),
        MusicTrack("callisto", "クレーターの星図", 52000L, 152, R.raw.bgm_callisto),
        MusicTrack("saturn", "環をくぐる航路", 56000L, 176, R.raw.bgm_saturn),
        MusicTrack("enceladus", "白い噴流に乗って", 60000L, 186, R.raw.bgm_enceladus),
        MusicTrack("rhea", "霜原のスキップ", 64000L, 162, R.raw.bgm_rhea),
        MusicTrack("titan", "琥珀の空の探検船", 68000L, 166, R.raw.bgm_titan),
        MusicTrack("iapetus", "白と黒の境界線", 72000L, 172, R.raw.bgm_iapetus),
        MusicTrack("uranus", "横倒しの星の遊覧船", 76000L, 168, R.raw.bgm_uranus),
        MusicTrack("ariel", "氷の峡谷スラローム", 80000L, 184, R.raw.bgm_ariel),
        MusicTrack("titania", "氷壁を越える王冠", 84000L, 174, R.raw.bgm_titania),
        MusicTrack("oberon", "遠い夜の探査灯", 88000L, 158, R.raw.bgm_oberon),
        MusicTrack("neptune", "群青ストームライダー", 92000L, 190, R.raw.bgm_neptune),
        MusicTrack("triton", "逆行軌道の冒険者", 96000L, 182, R.raw.bgm_triton),
        MusicTrack("pluto", "さいはてから、次の宇宙へ", 100000L, 170, R.raw.bgm_pluto),
    )
    fun forScore(score: Long): MusicTrack = all.lastOrNull { score >= it.threshold } ?: cadet
}
