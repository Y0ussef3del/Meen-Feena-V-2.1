package com.example.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.util.Log
import com.example.R
import kotlinx.coroutines.*
import kotlin.math.sin

object MysteryAudioPlayer {
    private const val TAG = "MysteryAudioPlayer"
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var mediaPlayer: MediaPlayer? = null
    private var musicVolume = 0.5f

    // 1. تشغيل موسيقى الخلفية التلقائية من مجلد res/raw
    fun startMusic(context: Context) {
        if (mediaPlayer == null) {
            try {
                mediaPlayer = MediaPlayer.create(context, R.raw.music_background).apply {
                    isLooping = true
                    setVolume(musicVolume, musicVolume)
                    start()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting background music", e)
            }
        } else if (!mediaPlayer!!.isPlaying) {
            mediaPlayer?.start()
        }
    }

    fun stopMusic() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping background music", e)
        }
    }

    fun setVolume(volume: Float) {
        musicVolume = volume.coerceIn(0.0f, 1.0f)
        mediaPlayer?.setVolume(musicVolume, musicVolume)
    }

    // 2. المؤثرات الصوتية المصنعة (تمنع حدوث خطأ Unresolved reference)
    fun playClick() { playSelection() }

    fun playSelection() {
        generatePcmSound(frequency = 600.0, durationMs = 40)
    }

    fun playSuccess() {
        generatePcmSound(frequency = 880.0, durationMs = 150)
    }

    fun playWarning() {
        generatePcmSound(frequency = 300.0, durationMs = 250)
    }

    fun playReveal() {
        generatePcmSound(frequency = 450.0, durationMs = 200)
    }

    private fun generatePcmSound(frequency: Double, durationMs: Int) {
        scope.launch {
            try {
                val sampleRate = 22050
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val envelope = sin(2.0 * Math.PI * frequency * t) * (1.0 - t / (durationMs / 1000.0))
                    buffer[i] = (envelope * 20000.0 * musicVolume).toInt().toShort()
                }
                
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
                delay(durationMs + 50L)
                audioTrack.release()
            } catch (e: Throwable) {
                Log.e(TAG, "Error generating audio signal", e)
            }
        }
    }
}