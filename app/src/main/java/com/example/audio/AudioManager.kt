package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

class AudioManager {
    private val sampleRate = 22050
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sfxExecutor = Executors.newFixedThreadPool(4)

    @Volatile
    var soundEnabled: Boolean = true
        private set

    @Volatile
    private var musicPlaying: Boolean = false

    private var musicTrack: AudioTrack? = null
    private var musicLoopPcm: ShortArray? = null

    private var swipePcm: ShortArray? = null
    private var jumpPcm: ShortArray? = null
    private var rollPcm: ShortArray? = null
    private var footstepPcm: ShortArray? = null
    private var powerUpPcm: ShortArray? = null
    private var trainHornPcm: ShortArray? = null
    private var stumblePcm: ShortArray? = null
    private var hoverboardOnPcm: ShortArray? = null
    private var hoverboardBoomPcm: ShortArray? = null
    private var crashPcm: ShortArray? = null
    private var fanfarePcm: ShortArray? = null
    private val coinStepsPcm = arrayOfNulls<ShortArray>(10)

    init {
        scope.launch {
            synthesizeAll()
        }
    }

    fun setEnabled(enabled: Boolean) {
        soundEnabled = enabled
        if (!enabled) {
            pauseMusic()
        }
    }

    fun startMusic() {
        if (!soundEnabled || musicPlaying) return
        val pcm = musicLoopPcm ?: return
        musicPlaying = true
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(pcm, 0, pcm.size)
            track.setLoopPoints(0, pcm.size, -1)
            track.setVolume(0.28f)
            track.play()
            musicTrack = track
        } catch (_: Throwable) {
            musicPlaying = false
        }
    }

    fun pauseMusic() {
        musicPlaying = false
        try {
            musicTrack?.pause()
            musicTrack?.flush()
            musicTrack?.release()
        } catch (_: Throwable) {
        } finally {
            musicTrack = null
        }
    }

    fun playSwipe() = playBuffer(swipePcm, 0.35f)
    fun playJump() = playBuffer(jumpPcm, 0.45f)
    fun playRoll() = playBuffer(rollPcm, 0.40f)
    fun playFootstep() = playBuffer(footstepPcm, 0.14f)
    fun playPowerUp() = playBuffer(powerUpPcm, 0.55f)
    fun playTrainHorn() = playBuffer(trainHornPcm, 0.55f)
    fun playStumble() = playBuffer(stumblePcm, 0.60f)
    fun playHoverboardActivate() = playBuffer(hoverboardOnPcm, 0.50f)
    fun playHoverboardCrash() = playBuffer(hoverboardBoomPcm, 0.70f)
    fun playCrash() = playBuffer(crashPcm, 0.65f)
    fun playMissionComplete() = playBuffer(fanfarePcm, 0.55f)

    fun playCoin(streak: Int) {
        val idx = streak.coerceIn(0, coinStepsPcm.lastIndex)
        playBuffer(coinStepsPcm[idx], 0.42f)
    }

    private fun playBuffer(pcm: ShortArray?, volume: Float) {
        if (!soundEnabled || pcm == null) return
        sfxExecutor.execute {
            try {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(pcm, 0, pcm.size)
                track.setVolume(volume)
                track.play()
                val durationMs = (pcm.size * 1000L / sampleRate) + 40L
                Thread.sleep(durationMs)
                track.stop()
                track.release()
            } catch (_: Throwable) {
            }
        }
    }

    fun release() {
        pauseMusic()
        scope.cancel()
        sfxExecutor.shutdownNow()
    }

    private fun synthesizeAll() {
        swipePcm = buildSwipe()
        jumpPcm = buildJump()
        rollPcm = buildRoll()
        footstepPcm = buildFootstep()
        powerUpPcm = buildPowerUp()
        trainHornPcm = buildTrainHorn()
        stumblePcm = buildStumble()
        hoverboardOnPcm = buildHoverboardOn()
        hoverboardBoomPcm = buildExplosion()
        crashPcm = buildCrash()
        fanfarePcm = buildFanfare()
        for (i in coinStepsPcm.indices) {
            coinStepsPcm[i] = buildCoin(i)
        }
        musicLoopPcm = buildHipHopBeatLoop()
    }

    private fun buildCoin(step: Int): ShortArray {
        val dur = 0.14f
        val n = (sampleRate * dur).toInt()
        val out = ShortArray(n)
        val semitone = step.coerceIn(0, 9)
        val baseFreq = 880.0 * 2.0.pow(semitone / 12.0)
        for (i in 0 until n) {
            val t = i.toDouble() / sampleRate
            val freq = if (t < 0.045) baseFreq else baseFreq * 1.498
            val env = exp(-t * 22.0)
            val wave = sin(2.0 * PI * freq * t) * 0.7 + sin(2.0 * PI * freq * 2.0 * t) * 0.3
            out[i] = (wave * env * 22000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun buildSwipe(): ShortArray {
        val n = (sampleRate * 0.09f).toInt()
        val out = ShortArray(n)
        var seed = 1337
        for (i in 0 until n) {
            val t = i.toDouble() / sampleRate
            val env = sin(PI * (i.toDouble() / n))
            seed = (seed * 1103515245 + 12345) and 0x7fffffff
            val noise = ((seed % 2000) / 1000.0) - 1.0
            val sweep = sin(2.0 * PI * (380.0 + t * 2400.0) * t)
            out[i] = ((noise * 0.35 + sweep * 0.65) * env * 14000.0).toInt().toShort()
        }
        return out
    }

    private fun buildJump(): ShortArray {
        val n = (sampleRate * 0.16f).toInt()
        val out = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val p = i.toDouble() / n
            val freq = 260.0 + p * 520.0
            phase += 2.0 * PI * freq / sampleRate
            val env = (1.0 - p) * exp(-p * 2.5)
            out[i] = (sin(phase) * env * 20000.0).toInt().toShort()
        }
        return out
    }

    private fun buildRoll(): ShortArray {
        val n = (sampleRate * 0.15f).toInt()
        val out = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val p = i.toDouble() / n
            val freq = 340.0 - p * 220.0
            phase += 2.0 * PI * freq / sampleRate
            val env = (1.0 - p)
            out[i] = (sin(phase) * env * 18000.0).toInt().toShort()
        }
        return out
    }

    private fun buildFootstep(): ShortArray {
        val n = (sampleRate * 0.045f).toInt()
        val out = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / sampleRate
            val env = exp(-t * 95.0)
            val thud = sin(2.0 * PI * 115.0 * t)
            out[i] = (thud * env * 12000.0).toInt().toShort()
        }
        return out
    }

    private fun buildPowerUp(): ShortArray {
        val n = (sampleRate * 0.36f).toInt()
        val out = ShortArray(n)
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
        for (i in 0 until n) {
            val t = i.toDouble() / sampleRate
            val noteIdx = ((t / 0.08).toInt()).coerceIn(0, notes.lastIndex)
            val freq = notes[noteIdx]
            val env = exp(-((t % 0.08) * 10.0)) * (1.0 - (i.toDouble() / n) * 0.3)
            val wave = sin(2.0 * PI * freq * t) + 0.35 * sin(2.0 * PI * freq * 2.0 * t)
            out[i] = (wave * env * 16000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun buildTrainHorn(): ShortArray {
        val n = (sampleRate * 0.42f).toInt()
        val out = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / sampleRate
            val env = if (t < 0.04) t / 0.04 else if (t > 0.32) (0.42 - t) / 0.10 else 1.0
            val f1 = 233.08 // Bb3
            val f2 = 293.66 // D4
            val w1 = sin(2.0 * PI * f1 * t) + 0.4 * sin(2.0 * PI * f1 * 2.0 * t)
            val w2 = sin(2.0 * PI * f2 * t) + 0.4 * sin(2.0 * PI * f2 * 2.0 * t)
            out[i] = ((w1 + w2) * 0.45 * env * 18000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun buildStumble(): ShortArray {
        val n = (sampleRate * 0.25f).toInt()
        val out = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / sampleRate
            val thud = sin(2.0 * PI * 90.0 * t) * exp(-t * 24.0)
            val whistle = if (t > 0.06) sin(2.0 * PI * 2150.0 * t) * exp(-(t - 0.06) * 16.0) * 0.45 else 0.0
            out[i] = ((thud + whistle) * 20000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun buildHoverboardOn(): ShortArray {
        val n = (sampleRate * 0.26f).toInt()
        val out = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val p = i.toDouble() / n
            val freq = 180.0 + p * 640.0
            phase += 2.0 * PI * freq / sampleRate
            val env = sin(PI * p)
            val wave = sin(phase) + 0.4 * sin(phase * 1.5)
            out[i] = (wave * env * 16000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun buildExplosion(): ShortArray {
        val n = (sampleRate * 0.35f).toInt()
        val out = ShortArray(n)
        var seed = 4242
        for (i in 0 until n) {
            val t = i.toDouble() / sampleRate
            val env = exp(-t * 10.0)
            seed = (seed * 1103515245 + 12345) and 0x7fffffff
            val noise = ((seed % 2000) / 1000.0) - 1.0
            val sub = sin(2.0 * PI * (120.0 * exp(-t * 6.0)) * t)
            out[i] = ((noise * 0.55 + sub * 0.75) * env * 22000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun buildCrash(): ShortArray {
        val n = (sampleRate * 0.30f).toInt()
        val out = ShortArray(n)
        var seed = 9001
        for (i in 0 until n) {
            val t = i.toDouble() / sampleRate
            val env = exp(-t * 14.0)
            seed = (seed * 1103515245 + 12345) and 0x7fffffff
            val noise = ((seed % 2000) / 1000.0) - 1.0
            val bass = sin(2.0 * PI * 70.0 * t)
            out[i] = ((noise * 0.5 + bass * 0.8) * env * 24000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun buildFanfare(): ShortArray {
        val n = (sampleRate * 0.55f).toInt()
        val out = ShortArray(n)
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50, 1318.51)
        for (i in 0 until n) {
            val t = i.toDouble() / sampleRate
            val idx = ((t / 0.10).toInt()).coerceIn(0, notes.lastIndex)
            val freq = notes[idx]
            val env = exp(-((t % 0.10) * 6.0)) * (1.0 - t / 0.65)
            val wave = sin(2.0 * PI * freq * t) + 0.25 * sin(2.0 * PI * freq * 2.0 * t)
            out[i] = (wave * env * 16000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun buildHipHopBeatLoop(): ShortArray {
        // 4 bars @ 126 BPM -> 16 beats -> 7.619 seconds seamless groove
        val bpm = 126.0
        val beatSec = 60.0 / bpm
        val totalBeats = 16
        val totalSec = totalBeats * beatSec
        val totalSamples = (sampleRate * totalSec).toInt()
        val out = ShortArray(totalSamples)
        val sixteenthSec = beatSec / 4.0

        val bassNotes = doubleArrayOf(
            65.41, 0.0, 65.41, 77.78, // C2, -, C2, Eb2
            98.00, 0.0, 87.31, 77.78, // G2, -, F2, Eb2
            58.27, 0.0, 58.27, 65.41, // Bb1, -, Bb1, C2
            77.78, 87.31, 98.00, 0.0  // Eb2, F2, G2, -
        )
        val leadNotes = doubleArrayOf(
            523.25, 0.0, 622.25, 0.0,
            783.99, 0.0, 698.46, 622.25,
            466.16, 0.0, 523.25, 0.0,
            622.25, 698.46, 783.99, 0.0
        )

        var seed = 777
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val step = ((t / sixteenthSec).toInt()) % 64
            val stepT = t % sixteenthSec
            val barStep = step % 16

            var mix = 0.0

            // Punchy Kick on steps 0, 3, 6, 8, 10 (syncopated hip-hop bounce)
            if (barStep == 0 || barStep == 6 || barStep == 8 || (barStep == 14 && (step / 16) % 2 == 1)) {
                val kickFreq = 48.0 + 130.0 * exp(-stepT * 42.0)
                mix += sin(2.0 * PI * kickFreq * stepT) * exp(-stepT * 14.0) * 0.55
            }

            // Crisp Snare on steps 4 and 12
            if (barStep == 4 || barStep == 12) {
                seed = (seed * 1103515245 + 12345) and 0x7fffffff
                val noise = ((seed % 2000) / 1000.0) - 1.0
                val body = sin(2.0 * PI * 195.0 * stepT)
                mix += (noise * 0.65 + body * 0.35) * exp(-stepT * 24.0) * 0.42
            }

            // Closed Hi-Hat on even 16ths
            if (barStep % 2 == 0) {
                seed = (seed * 1103515245 + 12345) and 0x7fffffff
                val hatNoise = ((seed % 2000) / 1000.0) - 1.0
                mix += hatNoise * exp(-stepT * 65.0) * 0.14
            }

            // Funky Bassline (quarter/eighth grid)
            val bassFreq = bassNotes[barStep]
            if (bassFreq > 0.0) {
                val bassEnv = exp(-stepT * 8.0)
                val bassWave = sin(2.0 * PI * bassFreq * t) + 0.35 * sin(2.0 * PI * bassFreq * 2.0 * t)
                mix += bassWave * bassEnv * 0.32
            }

            // Sunlit Synth Pluck Lead
            val leadFreq = leadNotes[(step / 2) % 16]
            if (leadFreq > 0.0 && step % 2 == 0) {
                val leadEnv = exp(-stepT * 11.0)
                val leadWave = sin(2.0 * PI * leadFreq * t) * 0.7 + sin(2.0 * PI * leadFreq * 2.0 * t) * 0.2
                mix += leadWave * leadEnv * 0.18
            }

            out[i] = (mix * 22000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }
}
