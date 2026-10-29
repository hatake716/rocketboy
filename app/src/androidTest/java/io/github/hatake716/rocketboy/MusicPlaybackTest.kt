package io.github.hatake716.rocketboy

import android.media.MediaPlayer
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class MusicPlaybackTest {
    @Test fun all25PackagedTracksDecodePlayAndLoopInAndroidMediaPlayer() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            CelestialMusic.all.forEach { track ->
                val failed = AtomicBoolean(false)
                val seeked = CountDownLatch(1)
                val player = requireNotNull(MediaPlayer.create(context, track.resource)) { track.id }
                try {
                    player.setOnErrorListener { _, _, _ -> failed.set(true); true }
                    player.setOnSeekCompleteListener { seeked.countDown() }
                    player.isLooping = true
                    player.setVolume(0f, 0f)
                    val expectedDuration = 64 * 4 * 60_000 / track.bpm
                    assertTrue("${track.id}: duration ${player.duration} / $expectedDuration",
                        kotlin.math.abs(player.duration - expectedDuration) < 100)
                    player.seekTo(player.duration - 200)
                    assertTrue("${track.id}: seek", seeked.await(5, TimeUnit.SECONDS))
                    player.start()
                    SystemClock.sleep(750)
                    assertTrue("${track.id}: playback", player.isPlaying)
                    assertTrue("${track.id}: loop wrapped", player.currentPosition in 1..2000)
                    assertFalse("${track.id}: decoder error", failed.get())
                    android.util.Log.i("RocketboyMusicTest", "${track.id}: ${player.duration}ms loop OK")
                } finally { player.release() }
            }
        } finally {
            UiDevice.getInstance(instrumentation).pressHome()
            scenario.close()
        }
    }
}
