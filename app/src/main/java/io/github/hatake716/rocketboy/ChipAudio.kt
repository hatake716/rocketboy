package io.github.hatake716.rocketboy

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool

/** Original music; only one music player is kept, and preparation never blocks a game frame. */
class ChipAudio(context: Context, private val store: SessionStore, private val onFocusLost: () -> Unit) {
    private val appContext = context.applicationContext
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    private var music: MediaPlayer? = null
    private var prepared = false
    internal var currentTrack: MusicTrack = CelestialMusic.cadet
        private set
    internal val isMusicPlaying get() = prepared && music?.isPlaying == true
    internal val musicPosition get() = if (prepared) music?.currentPosition ?: 0 else 0
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
    private var audition = false
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

    /** Resolves directly to the final rank when one quote crosses several thresholds. */
    fun selectScore(score: Long) {
        selectTrack(CelestialMusic.forScore(score))
    }

    private fun selectTrack(next: MusicTrack) {
        if (released) return
        if (next == currentTrack) return
        releaseMusic()
        currentTrack = next
        updatePlayback()
    }

    fun start(score: Long) {
        if (released) return
        audition = false
        selectScore(score)
        active = true
        if (!hasFocus && (store.musicEnabled || store.effectsEnabled)) {
            hasFocus = audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
        updatePlayback()
    }

    /** An explicit listening-room action works independently of in-game BGM mute. */
    fun preview(track: MusicTrack) {
        if (released) return
        audition = true
        selectTrack(track)
        active = true
        if (!hasFocus) hasFocus = audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        updatePlayback()
    }

    private fun updatePlayback() {
        if (released || !active || !hasFocus || (!audition && !store.musicEnabled)) return
        if (music == null) {
            val player = MediaPlayer()
            music = player
            player.setAudioAttributes(attributes)
            player.isLooping = true
            player.setVolume(0.40f, 0.40f)
            player.setOnPreparedListener {
                if (music === it && !released) {
                    prepared = true
                    updatePlayback()
                }
            }
            player.setOnErrorListener { failed, _, _ ->
                if (music === failed) releaseMusic()
                true
            }
            try {
                appContext.resources.openRawResourceFd(currentTrack.resource).use {
                    player.setDataSource(it.fileDescriptor, it.startOffset, it.length)
                }
                player.prepareAsync()
            } catch (_: java.io.IOException) {
                releaseMusic()
            }
        } else if (prepared && !isMusicPlaying) music?.start()
    }

    fun settingsChanged() {
        if (released) return
        if (audition) { updatePlayback(); return }
        if (!store.musicEnabled && isMusicPlaying) music?.pause()
        if (!store.effectsEnabled) sounds.autoPause()
        if (!store.musicEnabled && !store.effectsEnabled) {
            if (hasFocus) audioManager.abandonAudioFocusRequest(focusRequest)
            hasFocus = false
        } else if (active) start(currentTrack.threshold)
    }

    fun pause() {
        if (released) return
        active = false
        if (isMusicPlaying) music?.pause()
        sounds.autoPause()
        if (hasFocus) audioManager.abandonAudioFocusRequest(focusRequest)
        hasFocus = false
    }

    private fun play(id: Int, volume: Float = 0.65f) {
        if (!released && !audition && active && hasFocus && store.effectsEnabled && id in loaded) {
            sounds.play(id, volume, volume, 1, 0, 1f)
        }
    }
    fun launch() = play(shoot, 0.4f)
    fun intercept() = play(hit)
    fun impact() = play(impact)
    fun gameOver() {
        if (isMusicPlaying) music?.pause()
        play(over)
        active = false // Also prevents a still-preparing track from starting on the result screen.
    }
    fun rankUp() = play(rank)
    private fun releaseMusic() {
        prepared = false
        music?.release()
        music = null
    }
    fun release() {
        if (released) return
        pause()
        released = true
        releaseMusic()
        sounds.release()
    }
}
