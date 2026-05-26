package com.example.game.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.*
import kotlin.math.sin

object MysteryAudioPlayer {
    private const val TAG = "MysteryAudioPlayer"
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var musicVolume = 0.5f
    
    private var backgroundMusicJob: Job? = null
    private var backgroundTrack: AudioTrack? = null

    // Missing function required by ViewModel / GameNavigation
    fun playButtonClick() {
        playSelection()
    }

    // Missing function required by ViewModel
    fun playVote() {
        playSelection()
    }

    // Missing function required by ViewModel
    fun playTransition() {
        playReveal()
    }

    fun playClick() {
        playSelection()
    }

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
                    val wave = if (isSilenced) {
                        0.0
                    } else {
                        (sin(2.0 * Math.PI * 120.0 * t) + 0.5 * sin(2.0 * Math.PI * 240.0 * t))
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
                val duration = 0.35
                val numSamples = (sampleRate * duration).toInt()
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val freq = if (t < 0.15) 880.0 else 987.77
                    val envelope = sin(2.0 * Math.PI * freq * t) * (1.0 - t / duration)
                    buffer[i] = (envelope * 18000.0 * musicVolume).toInt().toShort()
                }
                playBufferStatic(buffer, sampleRate)
            } catch (e: Throwable) {
                Log.e(TAG, "Error playing warning sound", e)
            }
        }
    }

    fun playTimerTicking() {
        scope.launch {
            try {
                val sampleRate = 11025
                val numSamples = (sampleRate * 0.02).toInt()
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val envelope = sin(2.0 * Math.PI * 150.0 * t) * (1.0 - t / 0.02)
                    buffer[i] = (envelope * 15000.0 * musicVolume).toInt().toShort()
                }
                playBufferStatic(buffer, sampleRate)
            } catch (e: Throwable) {
                Log.e(TAG, "Error playing tick sound", e)
            }
        }
    }

    fun playElimination() {
        scope.launch {
            try {
                val sampleRate = 22050
                val duration = 0.7
                val numSamples = (sampleRate * duration).toInt()
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val currentFreq = 400.0 * (1.0 - (t / duration) * 0.75)
                    val wave = sin(2.0 * Math.PI * currentFreq * t)
                    val envelope = wave * (1.0 - t / duration)
                    buffer[i] = (envelope * 25000.0 * musicVolume).toInt().toShort()
                }
                playBufferStatic(buffer, sampleRate)
            } catch (e: Throwable) {
                Log.e(TAG, "Error playing elimination sound", e)
            }
        }
    }

    fun playReveal() {
        scope.launch {
            try {
                val sampleRate = 22050
                val duration = 1.2
                val numSamples = (sampleRate * duration).toInt()
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val f1 = 220.0 + (t * 40.0)
                    val f2 = 277.18
                    val f3 = 329.63
                    val combinedWave = sin(2.0 * Math.PI * f1 * t) + sin(2.0 * Math.PI * f2 * t) + sin(2.0 * Math.PI * f3 * t)
                    val envelope = (combinedWave / 3.0) * (1.0 - t / duration)
                    buffer[i] = (envelope * 23000.0 * musicVolume).toInt().toShort()
                }
                playBufferStatic(buffer, sampleRate)
            } catch (e: Throwable) {
                Log.e(TAG, "Error playing reveal sound", e)
            }
        }
    }

    private suspend fun playBufferStatic(buffer: ShortArray, sampleRate: Int) {
        withContext(Dispatchers.IO) {
            try {
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
                delay((buffer.size * 1000L / sampleRate) + 60L)
                audioTrack.stop()
                audioTrack.release()
            } catch (ex: Throwable) {
                Log.e(TAG, "Audio output hardware track failure", ex)
            }
        }
    }

    fun setVolume(volume: Float) {
        musicVolume = volume.coerceIn(0.0f, 1.0f)
        try {
            backgroundTrack?.setVolume(musicVolume * 0.4f)
        } catch (e: Exception) {
            Log.e(TAG, "Failed updating active music track volume", e)
        }
    }

    fun startMusic() {
        if (backgroundMusicJob?.isActive == true) return
        
        backgroundMusicJob = scope.launch(Dispatchers.IO) {
            val sampleRate = 22050
            val bufferSize = sampleRate * 4
            val buffer = ShortArray(bufferSize)
            
            for (i in 0 until bufferSize) {
                val t = i.toDouble() / sampleRate
                val darkLfo = sin(2.0 * Math.PI * 0.5 * t)
                val coreFreq = 110.0 + (5.0 * darkLfo)
                val wave = sin(2.0 * Math.PI * coreFreq * t) + 0.3 * sin(2.0 * Math.PI * (coreFreq * 1.5) * t)
                buffer[i] = ((wave / 1.3) * 12000.0 * musicVolume * 0.4f).toInt().toShort()
            }

            try {
                backgroundTrack = AudioTrack.Builder()
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
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                backgroundTrack?.let { track ->
                    track.write(buffer, 0, buffer.size)
                    track.setLoopPoints(0, buffer.size, -1)
                    track.play()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Could not initialize continuous music streamer", e)
            }
        }
    }

    fun stopMusic() {
        backgroundMusicJob?.cancel()
        backgroundMusicJob = null
        try {
            backgroundTrack?.stop()
            backgroundTrack?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping active music channel", e)
        }
        backgroundTrack = null
    }
}