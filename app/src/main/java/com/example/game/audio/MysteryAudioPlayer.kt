package com.example.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.*
import kotlin.math.sin

object MysteryAudioPlayer {
    private const val TAG = "MysteryAudioPlayer"
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var musicVolume = 0.5f
    private var mediaPlayer: MediaPlayer? = null
    private var context: Context? = null
    private var isMusicEnabled = false

    fun init(appContext: Context) {
        context = appContext.applicationContext
    }

    fun playClick() = playSelection()
    
    fun playSelection() {
        scope.launch {
            try {
                val sampleRate = 22050
                val numSamples = (sampleRate * 0.04).toInt()
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val freq = 600.0 * (1.0 - t * 20.0).coerceAtLeast(0.2)
                    val envelope = sin(2.0 * Math.PI * freq * t) * (1.0 - t / 0.04)
                    buffer[i] = (envelope * 20000.0 * musicVolume).toInt().toShort()
                }
                playBufferStatic(buffer, sampleRate)
            } catch (e: Throwable) {
                Log.e(TAG, "Error playing selection sound", e)
            }
        }
    }

    fun playSuccess() {
        scope.launch {
            try {
                val sampleRate = 22050
                val duration = 0.22
                val numSamples = (sampleRate * duration).toInt()
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val freq = when {
                        t < 0.07 -> 523.25
                        t < 0.14 -> 659.25
                        else -> 783.99
                    }
                    val envelope = sin(2.0 * Math.PI * freq * t) * (1.0 - t / duration)
                    buffer[i] = (envelope * 22000.0 * musicVolume).toInt().toShort()
                }
                playBufferStatic(buffer, sampleRate)
            } catch (e: Throwable) {
                Log.e(TAG, "Error playing success sound", e)
            }
        }
    }

    fun playError() {
        scope.launch {
            try {
                val sampleRate = 22050
                val duration = 0.25
                val numSamples = (sampleRate * duration).toInt()
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val isSilenced = t in 0.10..0.13
                    val wave = if (isSilenced) 0.0 else {
                        sin(2.0 * Math.PI * 120.0 * t) + 0.5 * sin(2.0 * Math.PI * 240.0 * t)
                    }
                    val envelope = wave * (1.0 - t / duration)
                    buffer[i] = (envelope * 24000.0 * musicVolume).toInt().toShort()
                }
                playBufferStatic(buffer, sampleRate)
            } catch (e: Throwable) {
                Log.e(TAG, "Error playing error sound", e)
            }
        }
    }

    fun playWarning() {
        scope.launch {
            try {
                val sampleRate = 22050
                val duration = 0.15
                val numSamples = (sampleRate * duration).toInt()
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val wave = sin(2.0 * Math.PI * 440.0 * t) + 0.3 * sin(2.0 * Math.PI * 880.0 * t)
                    val envelope = wave * (1.0 - t / duration)
                    buffer[i] = (envelope * 20000.0 * musicVolume).toInt().toShort()
                }
                playBufferStatic(buffer, sampleRate)
            } catch (e: Throwable) {
                Log.e(TAG, "Error playing warning sound", e)
            }
        }
    }

    fun playVote() {
        scope.launch {
            try {
                val sampleRate = 22050
                val duration = 0.12
                val numSamples = (sampleRate * duration).toInt()
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val freq = 120.0 * (1.0 - t * 8.0).coerceAtLeast(0.3)
                    val envelope = sin(2.0 * Math.PI * freq * t) * (1.0 - t / duration)
                    buffer[i] = (envelope * 25000.0 * musicVolume).toInt().toShort()
                }
                playBufferStatic(buffer, sampleRate)
            } catch (e: Throwable) {
                Log.e(TAG, "Error playing vote sound", e)
            }
        }
    }

    fun playTransition() {
        scope.launch {
            try {
                val sampleRate = 22050
                val duration = 0.45
                val numSamples = (sampleRate * duration).toInt()
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val freq = 150.0 + 650.0 * (t / duration) * (t / duration)
                    val volEnvelope = if (t < 0.1) t / 0.1 else (1.0 - t / duration)
                    val wave = sin(2.0 * Math.PI * freq * t) * volEnvelope
                    buffer[i] = (wave * 20000.0 * musicVolume).toInt().toShort()
                }
                playBufferStatic(buffer, sampleRate)
            } catch (e: Throwable) {
                Log.e(TAG, "Error playing transition sound", e)
            }
        }
    }

    fun playTension() = playReveal()

    fun playReveal() {
        scope.launch {
            try {
                val sampleRate = 22050
                val duration = 0.8
                val numSamples = (sampleRate * duration).toInt()
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val chord = sin(2.0 * Math.PI * 220.0 * t) +
                            0.7 * sin(2.0 * Math.PI * 311.13 * t) +
                            0.5 * sin(2.0 * Math.PI * 55.0 * t)
                    val envelope = chord * Math.exp(-3.5 * t)
                    buffer[i] = (envelope * 24000.0 * musicVolume).toInt().toShort()
                }
                playBufferStatic(buffer, sampleRate)
            } catch (e: Throwable) {
                Log.e(TAG, "Error playing reveal sound", e)
            }
        }
    }

    private suspend fun playBufferStatic(buffer: ShortArray, sampleRate: Int) {
        val audioTrack = AudioTrack.Builder()
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
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(buffer, 0, buffer.size)
        audioTrack.play()
        delay((buffer.size * 1000L / sampleRate) + 50L)
        try { audioTrack.release() } catch (e: Throwable) {}
    }

    fun setVolume(volume: Float) {
        musicVolume = volume.coerceIn(0.0f, 1.0f)
        mediaPlayer?.setVolume(musicVolume, musicVolume)
    }

    fun enableMusic(enabled: Boolean) {
        isMusicEnabled = enabled
        if (enabled && musicVolume > 0f) startMusic() else stopMusic()
    }

    fun startMusic() {
        val ctx = context ?: return
        if (!isMusicEnabled || musicVolume <= 0f) return
        if (mediaPlayer == null) {
            try {
                val resId = ctx.resources.getIdentifier("background_music", "raw", ctx.packageName)
                if (resId == 0) {
                    Log.w(TAG, "Background music not found in res/raw/. Place background_music.mp3 there.")
                    return
                }
                mediaPlayer = MediaPlayer.create(ctx, resId)?.apply {
                    isLooping = true
                    setVolume(musicVolume, musicVolume)
                    start()
                    Log.d(TAG, "Background music started")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start music", e)
            }
        } else {
            mediaPlayer?.start()
        }
    }

    fun stopMusic() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}