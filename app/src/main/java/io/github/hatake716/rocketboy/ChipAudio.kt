package io.github.hatake716.rocketboy

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool

/** Locally synthesized, original music. All audio stops on pause and audio-focus loss. */
class ChipAudio(context: Context, private val store: SessionStore, private val onFocusLost: () -> Unit) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    private val music = MediaPlayer.create(context, R.raw.orbit_runner).apply {
        isLooping = true
        setVolume(0.40f, 0.40f)
    }
    private val sounds = SoundPool.Builder().setMaxStreams(5).setAudioAttributes(attributes).build()
    private val loaded = mutableSetOf<Int>()
    private val shoot: Int
    private val hit: Int
    private val impact: Int
    private val over: Int
    private val rank: Int
    private var hasFocus = false
    private var active = false
    private var released = false
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(attributes)
        .setOnAudioFocusChangeListener { change ->
            if (change <= AudioManager.AUDIOFOCUS_LOSS) {
                hasFocus = false
                pause()
                onFocusLost()
            }
        }.build()

    init {
        sounds.setOnLoadCompleteListener { _, id, status -> if (status == 0) loaded += id }
        shoot = sounds.load(context, R.raw.launch, 1)
        hit = sounds.load(context, R.raw.intercept, 1)
        impact = sounds.load(context, R.raw.impact, 1)
        over = sounds.load(context, R.raw.game_over, 1)
        rank = sounds.load(context, R.raw.rank_up, 1)
    }

    fun start() {
        if (released) return
        active = true
        if (store.musicEnabled || store.effectsEnabled) {
            hasFocus = audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
        if (hasFocus && store.musicEnabled) music.start()
    }

    fun settingsChanged() {
        if (!store.musicEnabled && music.isPlaying) music.pause()
        if (active) start()
    }

    fun pause() {
        if (released) return
        active = false
        if (music.isPlaying) music.pause()
        sounds.autoPause()
        if (hasFocus) audioManager.abandonAudioFocusRequest(focusRequest)
        hasFocus = false
    }

    private fun play(id: Int, volume: Float = 0.65f) {
        if (!released && active && hasFocus && store.effectsEnabled && id in loaded) {
            sounds.play(id, volume, volume, 1, 0, 1f)
        }
    }
    fun launch() = play(shoot, 0.4f)
    fun intercept() = play(hit)
    fun impact() = play(impact)
    fun gameOver() {
        if (music.isPlaying) music.pause()
        play(over)
    }
    fun rankUp() = play(rank)
    fun release() {
        pause()
        released = true
        music.release()
        sounds.release()
    }
}
